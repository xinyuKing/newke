package com.nowcoder.community.controller.api;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DataService;
import com.nowcoder.community.util.ApiResponse;
import com.nowcoder.community.util.HostHolder;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台统计开放接口。
 */
@RestController
@RequestMapping("/api/admin/data")
public class DataApiController {

    private static final int ADMIN_USER_TYPE = 1;

    private final DataService dataService;
    private final HostHolder hostHolder;

    public DataApiController(DataService dataService, HostHolder hostHolder) {
        this.dataService = dataService;
        this.hostHolder = hostHolder;
    }

    /**
     * 查询指定时间范围内的 UV。
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return UV 统计结果
     */
    @GetMapping("/uv")
    public ApiResponse<Map<String, Object>> uv(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date end) {
        if (!isAdmin()) {
            return ApiResponse.error(403, "forbidden");
        }
        long uv = dataService.calculateUV(start, end);
        Map<String, Object> data = new LinkedHashMap<>(3);
        data.put("uv", uv);
        data.put("start", start);
        data.put("end", end);
        return ApiResponse.success(data);
    }

    /**
     * 查询指定时间范围内的 DAU。
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return DAU 统计结果
     */
    @GetMapping("/dau")
    public ApiResponse<Map<String, Object>> dau(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date end) {
        if (!isAdmin()) {
            return ApiResponse.error(403, "forbidden");
        }
        long dau = dataService.calculateDAU(start, end);
        Map<String, Object> data = new LinkedHashMap<>(3);
        data.put("dau", dau);
        data.put("start", start);
        data.put("end", end);
        return ApiResponse.success(data);
    }

    /**
     * 判断当前用户是否为后台管理员。
     *
     * @return 是否管理员
     */
    private boolean isAdmin() {
        User user = hostHolder.getUser();
        return user != null && user.getType() == ADMIN_USER_TYPE;
    }
}
