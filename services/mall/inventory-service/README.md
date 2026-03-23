# 库存服务（inventory-service）

## 功能概述
提供库存初始化、扣减与释放，支持批量接口。库存扣减使用 Redis + Lua 脚本确保并发一致性，批量扣减通过单次 Lua 合并减少 Redis 往返。

## 端口
- `18083`

## 依赖
- MySQL（`ecommerce_inventory`）
- Redis（库存缓存与 Lua 脚本）

## 核心配置
- Redis 连接池：`REDIS_POOL_MAX` `REDIS_POOL_MAX_IDLE` `REDIS_POOL_MIN_IDLE` `REDIS_POOL_MAX_WAIT_MS` `REDIS_TIMEOUT_MS`
- 库存锁 TTL：`INVENTORY_STOCK_LOCK_TTL`
- 数据库连接池：`DB_POOL_MAX` `DB_POOL_MIN` `DB_CONN_TIMEOUT_MS` `DB_IDLE_TIMEOUT_MS` `DB_MAX_LIFETIME_MS`

## 启动
```bash
mvn -q -DskipTests package
```
启动 `InventoryApplication`。

## 规范与说明
- 缓存缺失时使用短锁回源，降低并发打爆数据库风险。
- 批量扣减合并同 SKU，保证“全成或全不成”。
- 注释遵循阿里巴巴 Java 开发手册。
