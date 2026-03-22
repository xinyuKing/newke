package com.nowcoder.community.controller;

import com.nowcoder.community.client.LikeClient;
import com.nowcoder.community.client.UserClient;
import com.nowcoder.community.entity.*;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {
    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserClient userClient;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeClient likeClient;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ModerationService moderationService;

    @RequestMapping(path = "/mypost/{userId}",method = RequestMethod.GET)
    public String mypost(@PathVariable("userId")int userId,Page page,Model model){
        //配置分页信息
        int rows = discussPostService.findDiscussPostRowsByUserId(userId);
        page.setRows(rows);
        page.setPath("/discuss/mypost/"+userId);
        page.setLimit(5);

        model.addAttribute("rows",rows);
        User user = ApiResponseUtils.unwrap(userClient.getUser(userId));
        model.addAttribute("user",user);
        List<DiscussPost> posts = discussPostService.findDiscussPostByUserId(userId, page.getOffset(), page.getLimit());
        List<Map<String,Object>> postVoList=new ArrayList<>();
        List<Integer> postIds = new ArrayList<>();
        for (DiscussPost post : posts) {
            postIds.add(post.getId());
        }
        Map<String, Object> likeBody = new HashMap<>();
        likeBody.put("entityType", LIKE_TYPE_POST);
        likeBody.put("entityIds", postIds);
        Map<Integer, Long> likeCountMap = ApiResponseUtils.unwrap(likeClient.getEntityLikeCounts(likeBody));
        if (likeCountMap == null) {
            likeCountMap = new HashMap<>();
        }
        for (DiscussPost post : posts) {
            Map<String,Object> postVo=new HashMap<>();
            postVo.put("post",post);
            //????
            long likeCount = likeCountMap.getOrDefault(post.getId(),0L);
            postVo.put("likeCount",likeCount);
            postVoList.add(postVo);
        }
        model.addAttribute("posts",postVoList);
        return "/site/my-post";
    }

    @RequestMapping(path = "/add",method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title,String content){
        // 在Security中已经进行了拦截，这里可以不验证
        User user = hostHolder.getUser();
        if(user==null){
            return CommunityUtil.getJSONString(403,"您还没有登录!");
        }
        if (StringUtils.isBlank(title)) {
            return CommunityUtil.getJSONString(1, "标题不能为空");
        }
        if (StringUtils.isBlank(content)) {
            return CommunityUtil.getJSONString(1, "内容不能为空");
        }
        ModerationResult moderationResult = moderationService.reviewPost(title, content, null);
        if (!moderationResult.isPass()) {
            String reason = moderationResult.getReasons().isEmpty() ? "内容未通过审核" : moderationResult.getReasons().get(0);
            return CommunityUtil.getJSONString(1, reason);
        }
        DiscussPost discussPost = new DiscussPost();
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setUserId(user.getId());
        discussPost.setCreateTime(new Date());
        discussPostService.addDiscussPost(discussPost);

        //触发发帖事件
        Event event=new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                //在这里ENTITY_TYPE_POST更好理解一些，前面ENTITY_TYPE_COMMENT更好理解一些，常量设置少了
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(discussPost.getId());
        eventProducer.fireEvent(event);

        //用于计算帖子分数，记录需要刷新分数的帖子id
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey,discussPost.getId());

        //报错的情况将来统一处理
        return CommunityUtil.getJSONString(0,"发布成功！");
    }

    @RequestMapping(path = "/detail/{disPostId}",method = RequestMethod.GET)
    public String getDiscussPost(Model model, @PathVariable(name = "disPostId") int disPostId, Page page){
        //帖子
        DiscussPost post = discussPostService.findDiscussPostById(disPostId);
        model.addAttribute("post",post);
        //作者
        User user = ApiResponseUtils.unwrap(userClient.getUser(post.getUserId()));
        model.addAttribute("user",user);
        //评论分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/"+disPostId);
        page.setRows(post.getCommentCount());

        //评论：给帖子的评论
        //回复：给评论的评论
        //评论的列表
        //?????????
        //?????????
        //?????
        List<Comment> comments = commentService.findCommentsByEntity(ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());
        //???Vo??
        List<Map<String,Object>> commentVoList=new ArrayList<>();
        //????
        Long likeCount = ApiResponseUtils.unwrap(likeClient.getEntityLikeCount(LIKE_TYPE_POST, disPostId));
        model.addAttribute("likeCount", likeCount == null ? 0L : likeCount);
        int likeStatus = 0;
        if (hostHolder.getUser() != null) {
            Integer status = ApiResponseUtils.unwrap(
                    likeClient.getEntityLikeStatus(hostHolder.getUser().getId(), LIKE_TYPE_POST, disPostId));
            likeStatus = status == null ? 0 : status;
        }
        model.addAttribute("likeStatus", likeStatus);

        List<Integer> commentIds = new ArrayList<>();
        if(comments!=null){
            for (Comment comment : comments) {
                commentIds.add(comment.getId());
            }
        }

        List<Comment> replyList = commentService.findCommentsByEntityIds(ENTITY_TYPE_COMMENT, commentIds);
        Map<Integer, List<Comment>> replyMap = new HashMap<>();
        for(Comment reply: replyList){
            replyMap.computeIfAbsent(reply.getEntityId(), k -> new ArrayList<>()).add(reply);
        }
        Map<Integer, Integer> replyCountMap = commentService.findCountByEntityIds(ENTITY_TYPE_COMMENT, commentIds);

        Set<Integer> userIds = new HashSet<>();
        userIds.add(post.getUserId());
        if(comments!=null){
            for(Comment comment: comments){
                userIds.add(comment.getUserId());
            }
        }
        for(Comment reply: replyList){
            userIds.add(reply.getUserId());
            if(reply.getTargetId()!=0){
                userIds.add(reply.getTargetId());
            }
        }
        Map<Integer, User> userMap = ApiResponseUtils.unwrap(userClient.getUsers(new ArrayList<>(userIds)));
        if (userMap == null) {
            userMap = new HashMap<>();
        }

        List<Integer> likeEntityIds = new ArrayList<>();
        likeEntityIds.addAll(commentIds);
        for(Comment reply: replyList){
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

        if(comments!=null){
            for (Comment comment : comments) {
                //???Vo
                Map<String,Object> commentVo=new HashMap<>();
                commentVo.put("comment",comment);
                //?????
                User user1 = userMap.get(comment.getUserId());
                commentVo.put("user",user1);

                //????
                likeCount = likeCountMap.getOrDefault(comment.getId(),0L);
                commentVo.put("likeCount",likeCount);
                //????
                likeStatus = likeStatusMap.getOrDefault(comment.getId(),0);
                commentVo.put("likeStatus",likeStatus);

                //????
                List<Comment> replys = replyMap.getOrDefault(comment.getId(), Collections.emptyList());
                //???Vo??
                List<Map<String,Object>> replyVoList=new ArrayList<>();
                for (Comment reply : replys) {
                    //???Vo
                    Map<String,Object> replyVo=new HashMap<>();
                    replyVo.put("reply",reply);
                    //?????
                    User user2 = userMap.get(reply.getUserId());
                    replyVo.put("user",user2);
                    //?????
                    User target=reply.getTargetId()==0?null:userMap.get(reply.getTargetId());
                    replyVo.put("target",target);
                    //????
                    long likeCount1 = likeCountMap.getOrDefault(reply.getId(),0L);
                    replyVo.put("likeCount",likeCount1);
                    //????
                    int likeStatus1 = likeStatusMap.getOrDefault(reply.getId(),0);
                    replyVo.put("likeStatus",likeStatus1);

                    replyVoList.add(replyVo);
                }

                commentVo.put("replys",replyVoList);

                //????
                int replyCount = replyCountMap.getOrDefault(comment.getId(),0);
                commentVo.put("replyCount",replyCount);

                commentVoList.add(commentVo);
            }
        }

        model.addAttribute("comments",commentVoList);

        return "/site/discuss-detail";
    }

    //置顶
    @RequestMapping(path = "/top",method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id){
        discussPostService.updateType(id,1);

        //同步到elasticsearch中
        //触发发帖事件
        Event event=new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                //在这里ENTITY_TYPE_POST更好理解一些，前面ENTITY_TYPE_COMMENT更好理解一些，常量设置少了
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }

    //加精
    @RequestMapping(path = "/wonderful",method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id){
        discussPostService.updateStatus(id,1);

        //同步到elasticsearch中
        //触发发帖事件
        Event event=new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                //在这里ENTITY_TYPE_POST更好理解一些，前面ENTITY_TYPE_COMMENT更好理解一些，常量设置少了
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        //计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey,id);

        return CommunityUtil.getJSONString(0);
    }

    //删除
    @RequestMapping(path = "/delete",method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id){
        discussPostService.updateStatus(id,2);

        //同步到elasticsearch中
        //触发删帖事件
        Event event=new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(hostHolder.getUser().getId())
                //在这里ENTITY_TYPE_POST更好理解一些，前面ENTITY_TYPE_COMMENT更好理解一些，常量设置少了
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }
}
