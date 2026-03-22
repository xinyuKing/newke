package com.nowcoder.community.service;

import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LikeService implements CommunityConstant {
    @Autowired
    private RedisTemplate redisTemplate;

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

    //点赞取消点赞功能
    public void like(int userId, int entityType, int id, int targetUserId) {
//        String entityLikeKey= RedisKeyUtil.getEntityLikeKey(entityType,id);
//        //判断用户是否点赞
//        Boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
//        if(isMember){//已有点赞，就取消点赞
//            redisTemplate.opsForSet().remove(entityLikeKey,userId);
//        }else {//没有点赞，点赞成功
//            redisTemplate.opsForSet().add(entityLikeKey,userId);
//        }
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, id);
        String userLikeKey = RedisKeyUtil.getUserLikeKey(targetUserId);
        redisTemplate.execute(
                LIKE_TOGGLE_SCRIPT,
                List.of(entityLikeKey, userLikeKey),
                userId
        );
    }

    //查询实体点赞的数量
    public long findEntityLikeCount(int entityType, int id) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, id);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    //查询某人对某实体的点赞状态
    public int findEntityLikeStatus(int userId, int entityType, int id) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, id);
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
    }

    //查询每个用户获得的数量
    public int findUserLikeCount(int userId) {
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);

        return count == null ? 0 : count.intValue();
    }

    public Map<Integer, Long> findEntityLikeCounts(int entityType, Collection<Integer> entityIds){
        List<Integer> idList = normalizeIds(entityIds);
        if(idList.isEmpty()){
            return Collections.emptyMap();
        }
        List<Object> results = redisTemplate.executePipelined(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                for(Integer id:idList){
                    String key = RedisKeyUtil.getEntityLikeKey(entityType, id);
                    operations.opsForSet().size(key);
                }
                return null;
            }
        });
        Map<Integer, Long> map=new HashMap<>();
        for(int i=0;i<idList.size();i++){
            Object value = results.get(i);
            long count = value==null?0L:((Number)value).longValue();
            map.put(idList.get(i), count);
        }
        return map;
    }

    public Map<Integer, Integer> findEntityLikeStatuses(int userId,int entityType,Collection<Integer> entityIds){
        List<Integer> idList = normalizeIds(entityIds);
        if(idList.isEmpty()){
            return Collections.emptyMap();
        }
        List<Object> results = redisTemplate.executePipelined(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                for(Integer id:idList){
                    String key = RedisKeyUtil.getEntityLikeKey(entityType, id);
                    operations.opsForSet().isMember(key, userId);
                }
                return null;
            }
        });
        Map<Integer, Integer> map=new HashMap<>();
        for(int i=0;i<idList.size();i++){
            Object value = results.get(i);
            boolean isMember = value!=null && (Boolean) value;
            map.put(idList.get(i), isMember?1:0);
        }
        return map;
    }

    private List<Integer> normalizeIds(Collection<Integer> entityIds){
        if(entityIds==null||entityIds.isEmpty()){
            return Collections.emptyList();
        }
        List<Integer> list=new ArrayList<>();
        for(Integer id:entityIds){
            if(id!=null && !list.contains(id)){
                list.add(id);
            }
        }
        return list;
    }
}
