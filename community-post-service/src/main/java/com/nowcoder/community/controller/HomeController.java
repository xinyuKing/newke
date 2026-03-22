package com.nowcoder.community.controller;

import com.nowcoder.community.client.LikeClient;
import com.nowcoder.community.client.UserClient;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.util.ApiResponseUtils;
import com.nowcoder.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController implements CommunityConstant {
    @Autowired
    private UserClient userClient;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeClient likeClient;

    @RequestMapping(path = "/index",method = RequestMethod.GET)
    public String getIndexPage(Model model, Page page,@RequestParam(name="orderMode",defaultValue = "0") int orderMode){
        // 方法调用之前，SpringMVC会自动实例化Model和Page，并将Page注入Model
        // 所以再thymeleaf中可以直接访问Page对象中的数据
        page.setRows(discussPostService.selectDiscussPostRows(0));
        page.setPath("/index?orderMode="+orderMode);

        List<DiscussPost> list = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit(),orderMode);
        List<Map<String,Object>> discussPosts=new ArrayList<>();
        if(list!=null){
            List<Integer> userIds = new ArrayList<>();
            List<Integer> postIds = new ArrayList<>();
            for (DiscussPost post : list) {
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
            likeBody.put("entityType", LIKE_TYPE_POST);
            likeBody.put("entityIds", postIds);
            Map<Integer, Long> likeCountMap = ApiResponseUtils.unwrap(likeClient.getEntityLikeCounts(likeBody));
            if (likeCountMap == null) {
                likeCountMap = new HashMap<>();
            }

            for (DiscussPost post : list) {
                Map<String,Object> map=new HashMap<>();
                map.put("post",post);
                map.put("user",userMap.get(post.getUserId()));

                //????????????????
                //????
                long likeCount = likeCountMap.getOrDefault(post.getId(),0L);
                map.put("likeCount",likeCount);
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts",discussPosts);
        model.addAttribute("orderMode",orderMode);
        return "/index";
    }

    /*服务器错误*/
    @RequestMapping(path = "/error",method = RequestMethod.GET)
    public String getErrorPage(){
        return "/error/500";
    }

    /*拒绝访问时的提示页面*/
    @RequestMapping(path = "/denied", method = {RequestMethod.GET, RequestMethod.POST})
    public String getDeniedPage() {
        return "/error/404";
    }
}
