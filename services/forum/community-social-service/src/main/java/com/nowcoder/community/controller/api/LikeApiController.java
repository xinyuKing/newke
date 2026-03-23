package com.nowcoder.community.controller.api;

import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.util.ApiResponse;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/likes")
public class LikeApiController implements CommunityConstant {
    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping
    public ApiResponse<Map<String, Object>> like(@RequestBody Map<String, Object> body) {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(403, "not_login");
        }
        int entityType = Integer.parseInt(String.valueOf(body.get("entityType")));
        int entityId = Integer.parseInt(String.valueOf(body.get("entityId")));
        int targetId = Integer.parseInt(String.valueOf(body.get("targetId")));
        int postId = Integer.parseInt(String.valueOf(body.get("postId")));

        likeService.like(user.getId(), entityType, entityId, targetId);
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);

        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        if (likeStatus == 1) {
            Event event = new Event();
            event.setTopic(TOPIC_LIKE);
            event.setEntityType(entityType);
            event.setEntityId(entityId);
            event.setUserId(user.getId());
            event.setEntityUserId(targetId);
            event.setData("postId", postId);
            eventProducer.fireEvent(event);
        }

        if (entityType == ENTITY_TYPE_POST) {
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, postId);
        }

        return ApiResponse.success(map);
    }

    @GetMapping("/count")
    public ApiResponse<Long> count(@RequestParam("entityType") int entityType, @RequestParam("entityId") int entityId) {
        return ApiResponse.success(likeService.findEntityLikeCount(entityType, entityId));
    }

    @PostMapping("/counts")
    public ApiResponse<Map<Integer, Long>> counts(@RequestBody Map<String, Object> body) {
        int entityType = Integer.parseInt(String.valueOf(body.get("entityType")));
        List<Integer> ids = parseIds(body.get("entityIds"));
        return ApiResponse.success(likeService.findEntityLikeCounts(entityType, ids));
    }

    @GetMapping("/status")
    public ApiResponse<Integer> status(
            @RequestParam("userId") int userId,
            @RequestParam("entityType") int entityType,
            @RequestParam("entityId") int entityId) {
        return ApiResponse.success(likeService.findEntityLikeStatus(userId, entityType, entityId));
    }

    @PostMapping("/statuses")
    public ApiResponse<Map<Integer, Integer>> statuses(@RequestBody Map<String, Object> body) {
        int userId = Integer.parseInt(String.valueOf(body.get("userId")));
        int entityType = Integer.parseInt(String.valueOf(body.get("entityType")));
        List<Integer> ids = parseIds(body.get("entityIds"));
        return ApiResponse.success(likeService.findEntityLikeStatuses(userId, entityType, ids));
    }

    @GetMapping("/user-count")
    public ApiResponse<Integer> userCount(@RequestParam("userId") int userId) {
        return ApiResponse.success(likeService.findUserLikeCount(userId));
    }

    private List<Integer> parseIds(Object value) {
        List<Integer> result = new ArrayList<>();
        if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                if (item != null) {
                    result.add(Integer.parseInt(String.valueOf(item)));
                }
            }
        }
        return result;
    }
}
