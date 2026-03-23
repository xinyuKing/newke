package com.nowcoder.community.controller.api;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DataService;
import com.nowcoder.community.util.ApiResponse;
import com.nowcoder.community.util.HostHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 访问埋点接口。
 */
@RestController
@RequestMapping("/api/track")
public class TrackApiController {

    private final DataService dataService;
    private final HostHolder hostHolder;

    public TrackApiController(DataService dataService, HostHolder hostHolder) {
        this.dataService = dataService;
        this.hostHolder = hostHolder;
    }

    /**
     * 记录当前请求的 UV / DAU。
     *
     * @param request HTTP 请求
     * @return 统一响应
     */
    @PostMapping
    public ApiResponse<Void> track(HttpServletRequest request) {
        dataService.recordUV(extractClientIp(request));

        User user = hostHolder.getUser();
        if (user != null) {
            dataService.recordDAU(user.getId());
        }
        return ApiResponse.success(null);
    }

    /**
     * 提取客户端真实 IP。
     *
     * @param request HTTP 请求
     * @return 客户端 IP
     */
    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            return ip.split(",")[0].trim();
        }
        return ip;
    }
}
