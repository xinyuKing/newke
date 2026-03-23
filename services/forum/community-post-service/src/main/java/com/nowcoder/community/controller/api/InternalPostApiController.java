package com.nowcoder.community.controller.api;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.util.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 面向内部服务的帖子查询与搜索索引接口。
 */
@RestController
@RequestMapping("/api/internal")
public class InternalPostApiController {

    private final DiscussPostService discussPostService;
    private final CommentService commentService;
    private final ElasticsearchService elasticsearchService;

    public InternalPostApiController(DiscussPostService discussPostService,
                                     CommentService commentService,
                                     ElasticsearchService elasticsearchService) {
        this.discussPostService = discussPostService;
        this.commentService = commentService;
        this.elasticsearchService = elasticsearchService;
    }

    @GetMapping("/posts/{id}")
    public ApiResponse<DiscussPost> getPost(@PathVariable("id") int id) {
        DiscussPost post = discussPostService.findDiscussPostById(id);
        if (post == null) {
            return ApiResponse.error(404, "post_not_found");
        }
        return ApiResponse.success(post);
    }

    @GetMapping("/posts/user/{userId}")
    public ApiResponse<List<DiscussPost>> listPostsByUser(@PathVariable("userId") int userId,
                                                          @RequestParam("offset") int offset,
                                                          @RequestParam("limit") int limit) {
        return ApiResponse.success(discussPostService.findDiscussPostByUserId(userId, offset, limit));
    }

    @GetMapping("/posts/user/{userId}/count")
    public ApiResponse<Integer> countPostsByUser(@PathVariable("userId") int userId) {
        return ApiResponse.success(discussPostService.findDiscussPostRowsByUserId(userId));
    }

    @GetMapping("/comments/user/{userId}")
    public ApiResponse<List<Comment>> listCommentsByUser(@PathVariable("userId") int userId,
                                                         @RequestParam("offset") int offset,
                                                         @RequestParam("limit") int limit) {
        return ApiResponse.success(commentService.findCommentsByUserId(userId, offset, limit));
    }

    @GetMapping("/comments/user/{userId}/count")
    public ApiResponse<Integer> countCommentsByUser(@PathVariable("userId") int userId) {
        return ApiResponse.success(commentService.findCommentRowsByUserId(userId));
    }

    @GetMapping("/comments/{id}")
    public ApiResponse<Comment> getComment(@PathVariable("id") int id) {
        Comment comment = commentService.findCommentById(id);
        if (comment == null) {
            return ApiResponse.error(404, "comment_not_found");
        }
        return ApiResponse.success(comment);
    }

    @PostMapping("/search/index/{postId}")
    public ApiResponse<Void> indexPost(@PathVariable("postId") int postId) {
        DiscussPost post = discussPostService.findDiscussPostById(postId);
        if (post == null) {
            return ApiResponse.error(404, "post_not_found");
        }
        elasticsearchService.saveDiscussPost(post);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/search/index/{postId}")
    public ApiResponse<Void> deletePost(@PathVariable("postId") int postId) {
        elasticsearchService.deleteDiscussPost(postId);
        return ApiResponse.success(null);
    }
}
