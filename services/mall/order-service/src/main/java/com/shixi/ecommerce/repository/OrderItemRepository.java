package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.OrderItem;
import com.shixi.ecommerce.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    java.util.List<OrderItem> findByOrderNo(String orderNo);

    @Query("select case when count(oi) > 0 then true else false end "
            + "from OrderItem oi "
            + "where oi.skuId = :skuId "
            + "and oi.orderNo in ("
            + "select o.orderNo from Order o where o.userId = :userId and o.status = :status)")
    boolean existsByUserIdAndSkuIdAndOrderStatus(
            @Param("userId") Long userId, @Param("skuId") Long skuId, @Param("status") OrderStatus status);
}
