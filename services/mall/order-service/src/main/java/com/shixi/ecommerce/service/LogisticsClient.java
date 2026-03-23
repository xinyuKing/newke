package com.shixi.ecommerce.service;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.dto.TrackingEvent;
import com.shixi.ecommerce.dto.TrackingResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 物流查询客户端。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class LogisticsClient {
    private final RestTemplate restTemplate;
    private final String provider;
    private final String baseUrl;
    private final String apiKey;

    public LogisticsClient(
            RestTemplate restTemplate,
            @Value("${logistics.provider:mock}") String provider,
            @Value("${logistics.api.url:}") String baseUrl,
            @Value("${logistics.api.key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.provider = provider;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    /**
     * 查询物流轨迹。
     *
     * @param orderNo     订单号
     * @param carrierCode 快递公司编码
     * @param trackingNo  运单号
     * @return 物流响应
     */
    public TrackingResponse query(String orderNo, String carrierCode, String trackingNo) {
        if (carrierCode == null || trackingNo == null) {
            throw new BusinessException("Tracking info missing");
        }
        if ("mock".equalsIgnoreCase(provider) || baseUrl == null || baseUrl.isBlank()) {
            return mock(orderNo, carrierCode, trackingNo);
        }
        String url = baseUrl + "/track?carrier={carrier}&trackingNo={trackingNo}&key={key}";
        TrackingResponse response =
                restTemplate.getForObject(url, TrackingResponse.class, carrierCode, trackingNo, apiKey);
        if (response == null) {
            throw new BusinessException("Logistics query failed");
        }
        return response;
    }

    private TrackingResponse mock(String orderNo, String carrierCode, String trackingNo) {
        List<TrackingEvent> events = List.of(
                new TrackingEvent(LocalDateTime.now().minusDays(1), "揽收", "上海"),
                new TrackingEvent(LocalDateTime.now().minusHours(8), "运输中", "苏州"),
                new TrackingEvent(LocalDateTime.now().minusHours(2), "派送中", "南京"));
        return new TrackingResponse(orderNo, carrierCode, trackingNo, "运输中", events);
    }
}
