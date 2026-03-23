package com.shixi.ecommerce.service.agent.refund.data;

import com.shixi.ecommerce.domain.AfterSaleStatus;
import com.shixi.ecommerce.repository.AfterSaleTicketRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Service
public class RefundRiskDataService {
    private static final Set<AfterSaleStatus> OPEN_STATUSES = EnumSet.of(
            AfterSaleStatus.INIT,
            AfterSaleStatus.WAIT_PROOF,
            AfterSaleStatus.REVIEWING,
            AfterSaleStatus.APPROVED
    );

    private final AfterSaleTicketRepository repository;

    public RefundRiskDataService(AfterSaleTicketRepository repository) {
        this.repository = repository;
    }

    public RefundRiskProfile load(Long userId, String orderNo) {
        boolean existingAfterSaleTicket = orderNo != null && repository.findByOrderNo(orderNo).isPresent();
        if (userId == null) {
            return new RefundRiskProfile(0, 0, 0, existingAfterSaleTicket);
        }
        LocalDateTime since = LocalDateTime.now().minusDays(90);
        return new RefundRiskProfile(
                repository.countByUserId(userId),
                repository.countByUserIdAndCreatedAtAfter(userId, since),
                repository.countByUserIdAndStatusIn(userId, OPEN_STATUSES),
                existingAfterSaleTicket
        );
    }
}
