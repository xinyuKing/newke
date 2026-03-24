package com.shixi.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.Product;
import com.shixi.ecommerce.domain.ProductStatus;
import com.shixi.ecommerce.dto.ReviewCreateRequest;
import com.shixi.ecommerce.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewSummaryPublisher reviewSummaryPublisher;

    @Mock
    private ReviewStatsPublisher reviewStatsPublisher;

    @Mock
    private ProductService productService;

    @Mock
    private OrderClient orderClient;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        reviewService = new ReviewService(
                reviewRepository,
                reviewSummaryPublisher,
                reviewStatsPublisher,
                productService,
                orderClient,
                cacheManager,
                redisTemplate,
                new ObjectMapper());
    }

    @Test
    void addReviewRejectsUserWithoutCompletedPurchase() {
        when(reviewRepository.existsByProductIdAndUserId(1001L, 42L)).thenReturn(false);
        when(productService.getProductOrThrow(1001L)).thenReturn(product());
        when(orderClient.hasCompletedPurchase(42L, 1001L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> reviewService.addReview(42L, request()));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void addReviewPublishesAsyncRefreshForEligiblePurchase() {
        when(reviewRepository.existsByProductIdAndUserId(1001L, 42L)).thenReturn(false);
        when(productService.getProductOrThrow(1001L)).thenReturn(product());
        when(orderClient.hasCompletedPurchase(42L, 1001L)).thenReturn(true);
        when(reviewRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        reviewService.addReview(42L, request());

        verify(reviewSummaryPublisher).publishAfterCommit(1001L);
        verify(reviewStatsPublisher).publishAfterCommit(1001L);
    }

    private Product product() {
        Product product = new Product();
        product.setMerchantId(7L);
        product.setName("Keyboard");
        product.setDescription("Mechanical keyboard");
        product.setPrice(java.math.BigDecimal.TEN);
        product.setStatus(ProductStatus.ACTIVE);
        return product;
    }

    private ReviewCreateRequest request() {
        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setProductId(1001L);
        request.setRating(5);
        request.setContent("Solid daily driver.");
        return request;
    }
}
