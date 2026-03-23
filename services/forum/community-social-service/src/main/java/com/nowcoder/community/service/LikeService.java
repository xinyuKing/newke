package com.nowcoder.community.service;

import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 点赞服务。
 */
@Service
public class LikeService implements CommunityConstant {

    private static final int LIKE_STATUS_LIKED = 1;
    private static final int LIKE_STATUS_UNLIKED = 0;
    private static final DefaultRedisScript<Long> LIKE_TOGGLE_SCRIPT = new DefaultRedisScript<>();

    static {
        LIKE_TOGGLE_SCRIPT.setResultType(Long.class);
        LIKE_TOGGLE_SCRIPT.setScriptText(
                "local entityLikeKey = KEYS[1]\n" +
                "local userLikeKey = KEYS[2]\n" +
                "local userId = ARGV[1]\n" +
                "if redis.call('SISMEMBER', entityLikeKey, userId) == 1 then\n" +
                "  redis.call('SREM', entityLikeKey, userId)\n" +
                "  redis.call('DECR', userLikeKey)\n" +
                "  return 0\n" +
                "else\n" +
                "  redis.call('SADD', entityLikeKey, userId)\n" +
                "  redis.call('INCR', userLikeKey)\n" +
                "  return 1\n" +
                "end"
        );
    }

    private final RedisTemplate<String, Object> redisTemplate;

    public LikeService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 点赞或取消点赞。
     *
     * @param userId       当前用户 ID
     * @param entityType   实体类型
     * @param entityId     实体 ID
     * @param targetUserId 被点赞用户 ID
     */
    public void like(int userId, int entityType, int entityId, int targetUserId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        String userLikeKey = RedisKeyUtil.getUserLikeKey(targetUserId);
        redisTemplate.execute(LIKE_TOGGLE_SCRIPT, List.of(entityLikeKey, userLikeKey), userId);
    }

    /**
     * 查询实体点赞数。
     *
     * @param entityType 实体类型
     * @param entityId   实体 ID
     * @return 点赞数
     */
    public long findEntityLikeCount(int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        Long count = redisTemplate.opsForSet().size(entityLikeKey);
        return count == null ? 0L : count;
    }

    /**
     * 查询当前用户对实体的点赞状态。
     *
     * @param userId     当前用户 ID
     * @param entityType 实体类型
     * @param entityId   实体 ID
     * @return 1 表示已点赞，0 表示未点赞
     */
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        Boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
        return Boolean.TRUE.equals(isMember) ? LIKE_STATUS_LIKED : LIKE_STATUS_UNLIKED;
    }

    /**
     * 查询用户累计获得的点赞数。
     *
     * @param userId 用户 ID
     * @return 点赞数
     */
    public int findUserLikeCount(int userId) {
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Object count = redisTemplate.opsForValue().get(userLikeKey);
        return count instanceof Number ? ((Number) count).intValue() : 0;
    }

    /**
     * 批量查询实体点赞数。
     *
     * @param entityType 实体类型
     * @param entityIds  实体 ID 集合
     * @return 点赞数映射
     */
    public Map<Integer, Long> findEntityLikeCounts(int entityType, Collection<Integer> entityIds) {
        List<Integer> idList = normalizeIds(entityIds);
        if (idList.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object> results = redisTemplate.executePipelined((SessionCallback<Object>) operations -> {
            for (Integer entityId : idList) {
                String key = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                operations.opsForSet().size(key);
            }
            return null;
        });

        Map<Integer, Long> likeCountMap = new LinkedHashMap<>();
        for (int index = 0; index < idList.size(); index++) {
            Object value = results.get(index);
            likeCountMap.put(idList.get(index), value == null ? 0L : ((Number) value).longValue());
        }
        return likeCountMap;
    }

    /**
     * 批量查询实体点赞状态。
     *
     * @param userId     当前用户 ID
     * @param entityType 实体类型
     * @param entityIds  实体 ID 集合
     * @return 点赞状态映射
     */
    public Map<Integer, Integer> findEntityLikeStatuses(int userId, int entityType, Collection<Integer> entityIds) {
        List<Integer> idList = normalizeIds(entityIds);
        if (idList.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object> results = redisTemplate.executePipelined((SessionCallback<Object>) operations -> {
            for (Integer entityId : idList) {
                String key = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                operations.opsForSet().isMember(key, userId);
            }
            return null;
        });

        Map<Integer, Integer> likeStatusMap = new LinkedHashMap<>();
        for (int index = 0; index < idList.size(); index++) {
            Object value = results.get(index);
            boolean isMember = value instanceof Boolean && (Boolean) value;
            likeStatusMap.put(idList.get(index), isMember ? LIKE_STATUS_LIKED : LIKE_STATUS_UNLIKED);
        }
        return likeStatusMap;
    }

    /**
     * 标准化实体 ID 列表，去空并去重。
     *
     * @param entityIds 实体 ID 集合
     * @return 处理后的列表
     */
    private List<Integer> normalizeIds(Collection<Integer> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Integer> uniqueIds = new LinkedHashSet<>();
        for (Integer entityId : entityIds) {
            if (entityId != null) {
                uniqueIds.add(entityId);
            }
        }
        return new ArrayList<>(uniqueIds);
    }
}
