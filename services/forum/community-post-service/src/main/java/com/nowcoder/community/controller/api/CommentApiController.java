package com.nowcoder.community.controller.api;

import com.alibaba.fastjson2.JSON;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.moderation.ModerationResult;
import com.nowcoder.community.service.moderation.ModerationService;
import com.nowcoder.community.util.ApiResponse;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论 REST 接口。
 */
@RestController
@RequestMapping("/api/comments")
public class CommentApiController implements CommunityConstant {

    private final CommentService commentService;
    private final HostHolder hostHolder;
    private final EventProducer eventProducer;
    private final DiscussPostService discussPostService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ModerationService moderationService;

    public CommentApiController(
            CommentService commentService,
            HostHolder hostHolder,
            EventProducer eventProducer,
            DiscussPostService discussPostService,
            RedisTemplate<String, Object> redisTemplate,
            ModerationService moderationService) {
        this.commentService = commentService;
        this.hostHolder = hostHolder;
        this.eventProducer = eventProducer;
        this.discussPostService = discussPostService;
        this.redisTemplate = redisTemplate;
        this.moderationService = moderationService;
    }

    @PostMapping
    public ApiResponse<Object> add(@RequestBody Map<String, Object> body) {
        if (hostHolder.getUser() == null) {
            return ApiResponse.error(403, "not_login");
        }
        if (body == null) {
            return ApiResponse.error(400, "invalid_request");
        }

        int discussPostId;
        int entityType;
        int entityId;
        int targetId;
        try {
            discussPostId = parseRequiredInt(body, "discussPostId");
            entityType = parseRequiredInt(body, "entityType");
            entityId = parseRequiredInt(body, "entityId");
            targetId = body.get("targetId") == null ? 0 : Integer.parseInt(String.valueOf(body.get("targetId")));
        } catch (NumberFormatException ex) {
            return ApiResponse.error(400, "invalid_request");
        }

        String content = body.get("content") == null ? "" : body.get("content").toString();
        String media = toJsonIfNeeded(body.get("media"));
        if (StringUtils.isBlank(content) && StringUtils.isBlank(media)) {
            return ApiResponse.error(400, "content_or_media_empty");
        }

        ModerationResult moderationResult = moderationService.reviewComment(content, media);
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

        Comment comment = new Comment();
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        comment.setEntityType(entityType);
        comment.setEntityId(entityId);
        comment.setTargetId(targetId);
        comment.setContent(content);
        comment.setMedia(media);
        commentService.addComment(comment);

        eventProducer.fireEvent(buildCommentEvent(comment, discussPostId));
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            eventProducer.fireEvent(new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(discussPostId)
                    .setUserId(hostHolder.getUser().getId()));
            redisTemplate.opsForSet().add(RedisKeyUtil.getPostScoreKey(), discussPostId);
        }
        return ApiResponse.success(null);
    }

    /**
     * 构造评论通知事件。
     *
     * @param comment 评论实体
     * @param discussPostId 帖子 ID
     * @return 事件对象
     */
    private Event buildCommentEvent(Comment comment, int discussPostId) {
        Event event = new Event()
                .setTopic(TOPIC_COMMENT)
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setUserId(hostHolder.getUser().getId())
                .setData("postId", discussPostId);

        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            DiscussPost targetPost = discussPostService.findDiscussPostById(comment.getEntityId());
            if (targetPost != null) {
                event.setEntityUserId(targetPost.getUserId());
            }
        } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) {
            Comment targetComment = commentService.findCommentById(comment.getEntityId());
            if (targetComment != null) {
                event.setEntityUserId(targetComment.getUserId());
            }
        }
        return event;
    }

    /**
     * 读取必填整型参数。
     *
     * @param body 请求体
     * @param key 参数名
     * @return 整型值
     */
    private int parseRequiredInt(Map<String, Object> body, String key) {
        return Integer.parseInt(String.valueOf(body.get(key)));
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
