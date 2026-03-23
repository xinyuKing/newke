package com.nowcoder.community.service;

import com.nowcoder.community.dao.CommentMapper;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.EntityCount;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.SensitiveFilter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

/**
 * 评论领域服务。
 */
@Service
public class CommentService implements CommunityConstant {

    private final CommentMapper commentMapper;
    private final SensitiveFilter sensitiveFilter;
    private final DiscussPostService discussPostService;

    public CommentService(
            CommentMapper commentMapper, SensitiveFilter sensitiveFilter, DiscussPostService discussPostService) {
        this.commentMapper = commentMapper;
        this.sensitiveFilter = sensitiveFilter;
        this.discussPostService = discussPostService;
    }

    /**
     * 查询指定实体下的评论列表。
     *
     * @param entityType 实体类型
     * @param entityId 实体 ID
     * @param offset 偏移量
     * @param limit 条数
     * @return 评论列表
     */
    public List<Comment> findCommentsByEntity(int entityType, int entityId, int offset, int limit) {
        List<Comment> comments = commentMapper.selectCommentsByEntity(entityType, entityId, offset, limit);
        return comments == null ? Collections.emptyList() : comments;
    }

    /**
     * 查询实体评论数。
     *
     * @param entityType 实体类型
     * @param entityId 实体 ID
     * @return 评论数
     */
    public int findCountByEntity(int entityType, int entityId) {
        return commentMapper.selectCountByEntity(entityType, entityId);
    }

    /**
     * 批量查询多个实体下的评论。
     *
     * @param entityType 实体类型
     * @param entityIds 实体 ID 集合
     * @return 评论列表
     */
    public List<Comment> findCommentsByEntityIds(int entityType, List<Integer> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Comment> comments = commentMapper.selectCommentsByEntityIds(entityType, entityIds);
        return comments == null ? Collections.emptyList() : comments;
    }

    /**
     * 批量查询多个实体的评论总数。
     *
     * @param entityType 实体类型
     * @param entityIds 实体 ID 集合
     * @return 实体评论数映射
     */
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

    /**
     * 新增评论，并在必要时同步帖子评论数。
     *
     * @param comment 评论实体
     * @return 影响行数
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int addComment(Comment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("评论不能为空");
        }

        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        comment.setContent(sensitiveFilter.filter(comment.getContent()));
        int rows = commentMapper.insertComment(comment);

        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            int count = commentMapper.selectCountByEntity(comment.getEntityType(), comment.getEntityId());
            discussPostService.updateCommentCount(comment.getEntityId(), count);
        }
        return rows;
    }

    /**
     * 按 ID 查询评论。
     *
     * @param id 评论 ID
     * @return 评论实体
     */
    public Comment findCommentById(int id) {
        return commentMapper.selectCommentById(id);
    }

    /**
     * 查询指定用户的评论列表。
     *
     * @param userId 用户 ID
     * @param offset 偏移量
     * @param limit 条数
     * @return 评论列表
     */
    public List<Comment> findCommentsByUserId(int userId, int offset, int limit) {
        List<Comment> comments = commentMapper.selectCommentsByUserId(userId, offset, limit);
        return comments == null ? Collections.emptyList() : comments;
    }

    /**
     * 查询指定用户的评论总数。
     *
     * @param userId 用户 ID
     * @return 评论总数
     */
    public int findCommentRowsByUserId(int userId) {
        return commentMapper.selectCommentRowsByUserId(userId);
    }
}
