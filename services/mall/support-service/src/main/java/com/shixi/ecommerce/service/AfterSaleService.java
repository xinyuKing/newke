package com.shixi.ecommerce.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.AfterSaleStatus;
import com.shixi.ecommerce.domain.AfterSaleTicket;
import com.shixi.ecommerce.dto.AfterSaleCreateRequest;
import com.shixi.ecommerce.dto.AfterSaleEvidenceRequest;
import com.shixi.ecommerce.dto.AfterSaleResponse;
import com.shixi.ecommerce.dto.OrderItemResponse;
import com.shixi.ecommerce.dto.OrderRefundSnapshotResponse;
import com.shixi.ecommerce.repository.AfterSaleTicketRepository;
import com.shixi.ecommerce.service.order.OrderAccessService;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
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
    private static final Set<AfterSaleStatus> EVIDENCE_EDITABLE_STATUSES =
            EnumSet.of(AfterSaleStatus.INIT, AfterSaleStatus.WAIT_PROOF, AfterSaleStatus.REVIEWING);
    private static final Set<AfterSaleStatus> ORDER_SYNC_STATUSES =
            EnumSet.of(AfterSaleStatus.APPROVED, AfterSaleStatus.REFUNDED);

    private final AfterSaleTicketRepository repository;
    private final OrderAccessService orderAccessService;
    private final AfterSaleStateMachine stateMachine;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AfterSaleService(
            AfterSaleTicketRepository repository,
            OrderAccessService orderAccessService,
            AfterSaleStateMachine stateMachine,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.orderAccessService = orderAccessService;
        this.stateMachine = stateMachine;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
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
            applyProductSnapshot(ticket, findOrderItem(snapshot, request.getSkuId()));
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
        validateEvidenceTransition(ticket, status);
        if (ticket.getStatus() != status) {
            ticket.setStatus(status);
            repository.saveAndFlush(ticket);
            if (ORDER_SYNC_STATUSES.contains(status)) {
                orderAccessService.syncAfterSaleStatus(ticket, repository.findAllByOrderNo(ticket.getOrderNo()));
            }
        }
        return toResponse(ticket);
    }

    @Transactional
    public AfterSaleResponse submitEvidence(Long userId, Long id, AfterSaleEvidenceRequest request) {
        AfterSaleTicket ticket = repository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException("After-sale not found"));
        if (!EVIDENCE_EDITABLE_STATUSES.contains(ticket.getStatus())) {
            throw new BusinessException("Current after-sale status does not accept evidence");
        }
        String evidenceNote = normalizeEvidenceNote(request == null ? null : request.getEvidenceNote());
        List<String> evidenceUrls = normalizeEvidenceUrls(request == null ? null : request.getEvidenceUrls());
        if ((evidenceNote == null || evidenceNote.isBlank()) && evidenceUrls.isEmpty()) {
            throw new BusinessException("Evidence note or URL required");
        }
        ticket.setEvidenceNote(evidenceNote);
        ticket.setEvidenceUrlsJson(writeEvidenceUrls(evidenceUrls));
        if (ticket.getStatus() == AfterSaleStatus.INIT || ticket.getStatus() == AfterSaleStatus.WAIT_PROOF) {
            ticket.setStatus(AfterSaleStatus.REVIEWING);
        }
        repository.saveAndFlush(ticket);
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
                ticket.getProductNameSnapshot(),
                ticket.getProductDescriptionSnapshot(),
                ticket.getReason(),
                ticket.getEvidenceNote(),
                readEvidenceUrls(ticket),
                ticket.getStatus(),
                ticket.getCreatedAt());
    }

    private void applyProductSnapshot(AfterSaleTicket ticket, OrderItemResponse orderItem) {
        if (ticket == null || orderItem == null) {
            return;
        }
        ticket.setProductNameSnapshot(orderItem.getProductName());
        ticket.setProductDescriptionSnapshot(orderItem.getProductDescription());
    }

    private void validateEvidenceTransition(AfterSaleTicket ticket, AfterSaleStatus targetStatus) {
        if (ticket.getStatus() == AfterSaleStatus.WAIT_PROOF
                && targetStatus == AfterSaleStatus.REVIEWING
                && !hasEvidence(ticket)) {
            throw new BusinessException("Evidence required before review");
        }
    }

    private boolean hasEvidence(AfterSaleTicket ticket) {
        return (ticket.getEvidenceNote() != null && !ticket.getEvidenceNote().isBlank())
                || !readEvidenceUrls(ticket).isEmpty();
    }

    private String normalizeEvidenceNote(String evidenceNote) {
        if (evidenceNote == null) {
            return null;
        }
        String normalized = evidenceNote.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private List<String> normalizeEvidenceUrls(List<String> evidenceUrls) {
        if (evidenceUrls == null || evidenceUrls.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String evidenceUrl : evidenceUrls) {
            if (evidenceUrl == null) {
                continue;
            }
            String trimmed = evidenceUrl.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(normalizeEvidenceUrl(trimmed));
            }
        }
        return normalized;
    }

    private String normalizeEvidenceUrl(String evidenceUrl) {
        try {
            URI uri = new URI(evidenceUrl);
            String scheme = uri.getScheme();
            if (scheme == null
                    || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null
                    || uri.getHost().isBlank()) {
                throw new BusinessException("Evidence URL must be an absolute http(s) link");
            }
            return uri.toASCIIString();
        } catch (URISyntaxException ex) {
            throw new BusinessException("Evidence URL must be a valid link");
        }
    }

    private List<String> readEvidenceUrls(AfterSaleTicket ticket) {
        if (ticket == null
                || ticket.getEvidenceUrlsJson() == null
                || ticket.getEvidenceUrlsJson().isBlank()) {
            return List.of();
        }
        try {
            List<String> evidenceUrls =
                    objectMapper.readValue(ticket.getEvidenceUrlsJson(), new TypeReference<List<String>>() {});
            if (evidenceUrls == null || evidenceUrls.isEmpty()) {
                return List.of();
            }
            return Collections.unmodifiableList(evidenceUrls);
        } catch (Exception ex) {
            throw new BusinessException("After-sale evidence corrupted");
        }
    }

    private String writeEvidenceUrls(List<String> evidenceUrls) {
        try {
            return objectMapper.writeValueAsString(evidenceUrls == null ? List.of() : evidenceUrls);
        } catch (Exception ex) {
            throw new BusinessException("Unable to save after-sale evidence");
        }
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
