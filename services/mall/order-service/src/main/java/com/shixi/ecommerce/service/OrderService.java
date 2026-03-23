package com.shixi.ecommerce.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.common.OrderNoGenerator;
import com.shixi.ecommerce.domain.Order;
import com.shixi.ecommerce.domain.OrderItem;
import com.shixi.ecommerce.domain.OrderStatus;
import com.shixi.ecommerce.domain.ProductStatus;
import com.shixi.ecommerce.dto.CreateOrderRequest;
import com.shixi.ecommerce.dto.CreateOrderResponse;
import com.shixi.ecommerce.dto.CursorPageResponse;
import com.shixi.ecommerce.dto.OrderDetailResponse;
import com.shixi.ecommerce.dto.OrderItemResponse;
import com.shixi.ecommerce.dto.OrderLineItem;
import com.shixi.ecommerce.dto.OrderRefundSnapshotResponse;
import com.shixi.ecommerce.dto.OrderSummaryResponse;
import com.shixi.ecommerce.dto.ProductResponse;
import com.shixi.ecommerce.repository.OrderItemRepository;
import com.shixi.ecommerce.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 订单核心业务服务，负责下单、支付、发货、收货与库存扣减/释放。
 * 说明：价格以服务端为准；跨服务调用使用批量接口降低延迟。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class OrderService {
    private static final String SYSTEM_ROLE = "SYSTEM";
    private static final int ORDER_ITEM_BATCH_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Duration ORDER_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration ORDER_LIST_CACHE_TTL = Duration.ofMinutes(2);
    private static final String ORDER_VERSION_PREFIX = "ver:order:";
    private static final String ORDER_LIST_VERSION_PREFIX = "ver:order:list:";
    private static final String ORDER_CACHE_PREFIX = "cache:order:";
    private static final String ORDER_LIST_CACHE_PREFIX = "cache:order:list:";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryClient inventoryClient;
    private final IdempotencyService idempotencyService;
    private final OrderEventPublisher eventPublisher;
    private final ProductClient productClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final OrderStateMachine orderStateMachine;

    @PersistenceContext
    private EntityManager entityManager;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        InventoryClient inventoryClient,
                        IdempotencyService idempotencyService,
                        OrderEventPublisher eventPublisher,
                        ProductClient productClient,
                        StringRedisTemplate redisTemplate,
                        ObjectMapper objectMapper,
                        OrderStateMachine orderStateMachine) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.inventoryClient = inventoryClient;
        this.idempotencyService = idempotencyService;
        this.eventPublisher = eventPublisher;
        this.productClient = productClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.orderStateMachine = orderStateMachine;
    }

    /**
     * 单品下单入口，实际委托到多商品下单逻辑。
     *
     * @param request 下单请求
     * @return 下单结果
     */
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        OrderLineItem item = new OrderLineItem(request.getSkuId(), request.getQuantity(), request.getPrice());
        return createOrderByItems(request.getUserId(), request.getIdempotencyKey(), "order:create", List.of(item));
    }

    /**
     * 多商品下单，包含幂等控制、批量扣减库存、批量写入订单明细。
     *
     * @param userId         用户 ID
     * @param idempotencyKey 幂等键
     * @param bizPrefix      业务前缀
     * @param items          订单行
     * @return 下单结果
     */
    @Transactional
    public CreateOrderResponse createOrderByItems(Long userId, String idempotencyKey, String bizPrefix, List<OrderLineItem> items) {
        if (userId == null) {
            throw new BusinessException("UserId required");
        }
        if (items == null || items.isEmpty()) {
            throw new BusinessException("Empty order items");
        }
        String bizKey = bizPrefix + ":" + userId + ":" + idempotencyKey;
        boolean locked = idempotencyService.acquire(bizKey, Duration.ofMinutes(10));
        if (!locked) {
            throw new BusinessException("Duplicate order request");
        }
        boolean deducted = false;
        List<OrderLineItem> pricedItems = null;
        try {
            pricedItems = priceItems(items);
            boolean ok = inventoryClient.deductBatch(pricedItems);
            if (!ok) {
                throw new BusinessException("Insufficient stock");
            }
            deducted = true;
            String orderNo = OrderNoGenerator.nextOrderNo();
            Order order = new Order();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setStatus(OrderStatus.CREATED);
            List<OrderItem> orderItems = new ArrayList<>(pricedItems.size());
            BigDecimal total = BigDecimal.ZERO;
            for (OrderLineItem line : pricedItems) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderNo(orderNo);
                orderItem.setSkuId(line.getSkuId());
                orderItem.setQuantity(line.getQuantity());
                orderItem.setPrice(line.getPrice());
                orderItems.add(orderItem);
                total = total.add(line.getPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
            }
            order.setTotalAmount(total);
            orderRepository.save(order);
            saveOrderItems(orderItems);
            bumpOrderVersion(orderNo);
            bumpOrderListVersion(userId);

            eventPublisher.publishOrderCreated(orderNo);
            return new CreateOrderResponse(orderNo, order.getStatus());
        } catch (RuntimeException ex) {
            if (deducted) {
                inventoryClient.releaseBatch(pricedItems);
            }
            idempotencyService.release(bizKey);
            throw ex;
        }
    }

    /**
     * 支付订单：使用条件更新避免读写竞争。
     *
     * @param orderNo 订单号
     * @param role    操作角色
     */
    @Transactional
    public void payOrder(String orderNo, String role) {
        orderStateMachine.assertTransition(OrderStatus.CREATED, OrderStatus.PAID, role);
        int updated = orderRepository.updateStatusIfMatch(orderNo, OrderStatus.CREATED, OrderStatus.PAID);
        if (updated == 0) {
            throw new BusinessException("Order not payable or not found: " + orderNo);
        }
        bumpOrderVersion(orderNo);
        bumpOrderListVersionByOrderNo(orderNo);
        eventPublisher.publishOrderPaid(orderNo);
    }

    /**
     * 发货：将订单状态从已支付更新为已发货，并写入物流信息。
     *
     * @param orderNo     订单号
     * @param carrierCode 快递公司编码
     * @param trackingNo  运单号
     * @param role        操作角色
     */
    @Transactional
    public void shipOrder(String orderNo, String carrierCode, String trackingNo, String role) {
        if (carrierCode == null || carrierCode.isBlank()) {
            throw new BusinessException("Carrier code required");
        }
        if (trackingNo == null || trackingNo.isBlank()) {
            throw new BusinessException("Tracking number required");
        }
        orderStateMachine.assertTransition(OrderStatus.PAID, OrderStatus.SHIPPED, role);
        int updated = orderRepository.updateShipInfo(orderNo, OrderStatus.PAID, OrderStatus.SHIPPED,
                carrierCode.trim(), trackingNo.trim());
        if (updated == 0) {
            throw new BusinessException("Order not shippable or not found: " + orderNo);
        }
        bumpOrderVersion(orderNo);
        bumpOrderListVersionByOrderNo(orderNo);
        eventPublisher.publishOrderShipped(orderNo);
    }

    /**
     * 取消未支付订单，并释放库存。
     *
     * @param orderNo 订单号
     * @return 是否取消成功
     */
    @Transactional
    public boolean cancelIfUnpaid(String orderNo) {
        orderStateMachine.assertTransition(OrderStatus.CREATED, OrderStatus.CANCELED, SYSTEM_ROLE);
        int updated = orderRepository.updateStatusIfMatch(orderNo, OrderStatus.CREATED, OrderStatus.CANCELED);
        if (updated == 0) {
            return false;
        }
        List<OrderItem> items = orderItemRepository.findByOrderNo(orderNo);
        if (!items.isEmpty()) {
            inventoryClient.releaseBatch(items.stream()
                    .map(item -> new OrderLineItem(item.getSkuId(), item.getQuantity(), item.getPrice()))
                    .collect(Collectors.toList()));
        }
        bumpOrderVersion(orderNo);
        bumpOrderListVersionByOrderNo(orderNo);
        return true;
    }

    /**
     * 用户确认收货。
     *
     * @param userId  用户 ID
     * @param orderNo 订单号
     * @param role    操作角色
     */
    @Transactional
    public void confirmReceipt(Long userId, String orderNo, String role) {
        orderStateMachine.assertTransition(OrderStatus.SHIPPED, OrderStatus.COMPLETED, role);
        int updated = orderRepository.updateStatusIfMatchAndUser(orderNo, userId, OrderStatus.SHIPPED, OrderStatus.COMPLETED);
        if (updated == 0) {
            throw new BusinessException("Order not shippable or not found: " + orderNo);
        }
        bumpOrderVersion(orderNo);
        bumpOrderListVersion(userId);
        eventPublisher.publishOrderCompleted(orderNo);
    }

    /**
     * 查询订单详情（带缓存）。
     *
     * @param userId  用户 ID
     * @param orderNo 订单号
     * @return 订单详情
     */
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long userId, String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new BusinessException("OrderNo required");
        }
        String version = getVersion(orderVersionKey(orderNo));
        String cacheKey = ORDER_CACHE_PREFIX + orderNo + ":v" + version;
        OrderDetailResponse cached = getCache(cacheKey, new TypeReference<OrderDetailResponse>() {});
        if (cached != null) {
            return cached;
        }
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("Forbidden");
        }
        List<OrderItemResponse> items = orderItemRepository.findByOrderNo(orderNo).stream()
                .map(item -> new OrderItemResponse(item.getSkuId(), item.getQuantity(), item.getPrice()))
                .collect(Collectors.toList());
        OrderDetailResponse response = new OrderDetailResponse(
                order.getOrderNo(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCarrierCode(),
                order.getTrackingNo(),
                order.getShippedAt(),
                order.getCreatedAt(),
                items
        );
        setCache(cacheKey, response, ORDER_CACHE_TTL);
        return response;
    }

    @Transactional(readOnly = true)
    public OrderRefundSnapshotResponse getRefundSnapshot(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new BusinessException("OrderNo required");
        }
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("Order not found"));
        List<OrderItemResponse> items = orderItemRepository.findByOrderNo(orderNo).stream()
                .map(item -> new OrderItemResponse(item.getSkuId(), item.getQuantity(), item.getPrice()))
                .collect(Collectors.toList());
        return new OrderRefundSnapshotResponse(
                order.getOrderNo(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCarrierCode(),
                order.getTrackingNo(),
                order.getShippedAt(),
                order.getCreatedAt(),
                items
        );
    }

    /**
     * 用户订单游标分页查询（带缓存）。
     *
     * @param userId     用户 ID
     * @param cursorTime 游标时间
     * @param cursorId   游标 ID
     * @param size       每页大小
     * @return 游标分页结果
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<OrderSummaryResponse> listOrdersCursor(Long userId,
                                                                     LocalDateTime cursorTime,
                                                                     Long cursorId,
                                                                     Integer size) {
        int pageSize = normalizeSize(size);
        String version = getVersion(orderListVersionKey(userId));
        String cursorTimeKey = cursorTime == null ? "none" : cursorTime.toString();
        String cursorIdKey = cursorId == null ? "none" : String.valueOf(cursorId);
        String cacheKey = ORDER_LIST_CACHE_PREFIX + userId + ":v" + version + ":" + cursorTimeKey + ":" + cursorIdKey + ":" + pageSize;
        CursorPageResponse<OrderSummaryResponse> cached = getCache(cacheKey,
                new TypeReference<CursorPageResponse<OrderSummaryResponse>>() {});
        if (cached != null) {
            return cached;
        }
        List<Order> orders = orderRepository.findByUserIdCursor(
                userId,
                cursorTime,
                cursorId,
                PageRequest.of(0, pageSize + 1, Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))));
        boolean hasNext = orders.size() > pageSize;
        if (hasNext) {
            orders = orders.subList(0, pageSize);
        }
        LocalDateTime nextTime = null;
        Long nextId = null;
        if (!orders.isEmpty()) {
            Order last = orders.get(orders.size() - 1);
            nextTime = last.getCreatedAt();
            nextId = last.getId();
        }
        List<OrderSummaryResponse> items = orders.stream()
                .map(order -> new OrderSummaryResponse(
                        order.getOrderNo(),
                        order.getStatus(),
                        order.getTotalAmount(),
                        order.getCreatedAt()))
                .collect(Collectors.toList());
        CursorPageResponse<OrderSummaryResponse> response = new CursorPageResponse<>(items, hasNext, nextTime, nextId);
        setCache(cacheKey, response, ORDER_LIST_CACHE_TTL);
        return response;
    }

    /**
     * 订单明细批量入库，超大订单进行分批 flush/clear 以控制内存占用。
     *
     * @param items 明细列表
     */
    private void saveOrderItems(List<OrderItem> items) {
        if (items.isEmpty()) {
            return;
        }
        if (items.size() <= ORDER_ITEM_BATCH_SIZE) {
            orderItemRepository.saveAll(items);
            return;
        }
        for (int i = 0; i < items.size(); i += ORDER_ITEM_BATCH_SIZE) {
            int end = Math.min(i + ORDER_ITEM_BATCH_SIZE, items.size());
            List<OrderItem> batch = items.subList(i, end);
            orderItemRepository.saveAll(batch);
            orderItemRepository.flush();
            entityManager.clear();
        }
    }

    /**
     * 服务端定价：批量获取商品价格与状态，避免客户端价格被篡改。
     *
     * @param items 订单行
     * @return 定价后的订单行
     */
    private List<OrderLineItem> priceItems(List<OrderLineItem> items) {
        Set<Long> skuIds = new LinkedHashSet<>();
        for (OrderLineItem item : items) {
            skuIds.add(item.getSkuId());
        }
        List<ProductResponse> products = productClient.getProducts(new ArrayList<>(skuIds));
        Map<Long, ProductResponse> productMap = new HashMap<>(products.size() * 2);
        for (ProductResponse product : products) {
            productMap.put(product.getId(), product);
        }
        if (productMap.size() != skuIds.size()) {
            throw new BusinessException("Product not found");
        }
        List<OrderLineItem> pricedItems = new ArrayList<>(items.size());
        for (OrderLineItem item : items) {
            ProductResponse product = productMap.get(item.getSkuId());
            if (product == null) {
                throw new BusinessException("Product not found: " + item.getSkuId());
            }
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new BusinessException("Product inactive: " + item.getSkuId());
            }
            pricedItems.add(new OrderLineItem(item.getSkuId(), item.getQuantity(), product.getPrice()));
        }
        return pricedItems;
    }

    private String orderVersionKey(String orderNo) {
        return ORDER_VERSION_PREFIX + orderNo;
    }

    private String orderListVersionKey(Long userId) {
        return ORDER_LIST_VERSION_PREFIX + userId;
    }

    private String getVersion(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return "0";
        }
        return value;
    }

    private void bumpOrderVersion(String orderNo) {
        redisTemplate.opsForValue().increment(orderVersionKey(orderNo));
    }

    private void bumpOrderListVersion(Long userId) {
        if (userId == null) {
            return;
        }
        redisTemplate.opsForValue().increment(orderListVersionKey(userId));
    }

    private void bumpOrderListVersionByOrderNo(String orderNo) {
        Long userId = orderRepository.findUserIdByOrderNo(orderNo);
        bumpOrderListVersion(userId);
    }

    private <T> T getCache(String key, TypeReference<T> typeRef) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception ex) {
            return null;
        }
    }

    private void setCache(String key, Object value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception ignored) {
        }
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
