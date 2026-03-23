package com.nowcoder.community.client;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "post-service", path = "/community/api/internal")
public interface PostClient {
    @GetMapping("/posts/user/{userId}")
    ApiResponse<List<DiscussPost>> listPostsByUser(@PathVariable("userId") int userId,
                                                   @RequestParam("offset") int offset,
                                                   @RequestParam("limit") int limit);

    @GetMapping("/posts/user/{userId}/count")
    ApiResponse<Integer> countPostsByUser(@PathVariable("userId") int userId);

    @GetMapping("/comments/user/{userId}")
    ApiResponse<List<Comment>> listCommentsByUser(@PathVariable("userId") int userId,
                                                  @RequestParam("offset") int offset,
                                                  @RequestParam("limit") int limit);

    @GetMapping("/comments/user/{userId}/count")
    ApiResponse<Integer> countCommentsByUser(@PathVariable("userId") int userId);

    @GetMapping("/comments/{id}")
    ApiResponse<Comment> getComment(@PathVariable("id") int id);

    @GetMapping("/posts/{id}")
    ApiResponse<DiscussPost> getPost(@PathVariable("id") int id);
}
