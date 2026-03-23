package com.shixi.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.domain.Product;
import com.shixi.ecommerce.domain.ProductStatus;
import com.shixi.ecommerce.dto.ProductResponse;
import com.shixi.ecommerce.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ProductIndexPublisher productIndexPublisher;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        productService = new ProductService(
                productRepository,
                inventoryClient,
                redisTemplate,
                new ObjectMapper(),
                new ProductMapper(),
                productIndexPublisher);
    }

    @Test
    void getProductResponsesUsesBatchCacheBeforeDatabase() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ProductResponse second = new ProductResponse(
                2L, 22L, "Mouse", "Gaming mouse", null, new BigDecimal("129.00"), ProductStatus.ACTIVE);
        ProductResponse first = new ProductResponse(
                1L, 11L, "Keyboard", "Mechanical keyboard", null, new BigDecimal("399.00"), ProductStatus.ACTIVE);
        String secondJson = objectMapper.writeValueAsString(second);
        String firstJson = objectMapper.writeValueAsString(first);

        when(valueOperations.multiGet(List.of("ver:product:2", "ver:product:1")))
                .thenReturn(List.of("5", "7"));
        when(valueOperations.multiGet(List.of("cache:product:2:v5", "cache:product:1:v7")))
                .thenReturn(List.of(secondJson, firstJson));

        List<ProductResponse> responses = productService.getProductResponses(List.of(2L, 1L, 2L));

        assertEquals(2, responses.size());
        assertEquals(
                List.of(2L, 1L), responses.stream().map(ProductResponse::getId).toList());
        verify(productRepository, never()).findAllById(any(Iterable.class));
    }

    @Test
    void getProductResponsesBackfillsOnlyMissedProducts() {
        String cachedFirst =
                "{\"id\":1,\"merchantId\":11,\"name\":\"Keyboard\",\"description\":\"Mechanical keyboard\","
                        + "\"videoUrl\":null,\"price\":399.00,\"status\":\"ACTIVE\"}";
        Product missed = new Product();
        missed.setMerchantId(22L);
        missed.setName("Mouse");
        missed.setDescription("Gaming mouse");
        missed.setPrice(new BigDecimal("129.00"));
        missed.setStatus(ProductStatus.ACTIVE);

        when(valueOperations.multiGet(List.of("ver:product:2", "ver:product:1")))
                .thenReturn(Arrays.asList(null, "3"));
        when(valueOperations.multiGet(List.of("cache:product:2:v0", "cache:product:1:v3")))
                .thenReturn(Arrays.asList(null, cachedFirst));
        when(productRepository.findAllById(List.of(2L))).thenReturn(List.of(withId(missed, 2L)));
        when(redisTemplate.executePipelined(any(SessionCallback.class))).thenReturn(List.of());

        List<ProductResponse> responses = productService.getProductResponses(List.of(2L, 1L));

        assertEquals(
                List.of(2L, 1L), responses.stream().map(ProductResponse::getId).toList());
        assertNotNull(responses.get(0).getPrice());
        verify(productRepository).findAllById(List.of(2L));
        verify(redisTemplate).executePipelined(any(SessionCallback.class));
    }

    private Product withId(Product product, Long id) {
        try {
            java.lang.reflect.Field field = Product.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(product, id);
            return product;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
