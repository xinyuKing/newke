package com.nowcoder.community.client;

import com.nowcoder.community.util.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "post-service", path = "/community/api/internal/search")
public interface SearchClient {
    @PostMapping("/index/{postId}")
    ApiResponse<Void> indexPost(@PathVariable("postId") int postId);

    @DeleteMapping("/index/{postId}")
    ApiResponse<Void> deletePost(@PathVariable("postId") int postId);
}
