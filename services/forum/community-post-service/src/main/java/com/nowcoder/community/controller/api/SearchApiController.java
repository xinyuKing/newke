package com.nowcoder.community.controller.api;

import com.nowcoder.community.client.LikeClient;
import com.nowcoder.community.client.UserClient;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.util.ApiResponse;
import com.nowcoder.community.util.ApiResponseUtils;
import com.nowcoder.community.util.CommunityConstant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 搜索 REST 接口。
 */
@RestController
@RequestMapping("/api/search")
public class SearchApiController implements CommunityConstant {

    private final ElasticsearchService elasticsearchService;
    private final UserClient userClient;
    private final LikeClient likeClient;

    public SearchApiController(
            ElasticsearchService elasticsearchService, UserClient userClient, LikeClient likeClient) {
        this.elasticsearchService = elasticsearchService;
        this.userClient = userClient;
        this.likeClient = likeClient;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        int safePage = Math.max(page, 1);
        int safeLimit = limit > 0 ? limit : 10;
        String trimmedKeyword = StringUtils.trimToEmpty(keyword);

        Page<DiscussPost> searchResult =
                elasticsearchService.searchDiscussPost(trimmedKeyword, safePage - 1, safeLimit);
        if (searchResult == null) {
            searchResult = Page.empty();
        }

        Set<Integer> userIds = new LinkedHashSet<>();
        List<Integer> postIds = new ArrayList<>();
        for (DiscussPost post : searchResult) {
            userIds.add(post.getUserId());
            postIds.add(post.getId());
        }

        Map<Integer, User> userMap = ApiResponseUtils.unwrap(userClient.getUsers(new ArrayList<>(userIds)));
        if (userMap == null) {
            userMap = Collections.emptyMap();
        }
        Map<Integer, Long> likeCountMap = loadPostLikeCountMap(postIds);

        List<Map<String, Object>> items = new ArrayList<>();
        for (DiscussPost post : searchResult) {
            Map<String, Object> item = new HashMap<>();
            item.put("post", post);
            item.put("user", userMap.get(post.getUserId()));
            item.put("likeCount", likeCountMap.getOrDefault(post.getId(), 0L));
            items.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", items);
        data.put("keyword", trimmedKeyword);
        data.put("rows", (int) searchResult.getTotalElements());
        data.put("page", safePage);
        data.put("limit", safeLimit);
        return ApiResponse.success(data);
    }

    /**
     * 批量查询帖子点赞数。
     *
     * @param postIds 帖子 ID 集合
     * @return 点赞数映射
     */
    private Map<Integer, Long> loadPostLikeCountMap(List<Integer> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> likeBody = new HashMap<>();
        likeBody.put("entityType", LIKE_TYPE_POST);
        likeBody.put("entityIds", postIds);
        Map<Integer, Long> likeCountMap = ApiResponseUtils.unwrap(likeClient.getEntityLikeCounts(likeBody));
        return likeCountMap == null ? Collections.emptyMap() : likeCountMap;
    }
}
