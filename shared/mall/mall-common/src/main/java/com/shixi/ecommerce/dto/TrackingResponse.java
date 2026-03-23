package com.shixi.ecommerce.dto;

import java.util.List;

/**
 * 鐗╂祦鏌ヨ鍝嶅簲銆? *
 * @author shixi
 * @date 2026-03-20
 */
public class TrackingResponse {
    private String orderNo;
    private String carrierCode;
    private String trackingNo;
    private String status;
    private List<TrackingEvent> events;

    public TrackingResponse(String orderNo,
                            String carrierCode,
                            String trackingNo,
                            String status,
                            List<TrackingEvent> events) {
        this.orderNo = orderNo;
        this.carrierCode = carrierCode;
        this.trackingNo = trackingNo;
        this.status = status;
        this.events = events;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getCarrierCode() {
        return carrierCode;
    }

    public String getTrackingNo() {
        return trackingNo;
    }

    public String getStatus() {
        return status;
    }

    public List<TrackingEvent> getEvents() {
        return events;
    }
}