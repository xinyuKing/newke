package com.nowcoder.community.controller.api;

import com.nowcoder.community.client.LikeClient;
import com.nowcoder.community.client.UserClient;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.util.ApiResponse;
import com.nowcoder.community.util.ApiResponseUtils;
import com.nowcoder.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/search")
public class SearchApiController implements CommunityConstant {
    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private UserClient userClient;

    @Autowired
    private LikeClient likeClient;

    @GetMapping
    public ApiResponse<Map<String, Object>> search(@RequestParam String keyword,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int limit) {
        Page<DiscussPost> searchResult = elasticsearchService.searchDiscussPost(keyword, page - 1, limit);
        List<Map<String, Object>> items = new ArrayList<>();

        List<Integer> userIds = new ArrayList<>();
        List<Integer> postIds = new ArrayList<>();
        if (searchResult != null) {
            for (DiscussPost post : searchResult) {
                if (!userIds.contains(post.getUserId())) {
                    userIds.add(post.getUserId());
                }
                postIds.add(post.getId());
            }
        }
        Map<Integer, User> userMap = ApiResponseUtils.unwrap(userClient.getUsers(userIds));
        if (userMap == null) {
            userMap = new HashMap<>();
        }
        Map<String, Object> likeBody = new HashMap<>();
        likeBody.put("entityType", ENTITY_TYPE_POST);
        likeBody.put("entityIds", postIds);
        Map<Integer, Long> likeCountMap = ApiResponseUtils.unwrap(likeClient.getEntityLikeCounts(likeBody));
        if (likeCountMap == null) {
            likeCountMap = new HashMap<>();
        }

        if (searchResult != null) {
            for (DiscussPost post : searchResult) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                map.put("user", userMap.get(post.getUserId()));
                map.put("likeCount", likeCountMap.getOrDefault(post.getId(), 0L));
                items.add(map);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", items);
        data.put("keyword", keyword);
        data.put("rows", searchResult == null ? 0 : (int) searchResult.getTotalElements());
        return ApiResponse.success(data);
    }
}
