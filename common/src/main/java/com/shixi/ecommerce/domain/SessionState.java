package com.shixi.ecommerce.domain;

public enum SessionState {
    INIT,
    WAIT_ORDER,
    WAIT_REASON,
    WAIT_EVIDENCE,
    WAIT_PROOF,
    REVIEWING,
    APPROVED,
    REJECTED,
    DONE,
    HANDOFF
}
