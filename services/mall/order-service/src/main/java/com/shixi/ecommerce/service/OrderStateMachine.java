package com.shixi.ecommerce.service;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.OrderStatus;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 订单状态流转状态机，统一校验状态与角色权限。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class OrderStateMachine {
    private final Map<Transition, Set<String>> rules = new HashMap<>();

    public OrderStateMachine() {
        register(OrderStatus.CREATED, OrderStatus.PAID, Set.of("USER"));
        register(OrderStatus.PAID, OrderStatus.SHIPPED, Set.of("MERCHANT", "ADMIN", "SUPPORT"));
        register(OrderStatus.SHIPPED, OrderStatus.COMPLETED, Set.of("USER"));
        register(OrderStatus.CREATED, OrderStatus.CANCELED, Set.of("SYSTEM", "USER", "ADMIN", "SUPPORT"));
    }

    /**
     * 校验状态流转与角色权限。
     *
     * @param from 当前状态
     * @param to   目标状态
     * @param role 操作角色
     */
    public void assertTransition(OrderStatus from, OrderStatus to, String role) {
        Transition transition = new Transition(from, to);
        Set<String> allowed = rules.get(transition);
        if (allowed == null) {
            throw new BusinessException("Invalid status transition: " + from + " -> " + to);
        }
        if (role == null || !allowed.contains(role)) {
            throw new BusinessException("Role not allowed for transition: " + role);
        }
    }

    private void register(OrderStatus from, OrderStatus to, Set<String> roles) {
        rules.put(new Transition(from, to), new HashSet<>(roles));
    }

    private static class Transition {
        private final OrderStatus from;
        private final OrderStatus to;

        private Transition(OrderStatus from, OrderStatus to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Transition other = (Transition) obj;
            return from == other.from && to == other.to;
        }

        @Override
        public int hashCode() {
            return (from == null ? 0 : from.hashCode() * 31) + (to == null ? 0 : to.hashCode());
        }
    }
}
