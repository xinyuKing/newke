package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.AfterSaleTicket;
import com.shixi.ecommerce.domain.AfterSaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AfterSaleTicketRepository extends JpaRepository<AfterSaleTicket, Long> {
    List<AfterSaleTicket> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AfterSaleTicket> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, AfterSaleStatus status);

    Optional<AfterSaleTicket> findByIdAndUserId(Long id, Long userId);

    Optional<AfterSaleTicket> findByOrderNo(String orderNo);

    List<AfterSaleTicket> findByStatusOrderByCreatedAtDesc(AfterSaleStatus status);

    List<AfterSaleTicket> findAllByOrderByCreatedAtDesc();
}
