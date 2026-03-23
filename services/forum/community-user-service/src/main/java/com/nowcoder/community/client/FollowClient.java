package com.nowcoder.community.client;

import com.nowcoder.community.util.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "social-service", path = "/community/api")
public interface FollowClient {
    @GetMapping("/followees/count")
    ApiResponse<Long> getFolloweeCount(@RequestParam("userId") int userId,
                                       @RequestParam("entityType") int entityType);

    @GetMapping("/followers/count")
    ApiResponse<Long> getFollowerCount(@RequestParam("entityType") int entityType,
                                       @RequestParam("entityId") int entityId);

    @GetMapping("/has-followed")
    ApiResponse<Boolean> hasFollowed(@RequestParam("userId") int userId,
                                     @RequestParam("entityType") int entityType,
                                     @RequestParam("entityId") int entityId);
}
