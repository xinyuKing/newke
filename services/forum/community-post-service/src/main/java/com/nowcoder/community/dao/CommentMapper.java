package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.EntityCount;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentMapper {
    // 閫氳繃entityType锛堝尯鍒嗚瘎璁哄拰鍥炲锛?entityId鍒嗛〉鏌ヨ
    List<Comment> selectCommentsByEntity(int entityType, int entityId, int offset, int limit);

    // 閫氳繃entityType锛堝尯鍒嗚瘎璁哄拰鍥炲锛?entityId鏌ヨ鏁伴噺
    int selectCountByEntity(int entityType, int entityId);

    // 宸叆璇勮
    int insertComment(Comment comment);

    // 鏍规嵁id鏌ヨ
    Comment selectCommentById(int id);

    // 鏌ヨ鐢ㄦ埛鎵€鏈夌殑鍥炲笘鍜屽洖澶?
    List<Comment> selectCommentsByUserId(int userId, int offset, int limit);

    // 鏌ヨ鐢ㄦ埛鎵€鏈夌殑鍥炲笘鍜屽洖澶嶇殑鏁伴噺
    int selectCommentRowsByUserId(int userId);

    // 閫氳繃entityType鍜宨d鍒楄〃鏌ヨ璇勮/鍥炲
    List<Comment> selectCommentsByEntityIds(
            @Param("entityType") int entityType, @Param("entityIds") List<Integer> entityIds);

    // 閫氳繃entityType鍜宨d鍒楄〃缁熻鏁伴噺
    List<EntityCount> selectCountByEntityIds(
            @Param("entityType") int entityType, @Param("entityIds") List<Integer> entityIds);
}
