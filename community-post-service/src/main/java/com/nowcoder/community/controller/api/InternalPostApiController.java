package com.nowcoder.community.controller.api;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.util.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal")
public class InternalPostApiController {
    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private ElasticsearchService elasticsearchService;

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
        List<DiscussPost> posts = discussPostService.findDiscussPostByUserId(userId, offset, limit);
        return ApiResponse.success(posts);
    }

    @GetMapping("/posts/user/{userId}/count")
    public ApiResponse<Integer> countPostsByUser(@PathVariable("userId") int userId) {
        return ApiResponse.success(discussPostService.findDiscussPostRowsByUserId(userId));
    }

    @GetMapping("/comments/user/{userId}")
    public ApiResponse<List<Comment>> listCommentsByUser(@PathVariable("userId") int userId,
                                                         @RequestParam("offset") int offset,
                                                         @RequestParam("limit") int limit) {
        List<Comment> comments = commentService.findCommentsByUserId(userId, offset, limit);
        return ApiResponse.success(comments);
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
