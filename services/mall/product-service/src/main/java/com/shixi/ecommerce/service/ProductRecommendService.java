package com.shixi.ecommerce.service;

import com.shixi.ecommerce.domain.Product;
import com.shixi.ecommerce.domain.ProductReviewStats;
import com.shixi.ecommerce.domain.ProductStatus;
import com.shixi.ecommerce.dto.ProductResponse;
import com.shixi.ecommerce.repository.ProductRepository;
import com.shixi.ecommerce.repository.ProductReviewStatsRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private static final int MIN_STATS_CANDIDATES = 20;
    private static final int STATS_CANDIDATE_MULTIPLIER = 3;
    private static final int FALLBACK_PAGE_SIZE = 20;

    private final ProductReviewStatsRepository statsRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductRecommendService(
            ProductReviewStatsRepository statsRepository,
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
        List<ProductReviewStats> stats = statsRepository.findAllByOrderByTotalReviewsDescAvgRatingDesc(
                PageRequest.of(0, Math.max(MIN_STATS_CANDIDATES, limit * STATS_CANDIDATE_MULTIPLIER)));
        List<Long> productIds =
                stats.stream().map(ProductReviewStats::getProductId).collect(Collectors.toList());
        List<Product> products = productIds.isEmpty() ? List.of() : productRepository.findAllById(productIds);
        Map<Long, Product> productMap = new LinkedHashMap<>();
        for (Product product : products) {
            productMap.put(product.getId(), product);
        }
        List<ProductResponse> result = new ArrayList<>();
        Set<Long> selectedIds = new LinkedHashSet<>();
        for (Long id : productIds) {
            Product product = productMap.get(id);
            if (product != null && product.getStatus() == ProductStatus.ACTIVE) {
                if (!selectedIds.add(product.getId())) {
                    continue;
                }
                result.add(productMapper.toResponse(product));
                if (result.size() == limit) {
                    return result;
                }
            }
        }
        backfillLatestActiveProducts(result, selectedIds, limit);
        return result;
    }

    private void backfillLatestActiveProducts(List<ProductResponse> result, Set<Long> selectedIds, int limit) {
        int page = 0;
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        while (result.size() < limit) {
            Page<Product> activePage = productRepository.findByStatus(
                    ProductStatus.ACTIVE, PageRequest.of(page, FALLBACK_PAGE_SIZE, sort));
            if (activePage.isEmpty()) {
                return;
            }
            for (Product product : activePage.getContent()) {
                if (!selectedIds.add(product.getId())) {
                    continue;
                }
                result.add(productMapper.toResponse(product));
                if (result.size() == limit) {
                    return;
                }
            }
            if (!activePage.hasNext()) {
                return;
            }
            page++;
        }
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
