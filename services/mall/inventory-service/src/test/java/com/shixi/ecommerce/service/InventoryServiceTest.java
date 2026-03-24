package com.shixi.ecommerce.service;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shixi.ecommerce.domain.Inventory;
import com.shixi.ecommerce.repository.InventoryRepository;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private DefaultRedisScript<Long> inventoryDeductScript;

    @Mock
    private DefaultRedisScript<Long> inventoryDeductBatchScript;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        inventoryService = new InventoryService(
                inventoryRepository,
                redisTemplate,
                inventoryDeductScript,
                inventoryDeductBatchScript,
                Duration.ofSeconds(5));
    }

    @Test
    void releaseStockDoesNotCreatePhantomCacheWhenSkuIsMissing() {
        when(inventoryRepository.releaseStock(1001L, 1)).thenReturn(0);

        inventoryService.releaseStock(1001L, 1);

        verify(redisTemplate).delete("stock:sku:1001");
        verify(redisTemplate, never()).hasKey("stock:sku:1001");
        verify(valueOperations, never()).increment(anyString(), anyLong());
    }

    @Test
    void deleteStockRemovesInventoryRecordAndCache() {
        Inventory inventory = new Inventory();
        inventory.setSkuId(1001L);
        when(inventoryRepository.findBySkuId(1001L)).thenReturn(Optional.of(inventory));

        inventoryService.deleteStock(1001L);

        verify(inventoryRepository).delete(inventory);
        verify(redisTemplate).delete("stock:sku:1001");
    }
}
