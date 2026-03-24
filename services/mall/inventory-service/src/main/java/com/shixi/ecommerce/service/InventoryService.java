package com.shixi.ecommerce.service;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.Inventory;
import com.shixi.ecommerce.dto.InventoryDeductRequest;
import com.shixi.ecommerce.repository.InventoryRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存核心服务，支持库存初始化、扣减、释放与批量操作。
 * 使用 Redis + Lua 脚本保证高并发下的库存安全，并在缓存缺失时回源数据库。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class InventoryService {
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private static final String STOCK_KEY_PREFIX = "stock:sku:";
    private static final String STOCK_LOCK_PREFIX = "lock:stock:sku:";

    private final InventoryRepository inventoryRepository;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> inventoryDeductScript;
    private final DefaultRedisScript<Long> inventoryDeductBatchScript;
    private final Duration stockLockTtl;

    public InventoryService(
            InventoryRepository inventoryRepository,
            StringRedisTemplate redisTemplate,
            @Qualifier("inventoryDeductScript") DefaultRedisScript<Long> inventoryDeductScript,
            @Qualifier("inventoryDeductBatchScript") DefaultRedisScript<Long> inventoryDeductBatchScript,
            @Value("${inventory.stock.lock-ttl:5s}") Duration stockLockTtl) {
        this.inventoryRepository = inventoryRepository;
        this.redisTemplate = redisTemplate;
        this.inventoryDeductScript = inventoryDeductScript;
        this.inventoryDeductBatchScript = inventoryDeductBatchScript;
        this.stockLockTtl = stockLockTtl;
    }

    /**
     * 初始化库存并同步写入缓存。
     *
     * @param skuId 商品 ID
     * @param stock 初始库存
     */
    @Transactional
    public void initStock(Long skuId, Integer stock) {
        Inventory inventory = inventoryRepository.findBySkuId(skuId).orElseGet(Inventory::new);
        inventory.setSkuId(skuId);
        inventory.setAvailableStock(stock);
        inventoryRepository.save(inventory);
        redisTemplate.opsForValue().set(stockKey(skuId), String.valueOf(stock));
    }

    @Transactional
    public void deleteStock(Long skuId) {
        inventoryRepository.findBySkuId(skuId).ifPresent(inventoryRepository::delete);
        redisTemplate.delete(stockKey(skuId));
    }

    /**
     * 扣减库存：优先使用 Redis/Lua 原子扣减，必要时回源 DB 刷新缓存。
     *
     * @param skuId 商品 ID
     * @param qty   扣减数量
     * @return 是否扣减成功
     */
    @Transactional
    public boolean deductStock(Long skuId, Integer qty) {
        String key = stockKey(skuId);
        Long result = executeDeduct(key, qty);
        if (result == null || result == -1) {
            boolean refreshed = refreshCacheFromDb(skuId);
            if (refreshed) {
                result = executeDeduct(key, qty);
            } else {
                String cached = redisTemplate.opsForValue().get(key);
                if (cached != null) {
                    result = executeDeduct(key, qty);
                }
            }
        }
        if (result == null || result <= 0) {
            return false;
        }
        int updated = inventoryRepository.deductStock(skuId, qty);
        if (updated == 0) {
            redisTemplate.opsForValue().increment(key, qty);
            return false;
        }
        return true;
    }

    /**
     * 释放库存并回写缓存。
     *
     * @param skuId 商品 ID
     * @param qty   释放数量
     */
    @Transactional
    public void releaseStock(Long skuId, Integer qty) {
        String key = stockKey(skuId);
        int updated = inventoryRepository.releaseStock(skuId, qty);
        if (updated == 0) {
            log.warn("Skip inventory cache increment for missing sku {}", skuId);
            redisTemplate.delete(key);
            return;
        }
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            refreshCacheFromDb(skuId);
            return;
        }
        redisTemplate.opsForValue().increment(key, qty);
    }

    /**
     * 批量扣减库存，合并相同 SKU 并通过批量 Lua 脚本减少 Redis 往返。
     *
     * @param items 扣减条目
     * @return 是否扣减成功
     */
    @Transactional
    public boolean deductBatch(List<InventoryDeductRequest> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }
        Map<Long, Integer> merged = mergeItems(items);
        if (merged.isEmpty()) {
            return false;
        }
        List<String> keys = new ArrayList<>(merged.size());
        List<String> args = new ArrayList<>(merged.size());
        for (Map.Entry<Long, Integer> entry : merged.entrySet()) {
            keys.add(stockKey(entry.getKey()));
            args.add(String.valueOf(entry.getValue()));
        }
        Long result = executeBatchDeduct(keys, args);
        if (result == null || result == -1) {
            boolean refreshed = refreshCacheFromDbBatch(merged.keySet());
            if (refreshed) {
                result = executeBatchDeduct(keys, args);
            }
        }
        if (result == null || result <= 0) {
            return false;
        }
        List<Map.Entry<Long, Integer>> deducted = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : merged.entrySet()) {
            int updated = inventoryRepository.deductStock(entry.getKey(), entry.getValue());
            if (updated == 0) {
                rollbackRedis(merged);
                for (Map.Entry<Long, Integer> done : deducted) {
                    inventoryRepository.releaseStock(done.getKey(), done.getValue());
                }
                return false;
            }
            deducted.add(entry);
        }
        return true;
    }

    /**
     * 批量释放库存，合并相同 SKU。
     *
     * @param items 释放条目
     */
    @Transactional
    public void releaseBatch(List<InventoryDeductRequest> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<Long, Integer> merged = mergeItems(items);
        for (Map.Entry<Long, Integer> entry : merged.entrySet()) {
            releaseStock(entry.getKey(), entry.getValue());
        }
    }

    private Map<Long, Integer> mergeItems(List<InventoryDeductRequest> items) {
        Map<Long, Integer> merged = new LinkedHashMap<>();
        for (InventoryDeductRequest item : items) {
            if (item == null || item.getSkuId() == null || item.getQuantity() == null) {
                continue;
            }
            merged.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
        }
        return merged;
    }

    private void rollbackRedis(Map<Long, Integer> merged) {
        for (Map.Entry<Long, Integer> entry : merged.entrySet()) {
            redisTemplate.opsForValue().increment(stockKey(entry.getKey()), entry.getValue());
        }
    }

    /**
     * 执行 Redis/Lua 单条扣减脚本。
     *
     * @param key 库存缓存键
     * @param qty 扣减数量
     * @return 扣减结果
     */
    private Long executeDeduct(String key, Integer qty) {
        return redisTemplate.execute(inventoryDeductScript, Collections.singletonList(key), String.valueOf(qty));
    }

    /**
     * 执行 Redis/Lua 批量扣减脚本。
     *
     * @param keys Redis Key 列表
     * @param args 扣减数量列表
     * @return 扣减结果
     */
    private Long executeBatchDeduct(List<String> keys, List<String> args) {
        return redisTemplate.execute(inventoryDeductBatchScript, keys, args.toArray());
    }

    /**
     * 缓存回源：使用短锁避免并发回源导致的 DB 冲击。
     *
     * @param skuId 商品 ID
     * @return 是否成功刷新
     */
    private boolean refreshCacheFromDb(Long skuId) {
        String lockKey = stockLockKey(skuId);
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", stockLockTtl);
        if (!Boolean.TRUE.equals(locked)) {
            return false;
        }
        try {
            Optional<Inventory> inventoryOpt = inventoryRepository.findBySkuId(skuId);
            Inventory inventory = inventoryOpt.orElseThrow(() -> new BusinessException("SKU not found: " + skuId));
            redisTemplate.opsForValue().set(stockKey(skuId), String.valueOf(inventory.getAvailableStock()));
            return true;
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private boolean refreshCacheFromDbBatch(Set<Long> skuIds) {
        boolean refreshed = false;
        for (Long skuId : skuIds) {
            if (refreshCacheFromDb(skuId)) {
                refreshed = true;
            }
        }
        return refreshed;
    }

    private String stockKey(Long skuId) {
        return STOCK_KEY_PREFIX + skuId;
    }

    private String stockLockKey(Long skuId) {
        return STOCK_LOCK_PREFIX + skuId;
    }
}
