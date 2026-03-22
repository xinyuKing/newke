package com.nowcoder.community.service;

import com.nowcoder.community.dao.CommentMapper;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import com.nowcoder.community.entity.EntityCount;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentService implements CommunityConstant {
    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private DiscussPostService discussPostService;

    public List<Comment> findCommentsByEntity(int entityType,int entityId,int offset,int limit){
        return commentMapper.selectCommentsByEntity(entityType, entityId, offset, limit);
    }

    public int findCountByEntity(int entityType,int entityId){
        return commentMapper.selectCountByEntity(entityType,entityId);
    }

    public List<Comment> findCommentsByEntityIds(int entityType, List<Integer> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return Collections.emptyList();
        }
        return commentMapper.selectCommentsByEntityIds(entityType, entityIds);
    }

    public Map<Integer, Integer> findCountByEntityIds(int entityType, List<Integer> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<EntityCount> counts = commentMapper.selectCountByEntityIds(entityType, entityIds);
        Map<Integer, Integer> result = new HashMap<>();
        if (counts != null) {
            for (EntityCount count : counts) {
                result.put(count.getEntityId(), count.getCount());
            }
        }
        return result;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED,propagation = Propagation.REQUIRED)
    public int addComment(Comment comment){
        //判断是否为空
        if(comment==null){
            throw new IllegalArgumentException("评论为空");
        }

        //添加评论
        //转义HTML标记(防止注入关键词导致页面变化)
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        //过滤敏感词
        comment.setContent(sensitiveFilter.filter(comment.getContent()));
        int rows = commentMapper.insertComment(comment);

        //更新帖子评论（注意和回复区分开来）数量
        if(comment.getEntityType()==ENTITY_TYPE_POST){
            int count = commentMapper.selectCountByEntity(comment.getEntityType(), comment.getEntityId());
            discussPostService.updateCommentCount(comment.getEntityId(),count);
        }

        return rows;
    }

    public Comment findCommentById(int id){
        return commentMapper.selectCommentById(id);
    }

    public List<Comment> findCommentsByUserId(int userId,int offset,int limit){return commentMapper.selectCommentsByUserId(userId, offset, limit);}

    public int findCommentRowsByUserId(int userId){return commentMapper.selectCommentRowsByUserId(userId);}
}
