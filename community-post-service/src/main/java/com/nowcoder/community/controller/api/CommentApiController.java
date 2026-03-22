package com.nowcoder.community.controller.api;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.moderation.ModerationResult;
import com.nowcoder.community.service.moderation.ModerationService;
import com.nowcoder.community.util.ApiResponse;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import com.nowcoder.community.event.EventProducer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import com.alibaba.fastjson2.JSON;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentApiController implements CommunityConstant {
    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ModerationService moderationService;

    @PostMapping
    public ApiResponse<Void> add(@RequestBody Map<String, Object> body) {
        if (hostHolder.getUser() == null) {
            return ApiResponse.error(403, "not_login");
        }
        int discussPostId = Integer.parseInt(String.valueOf(body.get("discussPostId")));
        int entityType = Integer.parseInt(String.valueOf(body.get("entityType")));
        int entityId = Integer.parseInt(String.valueOf(body.get("entityId")));
        int targetId = body.get("targetId") == null ? 0 : Integer.parseInt(String.valueOf(body.get("targetId")));
        String content = body.get("content") == null ? "" : body.get("content").toString();
        String media = toJsonIfNeeded(body.get("media"));
        if (StringUtils.isBlank(content) && StringUtils.isBlank(media)) {
            return ApiResponse.error(400, "内容或媒体不能为空");
        }

        ModerationResult moderationResult = moderationService.reviewComment(content, media);
        if (!moderationResult.isPass()) {
            Map<String, Object> data = new HashMap<>();
            data.put("reason", moderationResult.getReasons().isEmpty() ? "内容未通过审核" : moderationResult.getReasons().get(0));
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

        Event event = new Event();
        event.setTopic(TOPIC_COMMENT);
        event.setEntityType(comment.getEntityType());
        event.setEntityId(comment.getEntityId());
        event.setUserId(hostHolder.getUser().getId());
        event.setData("postId", discussPostId);

        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) {
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        eventProducer.fireEvent(event);

        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            event = new Event();
            event.setTopic(TOPIC_PUBLISH);
            event.setEntityType(ENTITY_TYPE_POST);
            event.setEntityId(discussPostId);
            event.setUserId(hostHolder.getUser().getId());
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, discussPostId);
        }

        return ApiResponse.success(null);
    }

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
