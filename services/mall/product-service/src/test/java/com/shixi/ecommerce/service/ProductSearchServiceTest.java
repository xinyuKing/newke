package com.shixi.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shixi.ecommerce.config.SearchProperties;
import com.shixi.ecommerce.domain.Product;
import com.shixi.ecommerce.domain.ProductStatus;
import com.shixi.ecommerce.dto.CursorPageResponse;
import com.shixi.ecommerce.dto.ProductResponse;
import com.shixi.ecommerce.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RestTemplate restTemplate;

    private SearchProperties searchProperties;
    private ObjectMapper objectMapper;
    private ProductSearchService productSearchService;

    @BeforeEach
    void setUp() {
        searchProperties = new SearchProperties();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        productSearchService = new ProductSearchService(
                productRepository, new ProductMapper(), searchProperties, redisTemplate, restTemplate, objectMapper);
    }

    @Test
    void searchUsesCachedResultBeforeQueryingBackend() throws Exception {
        CursorPageResponse<ProductResponse> cached = new CursorPageResponse<>(
                List.of(new ProductResponse(
                        1L,
                        9L,
                        "Keyboard",
                        "Low latency keyboard",
                        null,
                        new BigDecimal("299.00"),
                        ProductStatus.ACTIVE)),
                false,
                LocalDateTime.of(2026, 3, 23, 12, 0),
                1L);
        when(valueOperations.get("ver:search:products")).thenReturn("3");
        when(valueOperations.get("cache:search:products:db:v3:a2V5Ym9hcmQ:none:none:20"))
                .thenReturn(objectMapper.writeValueAsString(cached));

        CursorPageResponse<ProductResponse> response =
                productSearchService.searchActiveProducts("keyboard", null, null, 20);

        assertEquals(1, response.getItems().size());
        verify(productRepository, never()).searchByStatusCursor(any(), any(), any(), any(), any(Pageable.class));
        verify(restTemplate, never()).exchange(any(String.class), any(HttpMethod.class), any(), eq(String.class));
    }

    @Test
    void searchFallsBackToDbWhenAutoProviderHasNoSearchUrl() {
        Product product = product(1L, "Keyboard");
        when(valueOperations.get("ver:search:products")).thenReturn(null);
        when(valueOperations.get("cache:search:products:db:v0:a2V5Ym9hcmQ:none:none:20"))
                .thenReturn(null);
        when(productRepository.searchByStatusCursor(
                        eq(ProductStatus.ACTIVE), eq("keyboard"), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(List.of(product));

        CursorPageResponse<ProductResponse> response =
                productSearchService.searchActiveProducts(" keyboard ", null, null, 20);

        assertEquals(
                List.of(1L),
                response.getItems().stream().map(ProductResponse::getId).toList());
        verify(productRepository)
                .searchByStatusCursor(
                        eq(ProductStatus.ACTIVE), eq("keyboard"), eq(null), eq(null), any(Pageable.class));
        verify(restTemplate, never()).exchange(any(String.class), any(HttpMethod.class), any(), eq(String.class));
        verify(valueOperations)
                .set(
                        eq("cache:search:products:db:v0:a2V5Ym9hcmQ:none:none:20"),
                        any(String.class),
                        eq(Duration.ofSeconds(60)));
    }

    @Test
    void searchFallsBackToDbWhenOpenSearchFails() {
        searchProperties.getOpenSearch().setUrl("http://localhost:9200");
        Product product = product(2L, "Mouse");
        when(valueOperations.get("ver:search:products")).thenReturn("1");
        when(valueOperations.get("cache:search:products:opensearch:v1:bW91c2U:none:none:20"))
                .thenReturn(null);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenThrow(new RuntimeException("search down"));
        when(productRepository.searchByStatusCursor(
                        eq(ProductStatus.ACTIVE), eq("mouse"), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(List.of(product));

        CursorPageResponse<ProductResponse> response =
                productSearchService.searchActiveProducts("mouse", null, null, 20);

        assertEquals(
                List.of(2L),
                response.getItems().stream().map(ProductResponse::getId).toList());
        verify(productRepository)
                .searchByStatusCursor(eq(ProductStatus.ACTIVE), eq("mouse"), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void blankKeywordUsesCursorListingInsteadOfLikeSearch() {
        Product product = product(3L, "Camera");
        when(valueOperations.get("ver:search:products")).thenReturn(null);
        when(valueOperations.get("cache:search:products:db:v0:all:none:none:20"))
                .thenReturn(null);
        when(productRepository.findByStatusCursor(eq(ProductStatus.ACTIVE), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(List.of(product));

        CursorPageResponse<ProductResponse> response = productSearchService.searchActiveProducts("   ", null, null, 20);

        assertEquals(
                List.of(3L),
                response.getItems().stream().map(ProductResponse::getId).toList());
        verify(productRepository).findByStatusCursor(eq(ProductStatus.ACTIVE), eq(null), eq(null), any(Pageable.class));
        verify(productRepository, never()).searchByStatusCursor(any(), any(), any(), any(), any(Pageable.class));
    }

    private Product product(Long id, String name) {
        Product product = new Product();
        product.setMerchantId(8L);
        product.setName(name);
        product.setDescription(name + " desc");
        product.setPrice(new BigDecimal("88.00"));
        product.setStatus(ProductStatus.ACTIVE);
        setField(product, "id", id);
        setField(product, "createdAt", LocalDateTime.of(2026, 3, 23, 10, 0));
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
