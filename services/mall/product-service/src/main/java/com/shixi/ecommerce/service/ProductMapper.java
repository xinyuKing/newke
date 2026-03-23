package com.shixi.ecommerce.service;

import com.shixi.ecommerce.domain.Product;
import com.shixi.ecommerce.dto.ProductResponse;
import org.springframework.stereotype.Component;

/**
 * 商品实体与响应对象转换。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Component
public class ProductMapper {
    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductResponse(
                product.getId(),
                product.getMerchantId(),
                product.getName(),
                product.getDescription(),
                product.getVideoUrl(),
                product.getPrice(),
                product.getStatus());
    }
}
