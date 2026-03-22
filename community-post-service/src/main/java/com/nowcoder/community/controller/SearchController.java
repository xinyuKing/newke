package com.nowcoder.community.controller;

import com.nowcoder.community.client.LikeClient;
import com.nowcoder.community.client.UserClient;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.util.ApiResponseUtils;
import com.nowcoder.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements CommunityConstant {

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private UserClient userClient;

    @Autowired
    private LikeClient likeClient;

    //"/search?keyword=xxx"
    @RequestMapping(path = "/search",method = RequestMethod.GET)
    public String search(String keyword, Page page,Model model){
        //搜索帖子
        org.springframework.data.domain.Page<DiscussPost> searchResult = elasticsearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());

        List<Map<String,Object>> discussPosts=new ArrayList<>();
        if(searchResult!=null){
            List<Integer> userIds = new ArrayList<>();
            List<Integer> postIds = new ArrayList<>();
            for (DiscussPost post : searchResult) {
                if(!userIds.contains(post.getUserId())){
                    userIds.add(post.getUserId());
                }
                postIds.add(post.getId());
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

            for (DiscussPost post : searchResult) {
                Map<String,Object> map=new HashMap<>();
                map.put("post",post);
                map.put("user",userMap.get(post.getUserId()));
                long likeCount = likeCountMap.getOrDefault(post.getId(),0L);
                map.put("likeCount",likeCount);
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts",discussPosts);
        model.addAttribute("keyword",keyword);

        //分页信息
        page.setPath("/search?keyword="+keyword);
        page.setRows(searchResult==null?0:(int) searchResult.getTotalElements());

        return "/site/search";
    }
}
