package com.nowcoder.community.controller.api;

import com.alibaba.fastjson2.JSON;
import com.nowcoder.community.client.LikeClient;
import com.nowcoder.community.client.UserClient;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.moderation.ModerationResult;
import com.nowcoder.community.service.moderation.ModerationService;
import com.nowcoder.community.util.ApiResponse;
import com.nowcoder.community.util.ApiResponseUtils;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 帖子 REST 接口。
 */
@RestController
@RequestMapping("/api/posts")
public class PostApiController implements CommunityConstant {

    private final DiscussPostService discussPostService;
    private final UserClient userClient;
    private final LikeClient likeClient;
    private final CommentService commentService;
    private final HostHolder hostHolder;
    private final EventProducer eventProducer;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ModerationService moderationService;

    public PostApiController(
            DiscussPostService discussPostService,
            UserClient userClient,
            LikeClient likeClient,
            CommentService commentService,
            HostHolder hostHolder,
            EventProducer eventProducer,
            RedisTemplate<String, Object> redisTemplate,
            ModerationService moderationService) {
        this.discussPostService = discussPostService;
        this.userClient = userClient;
        this.likeClient = likeClient;
        this.commentService = commentService;
        this.hostHolder = hostHolder;
        this.eventProducer = eventProducer;
        this.redisTemplate = redisTemplate;
        this.moderationService = moderationService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int orderMode) {
        int safePage = normalizePage(page);
        int safeLimit = normalizeLimit(limit);
        int safeOrderMode = orderMode == 1 ? 1 : 0;
        int rows = discussPostService.selectDiscussPostRows(0);
        int offset = (safePage - 1) * safeLimit;
        List<DiscussPost> posts = discussPostService.findDiscussPosts(0, offset, safeLimit, safeOrderMode);

        Set<Integer> userIds = new LinkedHashSet<>();
        List<Integer> postIds = new ArrayList<>();
        for (DiscussPost post : posts) {
            userIds.add(post.getUserId());
            postIds.add(post.getId());
        }

        Map<Integer, User> userMap = loadUserMap(userIds);
        Map<Integer, Long> likeCountMap = loadLikeCountMap(LIKE_TYPE_POST, postIds);

        List<Map<String, Object>> items = new ArrayList<>();
        for (DiscussPost post : posts) {
            Map<String, Object> item = new HashMap<>();
            item.put("post", post);
            item.put("user", userMap.get(post.getUserId()));
            item.put("likeCount", likeCountMap.getOrDefault(post.getId(), 0L));
            items.add(item);
        }

        Map<String, Object> pageInfo = new HashMap<>();
        pageInfo.put("current", safePage);
        pageInfo.put("limit", safeLimit);
        pageInfo.put("rows", rows);
        pageInfo.put("total", rows == 0 ? 0 : (rows + safeLimit - 1) / safeLimit);

        Map<String, Object> data = new HashMap<>();
        data.put("list", items);
        data.put("page", pageInfo);
        data.put("orderMode", safeOrderMode);
        return ApiResponse.success(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(
            @PathVariable("id") int postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int limit) {
        DiscussPost post = discussPostService.findDiscussPostById(postId);
        if (post == null) {
            return ApiResponse.error(404, "post_not_found");
        }

        int safePage = normalizePage(page);
        int safeLimit = normalizeLimit(limit);
        int offset = (safePage - 1) * safeLimit;
        User currentUser = hostHolder.getUser();

        List<Comment> comments = commentService.findCommentsByEntity(ENTITY_TYPE_POST, post.getId(), offset, safeLimit);
        List<Integer> commentIds = new ArrayList<>();
        for (Comment comment : comments) {
            commentIds.add(comment.getId());
        }

        List<Comment> replyList = commentService.findCommentsByEntityIds(ENTITY_TYPE_COMMENT, commentIds);
        Map<Integer, List<Comment>> replyMap = new HashMap<>();
        for (Comment reply : replyList) {
            replyMap.computeIfAbsent(reply.getEntityId(), key -> new ArrayList<>())
                    .add(reply);
        }
        Map<Integer, Integer> replyCountMap = commentService.findCountByEntityIds(ENTITY_TYPE_COMMENT, commentIds);

        Set<Integer> userIds = new LinkedHashSet<>();
        userIds.add(post.getUserId());
        for (Comment comment : comments) {
            userIds.add(comment.getUserId());
        }
        for (Comment reply : replyList) {
            userIds.add(reply.getUserId());
            if (reply.getTargetId() != 0) {
                userIds.add(reply.getTargetId());
            }
        }
        Map<Integer, User> userMap = loadUserMap(userIds);

        List<Integer> likeEntityIds = new ArrayList<>(commentIds);
        for (Comment reply : replyList) {
            likeEntityIds.add(reply.getId());
        }
        Map<Integer, Long> commentLikeCountMap = loadLikeCountMap(LIKE_TYPE_COMMENT, likeEntityIds);
        Map<Integer, Integer> commentLikeStatusMap = currentUser == null
                ? Collections.emptyMap()
                : loadLikeStatusMap(currentUser.getId(), LIKE_TYPE_COMMENT, likeEntityIds);

        List<Map<String, Object>> commentVoList = new ArrayList<>();
        for (Comment comment : comments) {
            Map<String, Object> commentVo = new HashMap<>();
            commentVo.put("comment", comment);
            commentVo.put("user", userMap.get(comment.getUserId()));
            commentVo.put("likeCount", commentLikeCountMap.getOrDefault(comment.getId(), 0L));
            commentVo.put("likeStatus", commentLikeStatusMap.getOrDefault(comment.getId(), 0));

            List<Map<String, Object>> replyVoList = new ArrayList<>();
            for (Comment reply : replyMap.getOrDefault(comment.getId(), Collections.emptyList())) {
                Map<String, Object> replyVo = new HashMap<>();
                replyVo.put("reply", reply);
                replyVo.put("user", userMap.get(reply.getUserId()));
                replyVo.put("target", reply.getTargetId() == 0 ? null : userMap.get(reply.getTargetId()));
                replyVo.put("likeCount", commentLikeCountMap.getOrDefault(reply.getId(), 0L));
                replyVo.put("likeStatus", commentLikeStatusMap.getOrDefault(reply.getId(), 0));
                replyVoList.add(replyVo);
            }
            commentVo.put("replys", replyVoList);
            commentVo.put("replyCount", replyCountMap.getOrDefault(comment.getId(), 0));
            commentVoList.add(commentVo);
        }

        Long postLikeCount = ApiResponseUtils.unwrap(likeClient.getEntityLikeCount(LIKE_TYPE_POST, postId));
        int postLikeStatus = 0;
        if (currentUser != null) {
            Integer status = ApiResponseUtils.unwrap(
                    likeClient.getEntityLikeStatus(currentUser.getId(), LIKE_TYPE_POST, postId));
            postLikeStatus = status == null ? 0 : status;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("post", post);
        data.put("user", userMap.get(post.getUserId()));
        data.put("likeCount", postLikeCount == null ? 0L : postLikeCount);
        data.put("likeStatus", postLikeStatus);
        data.put("comments", commentVoList);
        data.put("rows", post.getCommentCount());
        data.put("page", safePage);
        data.put("limit", safeLimit);
        return ApiResponse.success(data);
    }

    @PostMapping
    public ApiResponse<Object> add(@RequestBody Map<String, Object> body) {
        User currentUser = hostHolder.getUser();
        if (currentUser == null) {
            return ApiResponse.error(403, "not_login");
        }
        if (body == null) {
            return ApiResponse.error(400, "invalid_request");
        }

        String title = body.get("title") == null ? null : body.get("title").toString();
        String content =
                body.get("content") == null ? null : body.get("content").toString();
        String media = toJsonIfNeeded(body.get("media"));
        if (StringUtils.isBlank(title)) {
            return ApiResponse.error(400, "title_empty");
        }
        if (StringUtils.isBlank(content) && StringUtils.isBlank(media)) {
            return ApiResponse.error(400, "content_or_media_empty");
        }

        ModerationResult moderationResult = moderationService.reviewPost(title, content, media);
        if (!moderationResult.isPass()) {
            Map<String, Object> data = new HashMap<>();
            data.put(
                    "reason",
                    moderationResult.getReasons().isEmpty()
                            ? "内容未通过审核"
                            : moderationResult.getReasons().get(0));
            data.put("reasons", moderationResult.getReasons());
            data.put("tags", moderationResult.getTags());
            return ApiResponse.error(422, "moderation_rejected", data);
        }

        DiscussPost discussPost = new DiscussPost();
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setMedia(media);
        discussPost.setUserId(currentUser.getId());
        discussPost.setCreateTime(new Date());
        discussPostService.addDiscussPost(discussPost);

        firePostPublishEvent(currentUser.getId(), discussPost.getId());
        redisTemplate.opsForSet().add(RedisKeyUtil.getPostScoreKey(), discussPost.getId());
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/top")
    public ApiResponse<Void> setTop(@PathVariable("id") int id) {
        User currentUser = hostHolder.getUser();
        if (currentUser == null) {
            return ApiResponse.error(403, "not_login");
        }
        if (currentUser.getType() != 1 && currentUser.getType() != 2) {
            return ApiResponse.error(403, "forbidden");
        }

        discussPostService.updateType(id, 1);
        firePostPublishEvent(currentUser.getId(), id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/wonderful")
    public ApiResponse<Void> setWonderful(@PathVariable("id") int id) {
        User currentUser = hostHolder.getUser();
        if (currentUser == null) {
            return ApiResponse.error(403, "not_login");
        }
        if (currentUser.getType() != 1 && currentUser.getType() != 2) {
            return ApiResponse.error(403, "forbidden");
        }

        discussPostService.updateStatus(id, 1);
        firePostPublishEvent(currentUser.getId(), id);
        redisTemplate.opsForSet().add(RedisKeyUtil.getPostScoreKey(), id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/delete")
    public ApiResponse<Void> setDelete(@PathVariable("id") int id) {
        User currentUser = hostHolder.getUser();
        if (currentUser == null) {
            return ApiResponse.error(403, "not_login");
        }
        if (currentUser.getType() != 1) {
            return ApiResponse.error(403, "forbidden");
        }

        discussPostService.updateStatus(id, 2);
        eventProducer.fireEvent(new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(currentUser.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id));
        return ApiResponse.success(null);
    }

    /**
     * 统一触发帖子发布/变更事件。
     *
     * @param userId 操作用户 ID
     * @param postId 帖子 ID
     */
    private void firePostPublishEvent(int userId, int postId) {
        eventProducer.fireEvent(new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(userId)
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(postId));
    }

    /**
     * 查询用户映射。
     *
     * @param userIds 用户 ID 集合
     * @return 用户映射
     */
    private Map<Integer, User> loadUserMap(Set<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, User> userMap = ApiResponseUtils.unwrap(userClient.getUsers(new ArrayList<>(userIds)));
        return userMap == null ? Collections.emptyMap() : userMap;
    }

    /**
     * 查询点赞数映射。
     *
     * @param entityType 点赞实体类型
     * @param entityIds 实体 ID 集合
     * @return 点赞数映射
     */
    private Map<Integer, Long> loadLikeCountMap(int entityType, List<Integer> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> body = new HashMap<>();
        body.put("entityType", entityType);
        body.put("entityIds", entityIds);
        Map<Integer, Long> likeCountMap = ApiResponseUtils.unwrap(likeClient.getEntityLikeCounts(body));
        return likeCountMap == null ? Collections.emptyMap() : likeCountMap;
    }

    /**
     * 查询点赞状态映射。
     *
     * @param userId 用户 ID
     * @param entityType 点赞实体类型
     * @param entityIds 实体 ID 集合
     * @return 点赞状态映射
     */
    private Map<Integer, Integer> loadLikeStatusMap(int userId, int entityType, List<Integer> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("entityType", entityType);
        body.put("entityIds", entityIds);
        Map<Integer, Integer> statusMap = ApiResponseUtils.unwrap(likeClient.getEntityLikeStatuses(body));
        return statusMap == null ? Collections.emptyMap() : statusMap;
    }

    /**
     * 兜底修正页码。
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

    /**
     * 把复杂 media 结构统一转成 JSON 字符串。
     *
     * @param value media 值
     * @return JSON 字符串
     */
    private String toJsonIfNeeded(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return JSON.toJSONString(value);
    }
}
