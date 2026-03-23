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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户中心 JSON 接口。
 */
@RestController
@RequestMapping("/api/users")
public class UserApiController implements CommunityConstant {

    private final UserService userService;
    private final HostHolder hostHolder;
    private final LikeClient likeClient;
    private final FollowClient followClient;
    private final PostClient postClient;

    public UserApiController(
            UserService userService,
            HostHolder hostHolder,
            LikeClient likeClient,
            FollowClient followClient,
            PostClient postClient) {
        this.userService = userService;
        this.hostHolder = hostHolder;
        this.likeClient = likeClient;
        this.followClient = followClient;
        this.postClient = postClient;
    }

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
        User currentUser = hostHolder.getUser();
        if (currentUser != null) {
            Boolean followed =
                    ApiResponseUtils.unwrap(followClient.hasFollowed(currentUser.getId(), ENTITY_TYPE_USER, userId));
            hasFollowed = Boolean.TRUE.equals(followed);
        }
        data.put("hasFollowed", hasFollowed);
        return ApiResponse.success(data);
    }

    @GetMapping("/{userId}/posts")
    public ApiResponse<Map<String, Object>> myPosts(
            @PathVariable int userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        int safePage = normalizePage(page);
        int safeLimit = normalizeLimit(limit);
        int offset = (safePage - 1) * safeLimit;

        Integer rows = ApiResponseUtils.unwrap(postClient.countPostsByUser(userId));
        List<DiscussPost> posts = ApiResponseUtils.unwrap(postClient.listPostsByUser(userId, offset, safeLimit));
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
        data.put("page", safePage);
        data.put("limit", safeLimit);
        return ApiResponse.success(data);
    }

    @GetMapping("/{userId}/replies")
    public ApiResponse<Map<String, Object>> myReplies(
            @PathVariable int userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        int safePage = normalizePage(page);
        int safeLimit = normalizeLimit(limit);
        int offset = (safePage - 1) * safeLimit;

        Integer rows = ApiResponseUtils.unwrap(postClient.countCommentsByUser(userId));
        List<Comment> commentsList = ApiResponseUtils.unwrap(postClient.listCommentsByUser(userId, offset, safeLimit));
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
            if (cursor.getEntityType() != ENTITY_TYPE_COMMENT) {
                post = ApiResponseUtils.unwrap(postClient.getPost(cursor.getEntityId()));
            }
            commentVo.put("post", post);
            items.add(commentVo);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", items);
        data.put("rows", rows);
        data.put("page", safePage);
        data.put("limit", safeLimit);
        return ApiResponse.success(data);
    }

    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(@RequestBody Map<String, Object> body) {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(403, "not_login");
        }
        if (body == null) {
            return ApiResponse.error(400, "invalid_request");
        }

        String oldPassword =
                body.get("oldPassword") == null ? null : body.get("oldPassword").toString();
        String newPassword =
                body.get("newPassword") == null ? null : body.get("newPassword").toString();
        if (StringUtils.isBlank(newPassword)) {
            return ApiResponse.error(1, "new_password_empty");
        }
        if (!userService.matchesPassword(user, oldPassword)) {
            return ApiResponse.error(1, "wrong_password");
        }

        userService.updatePassword(user.getId(), userService.encodePassword(newPassword));
        return ApiResponse.success(null);
    }

    @PutMapping("/header")
    public ApiResponse<Void> updateHeader(@RequestBody Map<String, Object> body) {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(403, "not_login");
        }
        if (body == null) {
            return ApiResponse.error(400, "invalid_request");
        }

        String headerUrl =
                body.get("headerUrl") == null ? null : body.get("headerUrl").toString();
        if (StringUtils.isBlank(headerUrl)) {
            return ApiResponse.error(1, "header_empty");
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

    /**
     * 兜底修正页码参数。
     *
     * @param page 页码
     * @return 合法页码
     */
    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    /**
     * 兜底修正分页大小。
     *
     * @param limit 分页大小
     * @return 合法分页大小
     */
    private int normalizeLimit(int limit) {
        return limit > 0 ? limit : 10;
    }
}
