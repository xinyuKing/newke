package com.shixi.ecommerce.web;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 网关熔断降级兜底接口。
 *
 * @author shixi
 * @date 2026-03-20
 */
@RestController
public class FallbackController {
    @GetMapping("/fallback")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, Object> fallback() {
        return Map.of("success", false, "message", "Service temporarily unavailable");
    }
}
