package com.nowcoder.community.controller.api;

import com.nowcoder.community.client.FollowClient;
import com.nowcoder.community.client.LikeClient;
import com.nowcoder.community.client.PostClient;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.ApiResponse;
import com.nowcoder.community.util.ApiResponseUtils;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UserApiController implements CommunityConstant {
    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeClient likeClient;

    @Autowired
    private FollowClient followClient;

    @Autowired
    private PostClient postClient;

    @GetMapping("/{userId}")
    public ApiResponse<User> getUser(@PathVariable int userId) {
        return ApiResponse.success(userService.findUserById(userId));
    }

    @GetMapping("/{userId}/profile")
    public ApiResponse<Map<String, Object>> profile(@PathVariable int userId) {
        User user = userService.findUserById(userId);
        if (user == null) {
            return ApiResponse.error(404, "user_not_found");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        Integer userLikeCount = ApiResponseUtils.unwrap(likeClient.getUserLikeCount(userId));
        Long followeeCount = ApiResponseUtils.unwrap(followClient.getFolloweeCount(userId, ENTITY_TYPE_USER));
        Long followerCount = ApiResponseUtils.unwrap(followClient.getFollowerCount(ENTITY_TYPE_USER, userId));
        data.put("userLikeCount", userLikeCount == null ? 0 : userLikeCount);
        data.put("followeeCount", followeeCount == null ? 0L : followeeCount);
        data.put("followerCount", followerCount == null ? 0L : followerCount);
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            Boolean followed = ApiResponseUtils.unwrap(
                    followClient.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId));
            hasFollowed = Boolean.TRUE.equals(followed);
        }
        data.put("hasFollowed", hasFollowed);
        return ApiResponse.success(data);
    }

    @GetMapping("/{userId}/posts")
    public ApiResponse<Map<String, Object>> myPosts(@PathVariable int userId,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int limit) {
        int offset = (page - 1) * limit;
        Integer rows = ApiResponseUtils.unwrap(postClient.countPostsByUser(userId));
        List<DiscussPost> posts = ApiResponseUtils.unwrap(postClient.listPostsByUser(userId, offset, limit));
        if (rows == null) {
            rows = 0;
        }
        if (posts == null) {
            posts = Collections.emptyList();
        }
        List<Integer> postIds = new ArrayList<>();
        for (DiscussPost post : posts) {
            postIds.add(post.getId());
        }
        Map<String, Object> likeBody = new HashMap<>();
        likeBody.put("entityType", LIKE_TYPE_POST);
        likeBody.put("entityIds", postIds);
        Map<Integer, Long> likeCountMap = ApiResponseUtils.unwrap(likeClient.getEntityLikeCounts(likeBody));
        if (likeCountMap == null) {
            likeCountMap = Collections.emptyMap();
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (DiscussPost post : posts) {
            Map<String, Object> item = new HashMap<>();
            item.put("post", post);
            item.put("likeCount", likeCountMap.getOrDefault(post.getId(), 0L));
            items.add(item);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("list", items);
        data.put("rows", rows);
        data.put("page", page);
        data.put("limit", limit);
        return ApiResponse.success(data);
    }

    @GetMapping("/{userId}/replies")
    public ApiResponse<Map<String, Object>> myReplies(@PathVariable int userId,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int limit) {
        int offset = (page - 1) * limit;
        Integer rows = ApiResponseUtils.unwrap(postClient.countCommentsByUser(userId));
        List<Comment> commentsList = ApiResponseUtils.unwrap(postClient.listCommentsByUser(userId, offset, limit));
        if (rows == null) {
            rows = 0;
        }
        if (commentsList == null) {
            commentsList = Collections.emptyList();
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (Comment comment : commentsList) {
            Map<String, Object> commentVo = new HashMap<>();
            commentVo.put("comment", comment);
            Comment cursor = comment;
            while (cursor.getEntityType() == ENTITY_TYPE_COMMENT) {
                Comment next = ApiResponseUtils.unwrap(postClient.getComment(cursor.getEntityId()));
                if (next == null) {
                    break;
                }
                cursor = next;
            }
            DiscussPost post = null;
            if (cursor != null && cursor.getEntityType() != ENTITY_TYPE_COMMENT) {
                post = ApiResponseUtils.unwrap(postClient.getPost(cursor.getEntityId()));
            }
            commentVo.put("post", post);
            items.add(commentVo);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("list", items);
        data.put("rows", rows);
        data.put("page", page);
        data.put("limit", limit);
        return ApiResponse.success(data);
    }

    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(@RequestBody Map<String, Object> body) {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(403, "not_login");
        }
        String oldPassword = body.get("oldPassword") == null ? null : body.get("oldPassword").toString();
        String newPassword = body.get("newPassword") == null ? null : body.get("newPassword").toString();
        if (!userService.matchesPassword(user, oldPassword)) {
            return ApiResponse.error(1, "wrong_password");
        }
        userService.updatePassword(user.getId(), userService.encodePassword(newPassword));
        return ApiResponse.success(null);
    }

    @PutMapping("/header")
    public ApiResponse<Void> updateHeader(@RequestBody Map<String, Object> body) {
        String headerUrl = body.get("headerUrl") == null ? null : body.get("headerUrl").toString();
        if (StringUtils.isBlank(headerUrl)) {
            return ApiResponse.error(1, "header_empty");
        }
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(403, "not_login");
        }
        userService.updateHeader(user.getId(), headerUrl);
        return ApiResponse.success(null);
    }

    @PostMapping("/batch")
    public ApiResponse<Map<Integer, User>> batch(@RequestBody List<Integer> ids) {
        return ApiResponse.success(userService.findUsersByIds(ids));
    }

    @GetMapping("/by-name")
    public ApiResponse<User> byName(@RequestParam("username") String username) {
        if (StringUtils.isBlank(username)) {
            return ApiResponse.error(400, "username_empty");
        }
        User user = userService.findUserByName(username);
        if (user == null) {
            return ApiResponse.error(404, "user_not_found");
        }
        return ApiResponse.success(user);
    }
}
