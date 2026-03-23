package com.nowcoder.community.controller;

import com.nowcoder.community.client.LikeClient;
import com.nowcoder.community.client.UserClient;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.moderation.ModerationResult;
import com.nowcoder.community.service.moderation.ModerationService;
import com.nowcoder.community.util.ApiResponseUtils;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Thymeleaf pages for legacy discuss-post flows.
 */
@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {

    private static final int PAGE_LIMIT = 5;
    private static final int USER_TYPE_ADMIN = 1;
    private static final int USER_TYPE_MODERATOR = 2;

    private final DiscussPostService discussPostService;
    private final UserClient userClient;
    private final HostHolder hostHolder;
    private final CommentService commentService;
    private final LikeClient likeClient;
    private final EventProducer eventProducer;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ModerationService moderationService;

    public DiscussPostController(
            DiscussPostService discussPostService,
            UserClient userClient,
            HostHolder hostHolder,
            CommentService commentService,
            LikeClient likeClient,
            EventProducer eventProducer,
            RedisTemplate<String, Object> redisTemplate,
            ModerationService moderationService) {
        this.discussPostService = discussPostService;
        this.userClient = userClient;
        this.hostHolder = hostHolder;
        this.commentService = commentService;
        this.likeClient = likeClient;
        this.eventProducer = eventProducer;
        this.redisTemplate = redisTemplate;
        this.moderationService = moderationService;
    }

    @GetMapping("/mypost/{userId}")
    public String myPost(@PathVariable("userId") int userId, Page page, Model model) {
        int rows = discussPostService.findDiscussPostRowsByUserId(userId);
        initPage(page, "/discuss/mypost/" + userId, rows);

        User user = ApiResponseUtils.unwrap(userClient.getUser(userId));
        if (user == null) {
            return "/error/404";
        }

        model.addAttribute("rows", rows);
        model.addAttribute("user", user);

        List<DiscussPost> posts =
                emptyPostList(discussPostService.findDiscussPostByUserId(userId, page.getOffset(), page.getLimit()));
        List<Integer> postIds = new ArrayList<>();
        for (DiscussPost post : posts) {
            postIds.add(post.getId());
        }
        Map<Integer, Long> likeCountMap = loadLikeCountMap(LIKE_TYPE_POST, postIds);

        List<Map<String, Object>> postVoList = new ArrayList<>();
        for (DiscussPost post : posts) {
            postVoList.add(buildPostView(post, likeCountMap));
        }
        model.addAttribute("posts", postVoList);
        return "/site/my-post";
    }

    @PostMapping("/add")
    @ResponseBody
    public String addDiscussPost(String title, String content) {
        User currentUser = hostHolder.getUser();
        if (currentUser == null) {
            return CommunityUtil.getJSONString(403, "\u60a8\u8fd8\u6ca1\u6709\u767b\u5f55");
        }
        if (StringUtils.isBlank(title)) {
            return CommunityUtil.getJSONString(1, "\u6807\u9898\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (StringUtils.isBlank(content)) {
            return CommunityUtil.getJSONString(1, "\u5185\u5bb9\u4e0d\u80fd\u4e3a\u7a7a");
        }

        ModerationResult moderationResult = moderationService.reviewPost(title, content, null);
        if (!moderationResult.isPass()) {
            String reason = moderationResult.getReasons().isEmpty()
                    ? "\u5185\u5bb9\u672a\u901a\u8fc7\u5ba1\u6838"
                    : moderationResult.getReasons().get(0);
            return CommunityUtil.getJSONString(1, reason);
        }

        DiscussPost discussPost = new DiscussPost();
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setUserId(currentUser.getId());
        discussPost.setCreateTime(new Date());
        discussPostService.addDiscussPost(discussPost);

        firePostPublishEvent(currentUser.getId(), discussPost.getId());
        addPostScoreRefreshTask(discussPost.getId());
        return CommunityUtil.getJSONString(0, "\u53d1\u5e03\u6210\u529f");
    }

    @GetMapping("/detail/{postId}")
    public String getDiscussPost(Model model, @PathVariable("postId") int postId, Page page) {
        DiscussPost post = discussPostService.findDiscussPostById(postId);
        if (post == null) {
            return "/error/404";
        }

        initPage(page, "/discuss/detail/" + postId, post.getCommentCount());

        List<Comment> comments = emptyCommentList(
                commentService.findCommentsByEntity(ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit()));
        List<Integer> commentIds = new ArrayList<>();
        for (Comment comment : comments) {
            commentIds.add(comment.getId());
        }

        List<Comment> replyList =
                emptyCommentList(commentService.findCommentsByEntityIds(ENTITY_TYPE_COMMENT, commentIds));
        Map<Integer, List<Comment>> replyMap = new HashMap<>();
        for (Comment reply : replyList) {
            replyMap.computeIfAbsent(reply.getEntityId(), key -> new ArrayList<>())
                    .add(reply);
        }
        Map<Integer, Integer> replyCountMap = commentService.findCountByEntityIds(ENTITY_TYPE_COMMENT, commentIds);

        Set<Integer> userIds = collectUserIds(post, comments, replyList);
        Map<Integer, User> userMap = loadUserMap(userIds);

        List<Integer> commentLikeEntityIds = new ArrayList<>(commentIds);
        for (Comment reply : replyList) {
            commentLikeEntityIds.add(reply.getId());
        }
        Map<Integer, Long> commentLikeCountMap = loadLikeCountMap(LIKE_TYPE_COMMENT, commentLikeEntityIds);

        User currentUser = hostHolder.getUser();
        Map<Integer, Integer> commentLikeStatusMap = currentUser == null
                ? Collections.emptyMap()
                : loadLikeStatusMap(currentUser.getId(), LIKE_TYPE_COMMENT, commentLikeEntityIds);

        List<Map<String, Object>> commentVoList = new ArrayList<>();
        for (Comment comment : comments) {
            commentVoList.add(buildCommentView(
                    comment, replyMap, replyCountMap, userMap, commentLikeCountMap, commentLikeStatusMap));
        }

        Long postLikeCount = ApiResponseUtils.unwrap(likeClient.getEntityLikeCount(LIKE_TYPE_POST, postId));
        int postLikeStatus = 0;
        if (currentUser != null) {
            Integer status = ApiResponseUtils.unwrap(
                    likeClient.getEntityLikeStatus(currentUser.getId(), LIKE_TYPE_POST, postId));
            postLikeStatus = status == null ? 0 : status;
        }

        model.addAttribute("post", post);
        model.addAttribute("user", userMap.get(post.getUserId()));
        model.addAttribute("comments", commentVoList);
        model.addAttribute("likeCount", postLikeCount == null ? 0L : postLikeCount);
        model.addAttribute("likeStatus", postLikeStatus);
        return "/site/discuss-detail";
    }

    @PostMapping("/top")
    @ResponseBody
    public String setTop(int id) {
        User currentUser = hostHolder.getUser();
        if (currentUser == null) {
            return CommunityUtil.getJSONString(403, "\u60a8\u8fd8\u6ca1\u6709\u767b\u5f55");
        }
        if (!isManager(currentUser)) {
            return CommunityUtil.getJSONString(403, "\u65e0\u6743\u6267\u884c\u8be5\u64cd\u4f5c");
        }

        discussPostService.updateType(id, 1);
        firePostPublishEvent(currentUser.getId(), id);
        return CommunityUtil.getJSONString(0);
    }

    @PostMapping("/wonderful")
    @ResponseBody
    public String setWonderful(int id) {
        User currentUser = hostHolder.getUser();
        if (currentUser == null) {
            return CommunityUtil.getJSONString(403, "\u60a8\u8fd8\u6ca1\u6709\u767b\u5f55");
        }
        if (!isManager(currentUser)) {
            return CommunityUtil.getJSONString(403, "\u65e0\u6743\u6267\u884c\u8be5\u64cd\u4f5c");
        }

        discussPostService.updateStatus(id, 1);
        firePostPublishEvent(currentUser.getId(), id);
        addPostScoreRefreshTask(id);
        return CommunityUtil.getJSONString(0);
    }

    @PostMapping("/delete")
    @ResponseBody
    public String setDelete(int id) {
        User currentUser = hostHolder.getUser();
        if (currentUser == null) {
            return CommunityUtil.getJSONString(403, "\u60a8\u8fd8\u6ca1\u6709\u767b\u5f55");
        }
        if (!isAdmin(currentUser)) {
            return CommunityUtil.getJSONString(403, "\u65e0\u6743\u6267\u884c\u8be5\u64cd\u4f5c");
        }

        discussPostService.updateStatus(id, 2);
        eventProducer.fireEvent(new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(currentUser.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id));
        return CommunityUtil.getJSONString(0);
    }

    private void initPage(Page page, String path, int rows) {
        page.setLimit(PAGE_LIMIT);
        page.setPath(path);
        page.setRows(rows);
    }

    private List<DiscussPost> emptyPostList(List<DiscussPost> posts) {
        return posts == null ? Collections.emptyList() : posts;
    }

    private List<Comment> emptyCommentList(List<Comment> comments) {
        return comments == null ? Collections.emptyList() : comments;
    }

    private Map<String, Object> buildPostView(DiscussPost post, Map<Integer, Long> likeCountMap) {
        Map<String, Object> postVo = new HashMap<>();
        postVo.put("post", post);
        postVo.put("likeCount", likeCountMap.getOrDefault(post.getId(), 0L));
        return postVo;
    }

    private Set<Integer> collectUserIds(DiscussPost post, List<Comment> comments, List<Comment> replies) {
        Set<Integer> userIds = new LinkedHashSet<>();
        userIds.add(post.getUserId());
        for (Comment comment : comments) {
            userIds.add(comment.getUserId());
        }
        for (Comment reply : replies) {
            userIds.add(reply.getUserId());
            if (reply.getTargetId() != 0) {
                userIds.add(reply.getTargetId());
            }
        }
        return userIds;
    }

    private Map<String, Object> buildCommentView(
            Comment comment,
            Map<Integer, List<Comment>> replyMap,
            Map<Integer, Integer> replyCountMap,
            Map<Integer, User> userMap,
            Map<Integer, Long> commentLikeCountMap,
            Map<Integer, Integer> commentLikeStatusMap) {
        Map<String, Object> commentVo = new HashMap<>();
        commentVo.put("comment", comment);
        commentVo.put("user", userMap.get(comment.getUserId()));
        commentVo.put("likeCount", commentLikeCountMap.getOrDefault(comment.getId(), 0L));
        commentVo.put("likeStatus", commentLikeStatusMap.getOrDefault(comment.getId(), 0));
        commentVo.put(
                "replys",
                buildReplyViews(
                        replyMap.getOrDefault(comment.getId(), Collections.emptyList()),
                        userMap,
                        commentLikeCountMap,
                        commentLikeStatusMap));
        commentVo.put("replyCount", replyCountMap.getOrDefault(comment.getId(), 0));
        return commentVo;
    }

    private List<Map<String, Object>> buildReplyViews(
            List<Comment> replies,
            Map<Integer, User> userMap,
            Map<Integer, Long> commentLikeCountMap,
            Map<Integer, Integer> commentLikeStatusMap) {
        List<Map<String, Object>> replyVoList = new ArrayList<>();
        for (Comment reply : replies) {
            Map<String, Object> replyVo = new HashMap<>();
            replyVo.put("reply", reply);
            replyVo.put("user", userMap.get(reply.getUserId()));
            replyVo.put("target", reply.getTargetId() == 0 ? null : userMap.get(reply.getTargetId()));
            replyVo.put("likeCount", commentLikeCountMap.getOrDefault(reply.getId(), 0L));
            replyVo.put("likeStatus", commentLikeStatusMap.getOrDefault(reply.getId(), 0));
            replyVoList.add(replyVo);
        }
        return replyVoList;
    }

    private boolean isManager(User user) {
        return user != null && (user.getType() == USER_TYPE_ADMIN || user.getType() == USER_TYPE_MODERATOR);
    }

    private boolean isAdmin(User user) {
        return user != null && user.getType() == USER_TYPE_ADMIN;
    }

    private Map<Integer, User> loadUserMap(Set<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, User> userMap = ApiResponseUtils.unwrap(userClient.getUsers(new ArrayList<>(userIds)));
        return userMap == null ? Collections.emptyMap() : userMap;
    }

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

    private void firePostPublishEvent(int userId, int postId) {
        eventProducer.fireEvent(new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(userId)
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(postId));
    }

    private void addPostScoreRefreshTask(int postId) {
        redisTemplate.opsForSet().add(RedisKeyUtil.getPostScoreKey(), postId);
    }
}
