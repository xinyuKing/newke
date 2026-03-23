package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.Order;
import com.shixi.ecommerce.domain.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNo(String orderNo);

    Optional<Order> findByOrderNoAndUserId(String orderNo, Long userId);

    Optional<Order> findByOrderNoAndMerchantId(String orderNo, Long merchantId);

    @Modifying(clearAutomatically = true)
    @Query("update Order o set o.status = :to, o.updatedAt = CURRENT_TIMESTAMP "
            + "where o.orderNo = :orderNo and o.status = :from")
    int updateStatusIfMatch(
            @Param("orderNo") String orderNo, @Param("from") OrderStatus from, @Param("to") OrderStatus to);

    @Modifying(clearAutomatically = true)
    @Query("update Order o set o.status = :to, o.updatedAt = CURRENT_TIMESTAMP "
            + "where o.orderNo = :orderNo and o.userId = :userId and o.status = :from")
    int updateStatusIfMatchAndUser(
            @Param("orderNo") String orderNo,
            @Param("userId") Long userId,
            @Param("from") OrderStatus from,
            @Param("to") OrderStatus to);

    @Modifying(clearAutomatically = true)
    @Query("update Order o set o.status = :to, o.carrierCode = :carrierCode, o.trackingNo = :trackingNo, "
            + "o.shippedAt = CURRENT_TIMESTAMP, o.updatedAt = CURRENT_TIMESTAMP "
            + "where o.orderNo = :orderNo and o.status = :from")
    int updateShipInfo(
            @Param("orderNo") String orderNo,
            @Param("from") OrderStatus from,
            @Param("to") OrderStatus to,
            @Param("carrierCode") String carrierCode,
            @Param("trackingNo") String trackingNo);

    @Modifying(clearAutomatically = true)
    @Query("update Order o set o.status = :to, o.carrierCode = :carrierCode, o.trackingNo = :trackingNo, "
            + "o.shippedAt = CURRENT_TIMESTAMP, o.updatedAt = CURRENT_TIMESTAMP "
            + "where o.orderNo = :orderNo and o.merchantId = :merchantId and o.status = :from")
    int updateShipInfoByMerchant(
            @Param("orderNo") String orderNo,
            @Param("merchantId") Long merchantId,
            @Param("from") OrderStatus from,
            @Param("to") OrderStatus to,
            @Param("carrierCode") String carrierCode,
            @Param("trackingNo") String trackingNo);

    @Query("select o from Order o " + "where o.userId = :userId and "
            + "(:cursorTime is null or o.createdAt < :cursorTime "
            + "or (o.createdAt = :cursorTime and o.id < :cursorId)) "
            + "order by o.createdAt desc, o.id desc")
    List<Order> findByUserIdCursor(
            @Param("userId") Long userId,
            @Param("cursorTime") LocalDateTime cursorTime,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query("select o.userId from Order o where o.orderNo = :orderNo")
    Long findUserIdByOrderNo(@Param("orderNo") String orderNo);
}
