package com.shixi.ecommerce.web;

import com.shixi.ecommerce.dto.ProductBatchRequest;
import com.shixi.ecommerce.dto.ProductResponse;
import com.shixi.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品内部接口，供购物车、订单等服务调用。
 *
 * @author shixi
 * @date 2026-03-20
 */
@RestController
@RequestMapping("/internal/products")
public class ProductInternalController {
    private final ProductService productService;

    public ProductInternalController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 获取单个商品信息。
     *
     * @param id 商品 ID
     * @return 商品信息
     */
    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productService.getProductResponse(id);
    }

    /**
     * 批量获取商品信息。
     *
     * @param request 批量请求
     * @return 商品信息列表
     */
    @PostMapping("/batch")
    public List<ProductResponse> getByIds(@Valid @RequestBody ProductBatchRequest request) {
        return productService.getProductResponses(request.getSkuIds());
    }
}
