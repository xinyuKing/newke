package com.shixi.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 鍙戣揣璇锋眰銆? *
 * @author shixi
 * @date 2026-03-20
 */
public class ShipOrderRequest {
    /** 蹇€掑叕鍙哥紪鐮併€?*/
    @NotBlank
    @Size(max = 32)
    private String carrierCode;

    /** 杩愬崟鍙枫€?*/
    @NotBlank
    @Size(max = 64)
    private String trackingNo;

    public String getCarrierCode() {
        return carrierCode;
    }

    public void setCarrierCode(String carrierCode) {
        this.carrierCode = carrierCode;
    }

    public String getTrackingNo() {
        return trackingNo;
    }

    public void setTrackingNo(String trackingNo) {
        this.trackingNo = trackingNo;
    }
}