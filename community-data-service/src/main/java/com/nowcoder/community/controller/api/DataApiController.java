package com.nowcoder.community.controller.api;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DataService;
import com.nowcoder.community.util.ApiResponse;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/data")
public class DataApiController implements CommunityConstant {
    @Autowired
    private DataService dataService;

    @Autowired
    private HostHolder hostHolder;

    @GetMapping("/uv")
    public ApiResponse<Map<String, Object>> uv(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                                               @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date end) {
        if (!isAdmin()) {
            return ApiResponse.error(403, "forbidden");
        }
        long uv = dataService.calculateUV(start, end);
        Map<String, Object> data = new HashMap<>();
        data.put("uv", uv);
        data.put("start", start);
        data.put("end", end);
        return ApiResponse.success(data);
    }

    @GetMapping("/dau")
    public ApiResponse<Map<String, Object>> dau(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                                                @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date end) {
        if (!isAdmin()) {
            return ApiResponse.error(403, "forbidden");
        }
        long dau = dataService.calculateDAU(start, end);
        Map<String, Object> data = new HashMap<>();
        data.put("dau", dau);
        data.put("start", start);
        data.put("end", end);
        return ApiResponse.success(data);
    }

    private boolean isAdmin() {
        User user = hostHolder.getUser();
        return user != null && user.getType() == 1;
    }
}
