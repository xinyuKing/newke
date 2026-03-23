package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserId(Long userId);
    Optional<CartItem> findByUserIdAndSkuId(Long userId, Long skuId);
    void deleteByUserId(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("update CartItem c set c.quantity = c.quantity + :delta, c.priceSnapshot = :price, c.updatedAt = CURRENT_TIMESTAMP " +
            "where c.userId = :userId and c.skuId = :skuId")
    int increaseQuantity(@Param("userId") Long userId,
                         @Param("skuId") Long skuId,
                         @Param("delta") Integer delta,
                         @Param("price") java.math.BigDecimal price);

    @Modifying(clearAutomatically = true)
    @Query("update CartItem c set c.quantity = :quantity, c.updatedAt = CURRENT_TIMESTAMP " +
            "where c.userId = :userId and c.skuId = :skuId")
    int updateQuantity(@Param("userId") Long userId,
                       @Param("skuId") Long skuId,
                       @Param("quantity") Integer quantity);

    @Modifying(clearAutomatically = true)
    @Query("delete from CartItem c where c.userId = :userId and c.skuId = :skuId")
    int deleteByUserIdAndSkuId(@Param("userId") Long userId, @Param("skuId") Long skuId);
}
