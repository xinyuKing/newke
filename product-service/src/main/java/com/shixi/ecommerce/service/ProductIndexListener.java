package com.shixi.ecommerce.service;

import com.shixi.ecommerce.config.ProductIndexQueueConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * 商品索引消息监听器。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class ProductIndexListener {
    private static final Logger log = LoggerFactory.getLogger(ProductIndexListener.class);

    private final ProductIndexService productIndexService;

    public ProductIndexListener(ProductIndexService productIndexService) {
        this.productIndexService = productIndexService;
    }

    @RabbitListener(queues = ProductIndexQueueConfig.PRODUCT_INDEX_QUEUE)
    public void onMessage(String productIdText) {
        if (productIdText == null || productIdText.isBlank()) {
            return;
        }
        try {
            Long productId = Long.valueOf(productIdText);
            productIndexService.indexProduct(productId);
        } catch (Exception ex) {
            log.warn("Handle product index message failed: {}", productIdText, ex);
        }
    }
}
