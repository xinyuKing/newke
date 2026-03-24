package com.shixi.ecommerce.service;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.AfterSaleStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AfterSaleStateMachine {
    private final Map<AfterSaleStatus, Set<AfterSaleStatus>> transitions = new EnumMap<>(AfterSaleStatus.class);

    public AfterSaleStateMachine() {
        transitions.put(
                AfterSaleStatus.INIT,
                EnumSet.of(AfterSaleStatus.WAIT_PROOF, AfterSaleStatus.REVIEWING, AfterSaleStatus.REJECTED));
        transitions.put(
                AfterSaleStatus.WAIT_PROOF,
                EnumSet.of(AfterSaleStatus.WAIT_PROOF, AfterSaleStatus.REVIEWING, AfterSaleStatus.REJECTED));
        transitions.put(
                AfterSaleStatus.REVIEWING,
                EnumSet.of(AfterSaleStatus.WAIT_PROOF, AfterSaleStatus.APPROVED, AfterSaleStatus.REJECTED));
        transitions.put(AfterSaleStatus.APPROVED, EnumSet.of(AfterSaleStatus.REFUNDED));
        transitions.put(AfterSaleStatus.REJECTED, EnumSet.noneOf(AfterSaleStatus.class));
        transitions.put(AfterSaleStatus.REFUNDED, EnumSet.noneOf(AfterSaleStatus.class));
    }

    public void assertTransition(AfterSaleStatus from, AfterSaleStatus to) {
        if (from == null || to == null) {
            throw new BusinessException("After-sale status required");
        }
        if (from == to) {
            return;
        }
        Set<AfterSaleStatus> allowed = transitions.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new BusinessException("Invalid after-sale status transition: " + from + " -> " + to);
        }
    }
}
