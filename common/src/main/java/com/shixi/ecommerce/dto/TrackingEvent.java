package com.shixi.ecommerce.dto;

import java.time.LocalDateTime;

/**
 * 鐗╂祦杞ㄨ抗鑺傜偣銆? *
 * @author shixi
 * @date 2026-03-20
 */
public class TrackingEvent {
    private LocalDateTime time;
    private String status;
    private String location;

    public TrackingEvent(LocalDateTime time, String status, String location) {
        this.time = time;
        this.status = status;
        this.location = location;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public String getStatus() {
        return status;
    }

    public String getLocation() {
        return location;
    }
}