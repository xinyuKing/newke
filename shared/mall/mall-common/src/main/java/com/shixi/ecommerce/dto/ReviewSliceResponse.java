package com.shixi.ecommerce.dto;

import java.util.List;

/**
 * 评价分页响应（Slice）。
 *
 * @author shixi
 * @date 2026-03-20
 */
public class ReviewSliceResponse {
    private List<ReviewResponse> items;
    private boolean hasNext;

    public ReviewSliceResponse(List<ReviewResponse> items, boolean hasNext) {
        this.items = items;
        this.hasNext = hasNext;
    }

    public List<ReviewResponse> getItems() {
        return items;
    }

    public boolean isHasNext() {
        return hasNext;
    }
}