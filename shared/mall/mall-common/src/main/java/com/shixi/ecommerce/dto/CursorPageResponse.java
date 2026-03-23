package com.shixi.ecommerce.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 游标分页响应。
 *
 * @param <T> 元素类型
 * @author shixi
 * @date 2026-03-20
 */
public class CursorPageResponse<T> {
    private List<T> items;
    private boolean hasNext;
    private LocalDateTime nextCursorTime;
    private Long nextCursorId;

    public CursorPageResponse(List<T> items, boolean hasNext, LocalDateTime nextCursorTime, Long nextCursorId) {
        this.items = items;
        this.hasNext = hasNext;
        this.nextCursorTime = nextCursorTime;
        this.nextCursorId = nextCursorId;
    }

    public List<T> getItems() {
        return items;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public LocalDateTime getNextCursorTime() {
        return nextCursorTime;
    }

    public Long getNextCursorId() {
        return nextCursorId;
    }
}