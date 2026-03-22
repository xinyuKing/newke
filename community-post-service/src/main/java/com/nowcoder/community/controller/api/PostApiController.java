package com.nowcoder.community.controller.api;

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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import com.alibaba.fastjson2.JSON;

import java.util.*;

@RestController
@RequestMapping("/api/posts")
public class PostApiController implements CommunityConstant {
    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserClient userClient;

    @Autowired
    private LikeClient likeClient;

    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ModerationService moderationService;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int limit,
                                                 @RequestParam(defaultValue = "0") int orderMode) {
        int rows = discussPostService.selectDiscussPostRows(0);
        int offset = (page - 1) * limit;
        List<DiscussPost> posts = discussPostService.findDiscussPosts(0, offset, limit, orderMode);

        List<Integer> userIds = new ArrayList<>();
        List<Integer> postIds = new ArrayList<>();
        if (posts != null) {
            for (DiscussPost post : posts) {
                if (!userIds.contains(post.getUserId())) {
                    userIds.add(post.getUserId());
                }
                postIds.add(post.getId());
            }
        }
        Map<Integer, User> userMap = ApiResponseUtils.unwrap(userClient.getUsers(userIds));
        if (userMap == null) {
            userMap = new HashMap<>();
        }
        Map<String, Object> likeBody = new HashMap<>();
        likeBody.put("entityType", LIKE_TYPE_POST);
        likeBody.put("entityIds", postIds);
        Map<Integer, Long> likeCountMap = ApiResponseUtils.unwrap(likeClient.getEntityLikeCounts(likeBody));
        if (likeCountMap == null) {
            likeCountMap = new HashMap<>();
        }

        List<Map<String, Object>> items = new ArrayList<>();
        if (posts != null) {
            for (DiscussPost post : posts) {
                Map<String, Object> item = new HashMap<>();
                item.put("post", post);
                item.put("user", userMap.get(post.getUserId()));
                item.put("likeCount", likeCountMap.getOrDefault(post.getId(), 0L));
                items.add(item);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", items);
        Map<String, Object> pageInfo = new HashMap<>();
        pageInfo.put("current", page);
        pageInfo.put("limit", limit);
        pageInfo.put("rows", rows);
        pageInfo.put("total", rows % limit == 0 ? rows / limit : rows / limit + 1);
        data.put("page", pageInfo);
        return ApiResponse.success(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("id") int disPostId,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "5") int limit) {
        DiscussPost post = discussPostService.findDiscussPostById(disPostId);
        if (post == null) {
            return ApiResponse.error(404, "post_not_found");
        }
        User author = ApiResponseUtils.unwrap(userClient.getUser(post.getUserId()));

        int offset = (page - 1) * limit;
        List<Comment> comments = commentService.findCommentsByEntity(ENTITY_TYPE_POST, post.getId(), offset, limit);

        Long likeCountValue = ApiResponseUtils.unwrap(likeClient.getEntityLikeCount(LIKE_TYPE_POST, disPostId));
        long likeCount = likeCountValue == null ? 0L : likeCountValue;
        int likeStatus = 0;
        if (hostHolder.getUser() != null) {
            Integer status = ApiResponseUtils.unwrap(
                    likeClient.getEntityLikeStatus(hostHolder.getUser().getId(), LIKE_TYPE_POST, disPostId));
            likeStatus = status == null ? 0 : status;
        }

        List<Integer> commentIds = new ArrayList<>();
        if (comments != null) {
            for (Comment comment : comments) {
                commentIds.add(comment.getId());
            }
        }

        List<Comment> replyList = commentService.findCommentsByEntityIds(ENTITY_TYPE_COMMENT, commentIds);
        Map<Integer, List<Comment>> replyMap = new HashMap<>();
        for (Comment reply : replyList) {
            replyMap.computeIfAbsent(reply.getEntityId(), k -> new ArrayList<>()).add(reply);
        }
        Map<Integer, Integer> replyCountMap = commentService.findCountByEntityIds(ENTITY_TYPE_COMMENT, commentIds);

        Set<Integer> userIds = new HashSet<>();
        userIds.add(post.getUserId());
        if (comments != null) {
            for (Comment comment : comments) {
                userIds.add(comment.getUserId());
            }
        }
        for (Comment reply : replyList) {
            userIds.add(reply.getUserId());
            if (reply.getTargetId() != 0) {
                userIds.add(reply.getTargetId());
            }
        }
        Map<Integer, User> userMap = ApiResponseUtils.unwrap(userClient.getUsers(new ArrayList<>(userIds)));
        if (userMap == null) {
            userMap = new HashMap<>();
        }

        List<Integer> likeEntityIds = new ArrayList<>();
        likeEntityIds.addAll(commentIds);
        for (Comment reply : replyList) {
            likeEntityIds.add(reply.getId());
        }
        Map<String, Object> commentLikeBody = new HashMap<>();
        commentLikeBody.put("entityType", LIKE_TYPE_COMMENT);
        commentLikeBody.put("entityIds", likeEntityIds);
        Map<Integer, Long> likeCountMap = ApiResponseUtils.unwrap(likeClient.getEntityLikeCounts(commentLikeBody));
        if (likeCountMap == null) {
            likeCountMap = new HashMap<>();
        }
        Map<Integer, Integer> likeStatusMap;
        if (hostHolder.getUser() == null) {
            likeStatusMap = Collections.emptyMap();
        } else {
            Map<String, Object> statusBody = new HashMap<>();
            statusBody.put("userId", hostHolder.getUser().getId());
            statusBody.put("entityType", LIKE_TYPE_COMMENT);
            statusBody.put("entityIds", likeEntityIds);
            Map<Integer, Integer> statuses = ApiResponseUtils.unwrap(likeClient.getEntityLikeStatuses(statusBody));
            likeStatusMap = statuses == null ? Collections.emptyMap() : statuses;
        }

        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if (comments != null) {
            for (Comment comment : comments) {
                Map<String, Object> commentVo = new HashMap<>();
                commentVo.put("comment", comment);
                commentVo.put("user", userMap.get(comment.getUserId()));
                commentVo.put("likeCount", likeCountMap.getOrDefault(comment.getId(), 0L));
                commentVo.put("likeStatus", likeStatusMap.getOrDefault(comment.getId(), 0));

                List<Comment> replys = replyMap.getOrDefault(comment.getId(), Collections.emptyList());
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                for (Comment reply : replys) {
                    Map<String, Object> replyVo = new HashMap<>();
                    replyVo.put("reply", reply);
                    replyVo.put("user", userMap.get(reply.getUserId()));
                    replyVo.put("target", reply.getTargetId() == 0 ? null : userMap.get(reply.getTargetId()));
                    replyVo.put("likeCount", likeCountMap.getOrDefault(reply.getId(), 0L));
                    replyVo.put("likeStatus", likeStatusMap.getOrDefault(reply.getId(), 0));
                    replyVoList.add(replyVo);
                }
                commentVo.put("replys", replyVoList);
                commentVo.put("replyCount", replyCountMap.getOrDefault(comment.getId(), 0));
                commentVoList.add(commentVo);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("post", post);
        data.put("user", author);
        data.put("likeCount", likeCount);
        data.put("likeStatus", likeStatus);
        data.put("comments", commentVoList);
        data.put("rows", post.getCommentCount());
        data.put("page", page);
        data.put("limit", limit);
        return ApiResponse.success(data);
    }

    @PostMapping
    public ApiResponse<Void> add(@RequestBody Map<String, Object> body) {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(403, "not_login");
        }
        String title = body.get("title") == null ? null : body.get("title").toString();
        String content = body.get("content") == null ? null : body.get("content").toString();
        String media = toJsonIfNeeded(body.get("media"));
        if (StringUtils.isBlank(title)) {
            return ApiResponse.error(400, "标题不能为空");
        }
        if (StringUtils.isBlank(content) && StringUtils.isBlank(media)) {
            return ApiResponse.error(400, "内容或媒体不能为空");
        }

        ModerationResult moderationResult = moderationService.reviewPost(title, content, media);
        if (!moderationResult.isPass()) {
            Map<String, Object> data = new HashMap<>();
            data.put("reason", moderationResult.getReasons().isEmpty() ? "内容未通过审核" : moderationResult.getReasons().get(0));
            data.put("reasons", moderationResult.getReasons());
            data.put("tags", moderationResult.getTags());
            return ApiResponse.error(422, "moderation_rejected", data);
        }

        DiscussPost discussPost = new DiscussPost();
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setMedia(media);
        discussPost.setUserId(user.getId());
        discussPost.setCreateTime(new Date());
        discussPostService.addDiscussPost(discussPost);

        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(discussPost.getId());
        eventProducer.fireEvent(event);

        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, discussPost.getId());

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

    @PostMapping("/{id}/top")
    public ApiResponse<Void> setTop(@PathVariable("id") int id) {
        User current = hostHolder.getUser();
        if (current == null) {
            return ApiResponse.error(403, "not_login");
        }
        if (current.getType() != 1 && current.getType() != 2) {
            return ApiResponse.error(403, "forbidden");
        }
        discussPostService.updateType(id, 1);

        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/wonderful")
    public ApiResponse<Void> setWonderful(@PathVariable("id") int id) {
        User current = hostHolder.getUser();
        if (current == null) {
            return ApiResponse.error(403, "not_login");
        }
        if (current.getType() != 1 && current.getType() != 2) {
            return ApiResponse.error(403, "forbidden");
        }
        discussPostService.updateStatus(id, 1);

        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, id);

        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/delete")
    public ApiResponse<Void> setDelete(@PathVariable("id") int id) {
        User current = hostHolder.getUser();
        if (current == null) {
            return ApiResponse.error(403, "not_login");
        }
        if (current.getType() != 1) {
            return ApiResponse.error(403, "forbidden");
        }
        discussPostService.updateStatus(id, 2);

        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return ApiResponse.success(null);
    }
}
