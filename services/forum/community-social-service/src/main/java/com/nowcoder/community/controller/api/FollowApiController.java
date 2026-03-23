package com.nowcoder.community.controller.api;

import com.nowcoder.community.client.UserClient;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.util.ApiResponse;
import com.nowcoder.community.util.ApiResponseUtils;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FollowApiController implements CommunityConstant {
    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserClient userClient;

    @Autowired
    private EventProducer eventProducer;

    @PostMapping("/follow")
    public ApiResponse<Void> follow(@RequestBody Map<String, Object> body) {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(403, "not_login");
        }
        int entityType = Integer.parseInt(String.valueOf(body.get("entityType")));
        int entityId = Integer.parseInt(String.valueOf(body.get("entityId")));
        followService.follow(user.getId(), entityType, entityId);

        Event event = new Event();
        event.setTopic(TOPIC_FOLLOW);
        event.setEntityId(entityId);
        event.setEntityType(entityType);
        event.setUserId(user.getId());
        event.setEntityUserId(entityId);
        eventProducer.fireEvent(event);

        return ApiResponse.success(null);
    }

    @PostMapping("/unfollow")
    public ApiResponse<Void> unfollow(@RequestBody Map<String, Object> body) {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(403, "not_login");
        }
        int entityType = Integer.parseInt(String.valueOf(body.get("entityType")));
        int entityId = Integer.parseInt(String.valueOf(body.get("entityId")));
        followService.unfollow(user.getId(), entityType, entityId);
        return ApiResponse.success(null);
    }

    @GetMapping("/users/{userId}/followees")
    public ApiResponse<Map<String, Object>> followees(
            @PathVariable int userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        User user = ApiResponseUtils.unwrap(userClient.getUser(userId));
        if (user == null) {
            return ApiResponse.error(404, "user_not_found");
        }
        int offset = (page - 1) * limit;
        long rows = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        List<Map<String, Object>> userList = followService.findFollowees(userId, offset, limit);

        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("list", userList);
        data.put("rows", rows);
        data.put("page", page);
        data.put("limit", limit);
        return ApiResponse.success(data);
    }

    @GetMapping("/users/{userId}/followers")
    public ApiResponse<Map<String, Object>> followers(
            @PathVariable int userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        User user = ApiResponseUtils.unwrap(userClient.getUser(userId));
        if (user == null) {
            return ApiResponse.error(404, "user_not_found");
        }
        int offset = (page - 1) * limit;
        long rows = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        List<Map<String, Object>> userList = followService.findFollowers(userId, offset, limit);

        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("list", userList);
        data.put("rows", rows);
        data.put("page", page);
        data.put("limit", limit);
        return ApiResponse.success(data);
    }

    @GetMapping("/followees/count")
    public ApiResponse<Long> followeeCount(
            @RequestParam("userId") int userId, @RequestParam("entityType") int entityType) {
        return ApiResponse.success(followService.findFolloweeCount(userId, entityType));
    }

    @GetMapping("/followers/count")
    public ApiResponse<Long> followerCount(
            @RequestParam("entityType") int entityType, @RequestParam("entityId") int entityId) {
        return ApiResponse.success(followService.findFollowerCount(entityType, entityId));
    }

    @GetMapping("/has-followed")
    public ApiResponse<Boolean> hasFollowed(
            @RequestParam("userId") int userId,
            @RequestParam("entityType") int entityType,
            @RequestParam("entityId") int entityId) {
        return ApiResponse.success(followService.hasFollowed(userId, entityType, entityId));
    }

    private boolean hasFollowed(int userId) {
        if (hostHolder.getUser() == null) {
            return false;
        }
        return followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
    }
}
