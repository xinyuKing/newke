package com.shixi.ecommerce.service;

import com.shixi.ecommerce.domain.Product;
import com.shixi.ecommerce.domain.ProductReviewStats;
import com.shixi.ecommerce.domain.ProductStatus;
import com.shixi.ecommerce.dto.ProductResponse;
import com.shixi.ecommerce.repository.ProductRepository;
import com.shixi.ecommerce.repository.ProductReviewStatsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品推荐服务，优先使用评价统计快照。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class ProductRecommendService {
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    private final ProductReviewStatsRepository statsRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductRecommendService(ProductReviewStatsRepository statsRepository,
                                   ProductRepository productRepository,
                                   ProductMapper productMapper) {
        this.statsRepository = statsRepository;
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    /**
     * 推荐商品列表。
     *
     * @param size 返回数量
     * @return 商品列表
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> recommend(Integer size) {
        int limit = normalizeSize(size);
        List<ProductReviewStats> stats = statsRepository.findTop20ByOrderByTotalReviewsDescAvgRatingDesc();
        if (stats.isEmpty()) {
            return productRepository.findByStatus(
                            ProductStatus.ACTIVE,
                            PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                    .getContent()
                    .stream()
                    .map(productMapper::toResponse)
                    .collect(Collectors.toList());
        }
        List<Long> productIds = stats.stream()
                .limit(limit)
                .map(ProductReviewStats::getProductId)
                .collect(Collectors.toList());
        List<Product> products = productRepository.findAllById(productIds);
        Map<Long, Product> productMap = new LinkedHashMap<>();
        for (Product product : products) {
            productMap.put(product.getId(), product);
        }
        List<ProductResponse> result = new ArrayList<>();
        for (Long id : productIds) {
            Product product = productMap.get(id);
            if (product != null && product.getStatus() == ProductStatus.ACTIVE) {
                result.add(productMapper.toResponse(product));
            }
        }
        return result;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
