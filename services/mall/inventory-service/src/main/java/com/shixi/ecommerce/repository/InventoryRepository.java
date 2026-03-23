package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findBySkuId(Long skuId);

    @Modifying
    @Query("update Inventory i set i.availableStock = i.availableStock - :qty where i.skuId = :skuId and i.availableStock >= :qty")
    int deductStock(@Param("skuId") Long skuId, @Param("qty") Integer qty);

    @Modifying
    @Query("update Inventory i set i.availableStock = i.availableStock + :qty where i.skuId = :skuId")
    int releaseStock(@Param("skuId") Long skuId, @Param("qty") Integer qty);
}
