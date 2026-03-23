package com.nowcoder.community.controller;

import com.nowcoder.community.client.UserClient;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.util.ApiResponseUtils;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thymeleaf pages for comment history and comment submission.
 */
@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {

    private static final int PAGE_LIMIT = 5;

    private final CommentService commentService;
    private final HostHolder hostHolder;
    private final EventProducer eventProducer;
    private final DiscussPostService discussPostService;
    private final UserClient userClient;
    private final RedisTemplate<String, Object> redisTemplate;

    public CommentController(CommentService commentService,
                             HostHolder hostHolder,
                             EventProducer eventProducer,
                             DiscussPostService discussPostService,
                             UserClient userClient,
                             RedisTemplate<String, Object> redisTemplate) {
        this.commentService = commentService;
        this.hostHolder = hostHolder;
        this.eventProducer = eventProducer;
        this.discussPostService = discussPostService;
        this.userClient = userClient;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/myreply/{userId}")
    public String myReply(@PathVariable("userId") int userId, Page page, Model model) {
        int rows = commentService.findCommentRowsByUserId(userId);
        initPage(page, "/comment/myreply/" + userId, rows);

        User user = ApiResponseUtils.unwrap(userClient.getUser(userId));
        if (user == null) {
            return "/error/404";
        }

        model.addAttribute("user", user);
        model.addAttribute("rows", rows);

        List<Comment> comments =
                emptyIfNull(commentService.findCommentsByUserId(userId, page.getOffset(), page.getLimit()));
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        for (Comment comment : comments) {
            commentVoList.add(buildCommentView(comment));
        }

        model.addAttribute("comments", commentVoList);
        return "/site/my-reply";
    }

    @PostMapping("/add/{discussPostId}")
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) {
        User currentUser = hostHolder.getUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        comment.setCreateTime(new Date());
        comment.setUserId(currentUser.getId());
        comment.setStatus(0);
        commentService.addComment(comment);

        eventProducer.fireEvent(buildCommentEvent(currentUser.getId(), discussPostId, comment));

        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            notifyDiscussPostChanged(currentUser.getId(), discussPostId);
        }

        return "redirect:/discuss/detail/" + discussPostId;
    }

    private void initPage(Page page, String path, int rows) {
        page.setRows(rows);
        page.setLimit(PAGE_LIMIT);
        page.setPath(path);
    }

    private List<Comment> emptyIfNull(List<Comment> comments) {
        return comments == null ? Collections.emptyList() : comments;
    }

    private Map<String, Object> buildCommentView(Comment comment) {
        Map<String, Object> commentVo = new HashMap<>();
        commentVo.put("comment", comment);
        commentVo.put("post", resolveDiscussPost(comment));
        return commentVo;
    }

    private void notifyDiscussPostChanged(int currentUserId, int discussPostId) {
        eventProducer.fireEvent(new Event()
                .setTopic(TOPIC_PUBLISH)
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(discussPostId)
                .setUserId(currentUserId));
        redisTemplate.opsForSet().add(RedisKeyUtil.getPostScoreKey(), discussPostId);
    }

    private DiscussPost resolveDiscussPost(Comment comment) {
        Comment current = comment;
        while (current != null && current.getEntityType() == ENTITY_TYPE_COMMENT) {
            current = commentService.findCommentById(current.getEntityId());
        }
        if (current == null) {
            return null;
        }
        return discussPostService.findDiscussPostById(current.getEntityId());
    }

    private Event buildCommentEvent(int currentUserId, int discussPostId, Comment comment) {
        Event event = new Event()
                .setTopic(TOPIC_COMMENT)
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setUserId(currentUserId)
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
}
