package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.Product;
import com.shixi.ecommerce.domain.ProductStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByMerchantId(Long merchantId);

    List<Product> findByStatus(ProductStatus status);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByMerchantId(Long merchantId, Pageable pageable);

    @Query("select p from Product p where p.status = :status and "
            + "(:cursorTime is null or p.createdAt < :cursorTime "
            + "or (p.createdAt = :cursorTime and p.id < :cursorId)) "
            + "order by p.createdAt desc, p.id desc")
    List<Product> findByStatusCursor(
            @Param("status") ProductStatus status,
            @Param("cursorTime") LocalDateTime cursorTime,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query("select p from Product p where p.merchantId = :merchantId and "
            + "(:cursorTime is null or p.createdAt < :cursorTime "
            + "or (p.createdAt = :cursorTime and p.id < :cursorId)) "
            + "order by p.createdAt desc, p.id desc")
    List<Product> findByMerchantCursor(
            @Param("merchantId") Long merchantId,
            @Param("cursorTime") LocalDateTime cursorTime,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query("select p from Product p where p.status = :status and "
            + "(:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')) "
            + "or lower(p.description) like lower(concat('%', :keyword, '%'))) and "
            + "(:cursorTime is null or p.createdAt < :cursorTime "
            + "or (p.createdAt = :cursorTime and p.id < :cursorId)) "
            + "order by p.createdAt desc, p.id desc")
    List<Product> searchByStatusCursor(
            @Param("status") ProductStatus status,
            @Param("keyword") String keyword,
            @Param("cursorTime") LocalDateTime cursorTime,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    List<Product> findByStatusAndNameContainingIgnoreCaseOrderByIdDesc(
            ProductStatus status, String name, Pageable pageable);

    List<Product> findByStatusAndNameContainingIgnoreCaseAndIdLessThanOrderByIdDesc(
            ProductStatus status, String name, Long id, Pageable pageable);

    List<Product> findByIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);
}
