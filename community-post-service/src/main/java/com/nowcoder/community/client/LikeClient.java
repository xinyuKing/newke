package com.nowcoder.community.client;

import com.nowcoder.community.util.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "social-service", path = "/community/api/likes")
public interface LikeClient {
    @GetMapping("/count")
    ApiResponse<Long> getEntityLikeCount(@RequestParam("entityType") int entityType,
                                         @RequestParam("entityId") int entityId);

    @PostMapping("/counts")
    ApiResponse<Map<Integer, Long>> getEntityLikeCounts(@RequestBody Map<String, Object> body);

    @GetMapping("/status")
    ApiResponse<Integer> getEntityLikeStatus(@RequestParam("userId") int userId,
                                             @RequestParam("entityType") int entityType,
                                             @RequestParam("entityId") int entityId);

    @PostMapping("/statuses")
    ApiResponse<Map<Integer, Integer>> getEntityLikeStatuses(@RequestBody Map<String, Object> body);

    @GetMapping("/user-count")
    ApiResponse<Integer> getUserLikeCount(@RequestParam("userId") int userId);
}
