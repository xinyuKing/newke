package com.shixi.ecommerce.service;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.AfterSaleStatus;
import com.shixi.ecommerce.domain.AfterSaleTicket;
import com.shixi.ecommerce.dto.AfterSaleCreateRequest;
import com.shixi.ecommerce.dto.AfterSaleResponse;
import com.shixi.ecommerce.repository.AfterSaleTicketRepository;
import com.shixi.ecommerce.service.order.OrderAccessService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 鍞悗鏈嶅姟锛岃礋璐ｉ€€璐?閫€娆剧敵璇峰強鐘舵€佹祦杞€? *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class AfterSaleService {
    private final AfterSaleTicketRepository repository;
    private final OrderAccessService orderAccessService;

    public AfterSaleService(AfterSaleTicketRepository repository, OrderAccessService orderAccessService) {
        this.repository = repository;
        this.orderAccessService = orderAccessService;
    }

    /**
     * 鍒涘缓鍞悗鍗曘€?     *
     * @param userId 鐢ㄦ埛 ID
     * @param request 鍞悗璇锋眰
     * @return 鍞悗鍝嶅簲
     */
    @Transactional
    public AfterSaleResponse create(Long userId, AfterSaleCreateRequest request) {
        orderAccessService.requireEligibleAfterSaleOrder(userId, request.getOrderNo());
        repository.findByOrderNo(request.getOrderNo()).ifPresent(existing -> {
            throw new BusinessException("After-sale already exists");
        });
        AfterSaleTicket ticket = new AfterSaleTicket();
        ticket.setUserId(userId);
        ticket.setOrderNo(request.getOrderNo());
        ticket.setReason(request.getReason());
        ticket.setStatus(AfterSaleStatus.INIT);
        repository.save(ticket);
        return toResponse(ticket);
    }

    /**
     * 鏌ヨ鐢ㄦ埛鍞悗鍒楄〃銆?     *
     * @param userId 鐢ㄦ埛 ID
     * @param status 鐘舵€佺瓫閫?     * @return 鍞悗鍒楄〃
     */
    @Transactional(readOnly = true)
    public List<AfterSaleResponse> listUser(Long userId, AfterSaleStatus status) {
        List<AfterSaleTicket> tickets = status == null
                ? repository.findByUserIdOrderByCreatedAtDesc(userId)
                : repository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        return tickets.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 鏌ヨ鍏ㄩ儴鍞悗鍒楄〃锛堝鏈嶄晶锛夈€?     *
     * @param status 鐘舵€佺瓫閫?     * @return 鍞悗鍒楄〃
     */
    @Transactional(readOnly = true)
    public List<AfterSaleResponse> listAll(AfterSaleStatus status) {
        List<AfterSaleTicket> tickets = status == null
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findByStatusOrderByCreatedAtDesc(status);
        return tickets.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 鏇存柊鍞悗鐘舵€侊紙瀹㈡湇渚э級銆?     *
     * @param id 鍞悗鍗?ID
     * @param status 鐘舵€?     * @return 鏇存柊鍚庣殑鍞悗鍗?     */
    @Transactional
    public AfterSaleResponse updateStatus(Long id, AfterSaleStatus status) {
        AfterSaleTicket ticket =
                repository.findById(id).orElseThrow(() -> new BusinessException("After-sale not found"));
        ticket.setStatus(status);
        repository.save(ticket);
        return toResponse(ticket);
    }

    private AfterSaleResponse toResponse(AfterSaleTicket ticket) {
        return new AfterSaleResponse(
                ticket.getId(), ticket.getOrderNo(), ticket.getReason(), ticket.getStatus(), ticket.getCreatedAt());
    }
}
