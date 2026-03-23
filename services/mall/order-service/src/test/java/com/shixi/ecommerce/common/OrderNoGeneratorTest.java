package com.shixi.ecommerce.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OrderNoGeneratorTest {

    @Test
    void nextOrderNoUsesUppercaseSortableFormat() {
        Set<String> orderNos = new HashSet<>();
        for (int i = 0; i < 32; i++) {
            String orderNo = OrderNoGenerator.nextOrderNo();
            assertEquals(29, orderNo.length());
            assertTrue(orderNo.matches("[0-9A-Z]+"));
            orderNos.add(orderNo);
        }
        assertTrue(orderNos.size() > 1);
    }
}
