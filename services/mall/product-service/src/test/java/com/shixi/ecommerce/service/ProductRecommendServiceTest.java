package com.shixi.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shixi.ecommerce.domain.Product;
import com.shixi.ecommerce.domain.ProductReviewStats;
import com.shixi.ecommerce.domain.ProductStatus;
import com.shixi.ecommerce.dto.ProductResponse;
import com.shixi.ecommerce.repository.ProductRepository;
import com.shixi.ecommerce.repository.ProductReviewStatsRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProductRecommendServiceTest {

    @Mock
    private ProductReviewStatsRepository statsRepository;

    @Mock
    private ProductRepository productRepository;

    private ProductRecommendService productRecommendService;

    @BeforeEach
    void setUp() {
        productRecommendService = new ProductRecommendService(statsRepository, productRepository, new ProductMapper());
    }

    @Test
    void recommendBackfillsInactiveHotProductsWithLatestActiveProducts() {
        when(statsRepository.findAllByOrderByTotalReviewsDescAvgRatingDesc(any(Pageable.class)))
                .thenReturn(List.of(stats(1L), stats(2L), stats(3L)));
        when(productRepository.findAllById(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(
                        product(1L, ProductStatus.ACTIVE, LocalDateTime.of(2026, 3, 24, 10, 0)),
                        product(2L, ProductStatus.INACTIVE, LocalDateTime.of(2026, 3, 24, 9, 0)),
                        product(3L, ProductStatus.ACTIVE, LocalDateTime.of(2026, 3, 24, 8, 0))));
        when(productRepository.findByStatus(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        product(3L, ProductStatus.ACTIVE, LocalDateTime.of(2026, 3, 24, 8, 0)),
                        product(4L, ProductStatus.ACTIVE, LocalDateTime.of(2026, 3, 24, 7, 0)),
                        product(1L, ProductStatus.ACTIVE, LocalDateTime.of(2026, 3, 24, 6, 0)))));

        List<ProductResponse> responses = productRecommendService.recommend(3);

        assertEquals(
                List.of(1L, 3L, 4L),
                responses.stream().map(ProductResponse::getId).toList());
    }

    @Test
    void recommendSupportsRequestSizesAboveTwenty() {
        List<ProductReviewStats> stats =
                LongStream.rangeClosed(1, 25).mapToObj(this::stats).toList();
        List<Product> products = LongStream.rangeClosed(1, 25)
                .mapToObj(id -> product(
                        id,
                        ProductStatus.ACTIVE,
                        LocalDateTime.of(2026, 3, 24, 10, 0).minusMinutes(id)))
                .toList();
        when(statsRepository.findAllByOrderByTotalReviewsDescAvgRatingDesc(any(Pageable.class)))
                .thenReturn(stats);
        when(productRepository.findAllById(
                        stats.stream().map(ProductReviewStats::getProductId).toList()))
                .thenReturn(products);

        List<ProductResponse> responses = productRecommendService.recommend(25);

        assertEquals(25, responses.size());
        verify(productRepository, never()).findByStatus(any(), any(Pageable.class));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(statsRepository).findAllByOrderByTotalReviewsDescAvgRatingDesc(pageableCaptor.capture());
        assertEquals(75, pageableCaptor.getValue().getPageSize());
    }

    private ProductReviewStats stats(Long productId) {
        ProductReviewStats stats = new ProductReviewStats();
        stats.setProductId(productId);
        stats.setTotalReviews(10L + productId);
        stats.setAvgRating(4.5);
        stats.setNegativeReviews(0L);
        return stats;
    }

    private Product product(Long id, ProductStatus status, LocalDateTime createdAt) {
        Product product = new Product();
        product.setMerchantId(7L);
        product.setName("Product " + id);
        product.setDescription("Description " + id);
        product.setPrice(new BigDecimal("99.00"));
        product.setStatus(status);
        setField(product, "id", id);
        setField(product, "createdAt", createdAt);
        return product;
    }

    private void setField(Product product, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = Product.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(product, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
