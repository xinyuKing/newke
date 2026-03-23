package com.shixi.ecommerce.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.Product;
import com.shixi.ecommerce.domain.Review;
import com.shixi.ecommerce.dto.CursorPageResponse;
import com.shixi.ecommerce.dto.ReviewCreateRequest;
import com.shixi.ecommerce.dto.ReviewResponse;
import com.shixi.ecommerce.dto.ReviewSliceResponse;
import com.shixi.ecommerce.repository.ReviewRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 评价领域服务，负责新增评价与分页查询。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class ReviewService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Duration REVIEW_LIST_CACHE_TTL = Duration.ofMinutes(2);
    private static final String REVIEW_VERSION_PREFIX = "ver:review:";
    private static final String REVIEW_LIST_CACHE_PREFIX = "cache:review:list:";

    private final ReviewRepository reviewRepository;
    private final ReviewSummaryPublisher reviewSummaryPublisher;
    private final ReviewStatsPublisher reviewStatsPublisher;
    private final ProductService productService;
    private final CacheManager cacheManager;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ReviewService(
            ReviewRepository reviewRepository,
            ReviewSummaryPublisher reviewSummaryPublisher,
            ReviewStatsPublisher reviewStatsPublisher,
            ProductService productService,
            CacheManager cacheManager,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.reviewRepository = reviewRepository;
        this.reviewSummaryPublisher = reviewSummaryPublisher;
        this.reviewStatsPublisher = reviewStatsPublisher;
        this.productService = productService;
        this.cacheManager = cacheManager;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 新增评价并清理相关缓存，摘要/统计通过队列异步执行。
     *
     * @param userId  用户 ID
     * @param request 评价请求
     */
    @Transactional
    @CacheEvict(cacheNames = "reviewSummary", key = "#request.productId")
    public void addReview(Long userId, ReviewCreateRequest request) {
        if (userId == null) {
            throw new BusinessException("UserId required");
        }
        if (reviewRepository.existsByProductIdAndUserId(request.getProductId(), userId)) {
            throw new BusinessException("Review already exists");
        }
        Product product = productService.getProductOrThrow(request.getProductId());
        Review review = new Review();
        review.setProductId(request.getProductId());
        review.setUserId(userId);
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        try {
            reviewRepository.save(review);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Review already exists");
        }
        evictMerchantAnalysis(product.getMerchantId());
        bumpReviewVersion(request.getProductId());
        reviewSummaryPublisher.publishAfterCommit(request.getProductId());
        reviewStatsPublisher.publishAfterCommit(request.getProductId());
    }

    /**
     * 分页查询商品评价（Slice 方式，避免额外 count）。
     *
     * @param productId 商品 ID
     * @param rating    评分过滤
     * @param page      页码
     * @param size      每页大小
     * @return 评价切片
     */
    @Transactional(readOnly = true)
    public ReviewSliceResponse listReviews(Long productId, Integer rating, Integer page, Integer size) {
        if (productId == null) {
            throw new BusinessException("ProductId required");
        }
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        Slice<Review> slice;
        if (rating == null) {
            slice = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, PageRequest.of(pageNo, pageSize));
        } else {
            slice = reviewRepository.findByProductIdAndRatingOrderByCreatedAtDesc(
                    productId, rating, PageRequest.of(pageNo, pageSize));
        }
        List<ReviewResponse> items =
                slice.getContent().stream().map(this::toResponse).collect(Collectors.toList());
        return new ReviewSliceResponse(items, slice.hasNext());
    }

    /**
     * 游标分页查询评价（带缓存）。
     *
     * @param productId  商品 ID
     * @param rating     评分过滤
     * @param cursorTime 游标时间
     * @param cursorId   游标 ID
     * @param size       每页大小
     * @return 游标分页结果
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<ReviewResponse> listReviewsCursor(
            Long productId, Integer rating, LocalDateTime cursorTime, Long cursorId, Integer size) {
        if (productId == null) {
            throw new BusinessException("ProductId required");
        }
        int pageSize = normalizeSize(size);
        String version = getVersion(reviewVersionKey(productId));
        String cursorTimeKey = cursorTime == null ? "none" : cursorTime.toString();
        String cursorIdKey = cursorId == null ? "none" : String.valueOf(cursorId);
        String ratingKey = rating == null ? "all" : String.valueOf(rating);
        String cacheKey = REVIEW_LIST_CACHE_PREFIX + productId + ":v" + version + ":r" + ratingKey + ":" + cursorTimeKey
                + ":" + cursorIdKey + ":" + pageSize;
        CursorPageResponse<ReviewResponse> cached =
                getCache(cacheKey, new TypeReference<CursorPageResponse<ReviewResponse>>() {});
        if (cached != null) {
            return cached;
        }
        List<Review> reviews = reviewRepository.findByProductIdCursor(
                productId,
                rating,
                cursorTime,
                cursorId,
                PageRequest.of(
                        0,
                        pageSize + 1,
                        Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));
        boolean hasNext = reviews.size() > pageSize;
        if (hasNext) {
            reviews = reviews.subList(0, pageSize);
        }
        LocalDateTime nextTime = null;
        Long nextId = null;
        if (!reviews.isEmpty()) {
            Review last = reviews.get(reviews.size() - 1);
            nextTime = last.getCreatedAt();
            nextId = last.getId();
        }
        List<ReviewResponse> items = reviews.stream().map(this::toResponse).collect(Collectors.toList());
        CursorPageResponse<ReviewResponse> response = new CursorPageResponse<>(items, hasNext, nextTime, nextId);
        setCache(cacheKey, response, REVIEW_LIST_CACHE_TTL);
        return response;
    }

    private void evictMerchantAnalysis(Long merchantId) {
        if (merchantId == null) {
            return;
        }
        if (cacheManager.getCache("merchantAnalysis") != null) {
            cacheManager.getCache("merchantAnalysis").evict(merchantId);
        }
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getProductId(),
                review.getUserId(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt());
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String reviewVersionKey(Long productId) {
        return REVIEW_VERSION_PREFIX + productId;
    }

    private void bumpReviewVersion(Long productId) {
        if (productId == null) {
            return;
        }
        redisTemplate.opsForValue().increment(reviewVersionKey(productId));
    }

    private String getVersion(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return "0";
        }
        return value;
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
}
