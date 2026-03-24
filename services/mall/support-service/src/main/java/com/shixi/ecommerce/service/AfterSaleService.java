package com.shixi.ecommerce.service;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.AfterSaleStatus;
import com.shixi.ecommerce.domain.AfterSaleTicket;
import com.shixi.ecommerce.dto.AfterSaleCreateRequest;
import com.shixi.ecommerce.dto.AfterSaleResponse;
import com.shixi.ecommerce.dto.OrderItemResponse;
import com.shixi.ecommerce.dto.OrderRefundSnapshotResponse;
import com.shixi.ecommerce.repository.AfterSaleTicketRepository;
import com.shixi.ecommerce.service.order.OrderAccessService;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AfterSaleService {
    private static final String CREATE_LOCK_PREFIX = "lock:after-sale:create:";
    private static final Duration CREATE_LOCK_TTL = Duration.ofSeconds(10);
    private static final String CREATE_LOCK_BUSY_MESSAGE = "After-sale request already in progress";
    private static final String CREATE_LOCK_ERROR_MESSAGE = "After-sale request coordination unavailable";
    private static final Set<AfterSaleStatus> CONSUMING_STATUSES =
            EnumSet.complementOf(EnumSet.of(AfterSaleStatus.REJECTED));

    private final AfterSaleTicketRepository repository;
    private final OrderAccessService orderAccessService;
    private final AfterSaleStateMachine stateMachine;
    private final StringRedisTemplate redisTemplate;

    public AfterSaleService(
            AfterSaleTicketRepository repository,
            OrderAccessService orderAccessService,
            AfterSaleStateMachine stateMachine,
            StringRedisTemplate redisTemplate) {
        this.repository = repository;
        this.orderAccessService = orderAccessService;
        this.stateMachine = stateMachine;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public AfterSaleResponse create(Long userId, AfterSaleCreateRequest request) {
        String lockKey = createLockKey(request.getOrderNo());
        String lockToken = acquireCreateLock(lockKey);
        try {
            OrderRefundSnapshotResponse snapshot =
                    orderAccessService.requireEligibleAfterSaleOrder(userId, request.getOrderNo());
            List<AfterSaleTicket> existingTickets = repository.findAllByOrderNo(request.getOrderNo());
            validateCreateRequest(request, snapshot, existingTickets);

            AfterSaleTicket ticket = new AfterSaleTicket();
            ticket.setUserId(userId);
            ticket.setOrderNo(request.getOrderNo());
            ticket.setSkuId(request.getSkuId());
            ticket.setQuantity(resolveQuantity(request, snapshot, existingTickets));
            ticket.setReason(request.getReason());
            ticket.setStatus(AfterSaleStatus.INIT);
            repository.save(ticket);
            return toResponse(ticket);
        } finally {
            releaseCreateLock(lockKey, lockToken);
        }
    }

    @Transactional(readOnly = true)
    public List<AfterSaleResponse> listUser(Long userId, AfterSaleStatus status) {
        List<AfterSaleTicket> tickets = status == null
                ? repository.findByUserIdOrderByCreatedAtDesc(userId)
                : repository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        return tickets.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AfterSaleResponse> listAll(AfterSaleStatus status) {
        List<AfterSaleTicket> tickets = status == null
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findByStatusOrderByCreatedAtDesc(status);
        return tickets.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public AfterSaleResponse updateStatus(Long id, AfterSaleStatus status) {
        AfterSaleTicket ticket =
                repository.findById(id).orElseThrow(() -> new BusinessException("After-sale not found"));
        stateMachine.assertTransition(ticket.getStatus(), status);
        if (ticket.getStatus() != status) {
            ticket.setStatus(status);
            repository.saveAndFlush(ticket);
            orderAccessService.syncAfterSaleStatus(ticket.getOrderNo(), status);
        }
        return toResponse(ticket);
    }

    private void validateCreateRequest(
            AfterSaleCreateRequest request,
            OrderRefundSnapshotResponse snapshot,
            List<AfterSaleTicket> existingTickets) {
        if (request.getQuantity() != null && request.getSkuId() == null) {
            throw new BusinessException("SkuId required when quantity is specified");
        }
        if (request.getSkuId() == null) {
            if (hasAnyConsumingTicket(existingTickets)) {
                throw new BusinessException("After-sale already exists for this order");
            }
            return;
        }
        OrderItemResponse orderItem = findOrderItem(snapshot, request.getSkuId());
        if (orderItem == null) {
            throw new BusinessException("Sku not found in order");
        }
        if (hasConsumingWholeOrderTicket(existingTickets)) {
            throw new BusinessException("After-sale already exists for this order");
        }
        int remainingQuantity = remainingQuantity(orderItem.getQuantity(), request.getSkuId(), existingTickets);
        if (remainingQuantity <= 0) {
            throw new BusinessException("After-sale quantity exhausted for this order item");
        }
        if (request.getQuantity() != null && request.getQuantity() > remainingQuantity) {
            throw new BusinessException("After-sale quantity exceeds remaining purchasable quantity");
        }
    }

    private boolean hasAnyConsumingTicket(List<AfterSaleTicket> existingTickets) {
        for (AfterSaleTicket existing : existingTickets) {
            if (isConsuming(existing)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasConsumingWholeOrderTicket(List<AfterSaleTicket> existingTickets) {
        for (AfterSaleTicket existing : existingTickets) {
            if (existing.getSkuId() == null && isConsuming(existing)) {
                return true;
            }
        }
        return false;
    }

    private Integer resolveQuantity(
            AfterSaleCreateRequest request,
            OrderRefundSnapshotResponse snapshot,
            List<AfterSaleTicket> existingTickets) {
        if (request.getSkuId() == null) {
            return null;
        }
        OrderItemResponse orderItem = findOrderItem(snapshot, request.getSkuId());
        if (orderItem == null) {
            throw new BusinessException("Sku not found in order");
        }
        int remainingQuantity = remainingQuantity(orderItem.getQuantity(), request.getSkuId(), existingTickets);
        if (request.getQuantity() != null) {
            return request.getQuantity();
        }
        return remainingQuantity;
    }

    private int remainingQuantity(Integer purchasedQuantity, Long skuId, List<AfterSaleTicket> existingTickets) {
        int usedQuantity = 0;
        for (AfterSaleTicket existing : existingTickets) {
            if (!Objects.equals(existing.getSkuId(), skuId) || !isConsuming(existing)) {
                continue;
            }
            usedQuantity += existing.getQuantity() == null ? 0 : existing.getQuantity();
        }
        return Math.max(0, (purchasedQuantity == null ? 0 : purchasedQuantity) - usedQuantity);
    }

    private boolean isConsuming(AfterSaleTicket ticket) {
        return ticket != null && ticket.getStatus() != null && CONSUMING_STATUSES.contains(ticket.getStatus());
    }

    private OrderItemResponse findOrderItem(OrderRefundSnapshotResponse snapshot, Long skuId) {
        if (snapshot.getItems() == null || skuId == null) {
            return null;
        }
        return snapshot.getItems().stream()
                .filter(item -> Objects.equals(item.getSkuId(), skuId))
                .findFirst()
                .orElse(null);
    }

    private AfterSaleResponse toResponse(AfterSaleTicket ticket) {
        return new AfterSaleResponse(
                ticket.getId(),
                ticket.getOrderNo(),
                ticket.getSkuId(),
                ticket.getQuantity(),
                ticket.getReason(),
                ticket.getStatus(),
                ticket.getCreatedAt());
    }

    private String createLockKey(String orderNo) {
        return CREATE_LOCK_PREFIX + orderNo;
    }

    private String acquireCreateLock(String lockKey) {
        String lockToken = UUID.randomUUID().toString();
        Boolean locked;
        try {
            locked = redisTemplate.opsForValue().setIfAbsent(lockKey, lockToken, CREATE_LOCK_TTL);
        } catch (RuntimeException ex) {
            throw new IllegalStateException(CREATE_LOCK_ERROR_MESSAGE, ex);
        }
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException(CREATE_LOCK_BUSY_MESSAGE);
        }
        return lockToken;
    }

    private void releaseCreateLock(String lockKey, String lockToken) {
        if (lockToken == null) {
            return;
        }
        try {
            String currentLockToken = redisTemplate.opsForValue().get(lockKey);
            if (Objects.equals(lockToken, currentLockToken)) {
                redisTemplate.delete(lockKey);
            }
        } catch (RuntimeException ignored) {
        }
    }
}
