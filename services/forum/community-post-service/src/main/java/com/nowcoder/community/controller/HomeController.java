package com.nowcoder.community.controller;

import com.nowcoder.community.client.LikeClient;
import com.nowcoder.community.client.UserClient;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.util.ApiResponseUtils;
import com.nowcoder.community.util.CommunityConstant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 论坛首页控制器。
 */
@Controller
public class HomeController implements CommunityConstant {

    private final UserClient userClient;
    private final DiscussPostService discussPostService;
    private final LikeClient likeClient;

    public HomeController(UserClient userClient, DiscussPostService discussPostService, LikeClient likeClient) {
        this.userClient = userClient;
        this.discussPostService = discussPostService;
        this.likeClient = likeClient;
    }

    /**
     * 首页帖子列表。
     *
     * @param model 页面模型
     * @param page 分页对象
     * @param orderMode 排序模式
     * @return 视图名称
     */
    @GetMapping("/index")
    public String getIndexPage(
            Model model, Page page, @RequestParam(name = "orderMode", defaultValue = "0") int orderMode) {
        int safeOrderMode = orderMode == 1 ? 1 : 0;
        page.setRows(discussPostService.selectDiscussPostRows(0));
        page.setPath("/index?orderMode=" + safeOrderMode);

        List<DiscussPost> posts =
                discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit(), safeOrderMode);
        if (posts == null) {
            posts = Collections.emptyList();
        }

        Set<Integer> userIds = new LinkedHashSet<>();
        List<Integer> postIds = new ArrayList<>();
        for (DiscussPost post : posts) {
            userIds.add(post.getUserId());
            postIds.add(post.getId());
        }

        Map<Integer, User> userMap = ApiResponseUtils.unwrap(userClient.getUsers(new ArrayList<>(userIds)));
        if (userMap == null) {
            userMap = Collections.emptyMap();
        }
        Map<Integer, Long> likeCountMap = loadPostLikeCountMap(postIds);

        List<Map<String, Object>> discussPosts = new ArrayList<>();
        for (DiscussPost post : posts) {
            Map<String, Object> item = new HashMap<>();
            item.put("post", post);
            item.put("user", userMap.get(post.getUserId()));
            item.put("likeCount", likeCountMap.getOrDefault(post.getId(), 0L));
            discussPosts.add(item);
        }

        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("orderMode", safeOrderMode);
        return "/index";
    }

    /**
     * 500 页面。
     *
     * @return 视图名称
     */
    @GetMapping("/error")
    public String getErrorPage() {
        return "/error/500";
    }

    /**
     * 无权限访问提示页。
     *
     * @return 视图名称
     */
    @GetMapping("/denied")
    public String getDeniedPage() {
        return "/error/404";
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
