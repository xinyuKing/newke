package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.AfterSaleStatus;
import com.shixi.ecommerce.domain.AfterSaleTicket;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AfterSaleTicketRepository extends JpaRepository<AfterSaleTicket, Long> {
    List<AfterSaleTicket> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AfterSaleTicket> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, AfterSaleStatus status);

    Optional<AfterSaleTicket> findByIdAndUserId(Long id, Long userId);

    Optional<AfterSaleTicket> findByOrderNo(String orderNo);

    List<AfterSaleTicket> findByStatusOrderByCreatedAtDesc(AfterSaleStatus status);

    List<AfterSaleTicket> findAllByOrderByCreatedAtDesc();

    long countByUserId(Long userId);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime createdAt);

    long countByUserIdAndStatusIn(Long userId, Collection<AfterSaleStatus> statuses);
}
