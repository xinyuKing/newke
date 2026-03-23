package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 站点统计服务。
 *
 * <p>负责基于 Redis 记录和统计站点 UV、DAU 数据。</p>
 */
@Service
public class DataService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();

    private final RedisTemplate<String, Object> redisTemplate;

    public DataService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 记录单日 UV。
     *
     * @param ip 用户 IP
     */
    public void recordUV(String ip) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        String redisKey = RedisKeyUtil.getUVKey(formatDate(new Date()));
        redisTemplate.opsForHyperLogLog().add(redisKey, ip);
    }

    /**
     * 统计指定日期范围内的 UV。
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return UV 统计结果
     */
    public long calculateUV(Date start, Date end) {
        validateDateRange(start, end);

        List<String> redisKeys = new ArrayList<>();
        LocalDate currentDate = toLocalDate(start);
        LocalDate endDate = toLocalDate(end);
        while (!currentDate.isAfter(endDate)) {
            redisKeys.add(RedisKeyUtil.getUVKey(currentDate.format(DATE_FORMATTER)));
            currentDate = currentDate.plusDays(1);
        }

        String unionKey = RedisKeyUtil.getUVKey(formatDate(start), formatDate(end));
        redisTemplate.opsForHyperLogLog().union(unionKey, redisKeys.toArray(new String[0]));
        Long size = redisTemplate.opsForHyperLogLog().size(unionKey);
        return size == null ? 0L : size;
    }

    /**
     * 记录单日 DAU。
     *
     * @param userId 用户 ID
     */
    public void recordDAU(int userId) {
        String redisKey = RedisKeyUtil.getDAUKey(formatDate(new Date()));
        redisTemplate.opsForValue().setBit(redisKey, userId, true);
    }

    /**
     * 统计指定日期范围内的 DAU。
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return DAU 统计结果
     */
    public long calculateDAU(Date start, Date end) {
        validateDateRange(start, end);

        List<byte[]> redisKeys = new ArrayList<>();
        LocalDate currentDate = toLocalDate(start);
        LocalDate endDate = toLocalDate(end);
        while (!currentDate.isAfter(endDate)) {
            redisKeys.add(
                    RedisKeyUtil.getDAUKey(currentDate.format(DATE_FORMATTER)).getBytes(StandardCharsets.UTF_8));
            currentDate = currentDate.plusDays(1);
        }

        Long count = redisTemplate.execute(
                (RedisCallback<Long>) connection -> bitCountByRange(connection, start, end, redisKeys));
        return count == null ? 0L : count;
    }

    /**
     * 执行位图 OR 统计。
     *
     * @param connection Redis 连接
     * @param start      开始日期
     * @param end        结束日期
     * @param redisKeys  位图 Key 列表
     * @return DAU 统计结果
     * @throws DataAccessException Redis 访问异常
     */
    private Long bitCountByRange(RedisConnection connection, Date start, Date end, List<byte[]> redisKeys)
            throws DataAccessException {
        String redisKey = RedisKeyUtil.getDAUKey(formatDate(start), formatDate(end));
        byte[] unionKey = redisKey.getBytes(StandardCharsets.UTF_8);
        connection.bitOp(RedisStringCommands.BitOperation.OR, unionKey, redisKeys.toArray(new byte[0][]));
        return connection.bitCount(unionKey);
    }

    /**
     * 校验日期区间是否合法。
     *
     * @param start 开始日期
     * @param end   结束日期
     */
    private void validateDateRange(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("统计日期不能为空");
        }
        if (start.after(end)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
    }

    /**
     * 将日期格式化为 Redis Key 所需字符串。
     *
     * @param date 日期
     * @return 格式化结果
     */
    private String formatDate(Date date) {
        return toLocalDate(date).format(DATE_FORMATTER);
    }

    /**
     * 将 {@link Date} 转换为 {@link LocalDate}。
     *
     * @param date 日期
     * @return 本地日期
     */
    private LocalDate toLocalDate(Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(DEFAULT_ZONE_ID).toLocalDate();
    }
}
