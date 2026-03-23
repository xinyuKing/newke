package com.shixi.ecommerce.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cursor pagination response.
 *
 * @param <T> item type
 * @author shixi
 * @date 2026-03-20
 */
public class CursorPageResponse<T> {
    private List<T> items;
    private boolean hasNext;
    private LocalDateTime nextCursorTime;
    private Long nextCursorId;

    public CursorPageResponse() {}

    public CursorPageResponse(List<T> items, boolean hasNext, LocalDateTime nextCursorTime, Long nextCursorId) {
        this.items = items;
        this.hasNext = hasNext;
        this.nextCursorTime = nextCursorTime;
        this.nextCursorId = nextCursorId;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public LocalDateTime getNextCursorTime() {
        return nextCursorTime;
    }

    public void setNextCursorTime(LocalDateTime nextCursorTime) {
        this.nextCursorTime = nextCursorTime;
    }

    public Long getNextCursorId() {
        return nextCursorId;
    }

    public void setNextCursorId(Long nextCursorId) {
        this.nextCursorId = nextCursorId;
    }
}
