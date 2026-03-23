package com.shixi.ecommerce.service;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.Order;
import com.shixi.ecommerce.dto.TrackingResponse;
import com.shixi.ecommerce.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 物流查询服务。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class LogisticsService {
    private final OrderRepository orderRepository;
    private final LogisticsClient logisticsClient;

    public LogisticsService(OrderRepository orderRepository, LogisticsClient logisticsClient) {
        this.orderRepository = orderRepository;
        this.logisticsClient = logisticsClient;
    }

    /**
     * 查询订单物流信息。
     *
     * @param userId  用户 ID
     * @param orderNo 订单号
     * @return 物流信息
     */
    @Transactional(readOnly = true)
    public TrackingResponse query(Long userId, String orderNo) {
        Order order = requireOrder(orderNo);
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("Forbidden");
        }
        return query(order);
    }

    @Transactional(readOnly = true)
    public TrackingResponse queryInternal(String orderNo) {
        return query(requireOrder(orderNo));
    }

    private Order requireOrder(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("Order not found"));
    }

    private TrackingResponse query(Order order) {
        if (order.getTrackingNo() == null || order.getCarrierCode() == null) {
            throw new BusinessException("Tracking info not ready");
        }
        return logisticsClient.query(order.getOrderNo(), order.getCarrierCode(), order.getTrackingNo());
    }
}
