package com.nowcoder.community.service;

import com.nowcoder.community.client.UserClient;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.ApiResponseUtils;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 关注关系服务。
 */
@Service
public class FollowService implements CommunityConstant {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserClient userClient;

    public FollowService(RedisTemplate<String, Object> redisTemplate, UserClient userClient) {
        this.redisTemplate = redisTemplate;
        this.userClient = userClient;
    }

    /**
     * 关注实体。
     *
     * @param userId     当前用户 ID
     * @param entityType 实体类型
     * @param entityId   实体 ID
     */
    public void follow(int userId, int entityType, int entityId) {
        executeFollowTransaction(userId, entityType, entityId, true);
    }

    /**
     * 取消关注实体。
     *
     * @param userId     当前用户 ID
     * @param entityType 实体类型
     * @param entityId   实体 ID
     */
    public void unfollow(int userId, int entityType, int entityId) {
        executeFollowTransaction(userId, entityType, entityId, false);
    }

    /**
     * 查询用户关注的实体数量。
     *
     * @param userId     用户 ID
     * @param entityType 实体类型
     * @return 关注数量
     */
    public long findFolloweeCount(int userId, int entityType) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        Long count = redisTemplate.opsForZSet().zCard(followeeKey);
        return count == null ? 0L : count;
    }

    /**
     * 查询实体的粉丝数量。
     *
     * @param entityType 实体类型
     * @param entityId   实体 ID
     * @return 粉丝数量
     */
    public long findFollowerCount(int entityType, int entityId) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        Long count = redisTemplate.opsForZSet().zCard(followerKey);
        return count == null ? 0L : count;
    }

    /**
     * 判断用户是否已关注目标实体。
     *
     * @param userId     当前用户 ID
     * @param entityType 实体类型
     * @param entityId   实体 ID
     * @return 是否已关注
     */
    public boolean hasFollowed(int userId, int entityType, int entityId) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null;
    }

    /**
     * 查询用户关注的人列表。
     *
     * @param userId 用户 ID
     * @param offset 起始偏移
     * @param limit  查询数量
     * @return 关注列表
     */
    public List<Map<String, Object>> findFollowees(int userId, int offset, int limit) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);
        Set<Object> targetIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, limit + offset - 1);
        if (targetIds == null || targetIds.isEmpty()) {
            return Collections.emptyList();
        }
        return buildFollowUserView(targetIds, followeeKey);
    }

    /**
     * 查询用户粉丝列表。
     *
     * @param entityId 用户 ID
     * @param offset   起始偏移
     * @param limit    查询数量
     * @return 粉丝列表
     */
    public List<Map<String, Object>> findFollowers(int entityId, int offset, int limit) {
        String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, entityId);
        Set<Object> targetIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, limit + offset - 1);
        if (targetIds == null || targetIds.isEmpty()) {
            return Collections.emptyList();
        }
        return buildFollowUserView(targetIds, followerKey);
    }

    /**
     * 执行关注关系事务。
     *
     * @param userId     当前用户 ID
     * @param entityType 实体类型
     * @param entityId   实体 ID
     * @param follow     true 表示关注，false 表示取消关注
     */
    private void executeFollowTransaction(int userId, int entityType, int entityId, boolean follow) {
        redisTemplate.execute((SessionCallback<Object>) operations -> {
            String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
            String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
            operations.multi();
            if (follow) {
                double score = System.currentTimeMillis();
                operations.opsForZSet().add(followeeKey, entityId, score);
                operations.opsForZSet().add(followerKey, userId, score);
            } else {
                operations.opsForZSet().remove(followeeKey, entityId);
                operations.opsForZSet().remove(followerKey, userId);
            }
            return operations.exec();
        });
    }

    /**
     * 组装关注 / 粉丝视图。
     *
     * @param targetIds 目标用户 ID 集合
     * @param redisKey  对应关系 Key
     * @return 组装后的列表
     */
    private List<Map<String, Object>> buildFollowUserView(Set<Object> targetIds, String redisKey) {
        List<Integer> userIds = new ArrayList<>();
        for (Object targetId : targetIds) {
            if (targetId instanceof Integer) {
                userIds.add((Integer) targetId);
            }
        }

        Map<Integer, User> userMap = fetchUsers(userIds);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Integer targetId : userIds) {
            Map<String, Object> item = new HashMap<>();
            item.put("user", userMap.get(targetId));
            Double score = redisTemplate.opsForZSet().score(redisKey, targetId);
            item.put("followedTime", score == null ? null : new Date(score.longValue()));
            result.add(item);
        }
        return result;
    }

    /**
     * 批量加载用户信息。
     *
     * @param ids 用户 ID 集合
     * @return 用户映射
     */
    private Map<Integer, User> fetchUsers(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, User> users = ApiResponseUtils.unwrap(userClient.getUsers(new ArrayList<>(ids)));
        return users == null ? Collections.emptyMap() : users;
    }
}
