package com.nowcoder.community.controller;

import com.nowcoder.community.client.LikeClient;
import com.nowcoder.community.client.UserClient;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.util.ApiResponseUtils;
import com.nowcoder.community.util.CommunityConstant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 论坛搜索页控制器。
 */
@Controller
public class SearchController implements CommunityConstant {

    private final ElasticsearchService elasticsearchService;
    private final UserClient userClient;
    private final LikeClient likeClient;

    public SearchController(ElasticsearchService elasticsearchService,
                            UserClient userClient,
                            LikeClient likeClient) {
        this.elasticsearchService = elasticsearchService;
        this.userClient = userClient;
        this.likeClient = likeClient;
    }

    /**
     * 搜索帖子并渲染结果页。
     *
     * @param keyword 关键词
     * @param page 分页对象
     * @param model 页面模型
     * @return 视图名称
     */
    @GetMapping("/search")
    public String search(String keyword, Page page, Model model) {
        String trimmedKeyword = StringUtils.trimToEmpty(keyword);
        Page<DiscussPost> searchResult =
                elasticsearchService.searchDiscussPost(trimmedKeyword, page.getCurrent() - 1, page.getLimit());
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

        List<Map<String, Object>> discussPosts = new ArrayList<>();
        for (DiscussPost post : searchResult) {
            Map<String, Object> item = new HashMap<>();
            item.put("post", post);
            item.put("user", userMap.get(post.getUserId()));
            item.put("likeCount", likeCountMap.getOrDefault(post.getId(), 0L));
            discussPosts.add(item);
        }

        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("keyword", trimmedKeyword);
        page.setPath("/search?keyword=" + trimmedKeyword);
        page.setRows((int) searchResult.getTotalElements());
        return "/site/search";
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
