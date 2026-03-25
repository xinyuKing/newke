package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.UserAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 用户地址仓库。
 *
 * @author shixi
 * @date 2026-03-20
 */
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findByUserIdOrderByIsDefaultDescIdDesc(Long userId);

    Optional<UserAddress> findByIdAndUserId(Long id, Long userId);

    Optional<UserAddress> findFirstByUserIdAndIsDefaultTrueOrderByIdDesc(Long userId);

    Optional<UserAddress> findTopByUserIdAndIdNotOrderByIdDesc(Long userId, Long id);

    boolean existsByUserIdAndIsDefaultTrue(Long userId);

    long countByUserId(Long userId);

    @Modifying
    @Query("update UserAddress a set a.isDefault = false where a.userId = :userId and a.isDefault = true")
    int clearDefault(@Param("userId") Long userId);
}
