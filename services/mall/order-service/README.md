# 订单服务（order-service）

## 功能概述
负责下单、支付、取消、发货与收货，并投递订单事件。下单链路进行服务端定价、批量库存扣减与批量明细写入。
支持发货与物流查询（运单号、轨迹），订单列表游标分页与详情缓存。

## 端口
- `18084`

## 依赖
- MySQL（`ecommerce_order`）
- Redis（限流、缓存）
- RabbitMQ（订单事件）
- 商品服务（`product-service`）
- 库存服务（`inventory-service`）

## 核心配置
- 商品服务地址：`product.service.url`
- 库存服务地址：`inventory.service.url`
- 物流查询：`LOGISTICS_PROVIDER` `LOGISTICS_API_URL` `LOGISTICS_API_KEY`
- 订单限流：`ORDER_RATE_WINDOW` `ORDER_RATE_USER_MAX` `ORDER_RATE_IP_MAX` `ORDER_RATE_ALGO`
- Redis 连接池：`REDIS_POOL_MAX` `REDIS_POOL_MAX_IDLE` `REDIS_POOL_MIN_IDLE` `REDIS_POOL_MAX_WAIT_MS` `REDIS_TIMEOUT_MS`
- 数据库连接池：`DB_POOL_MAX` `DB_POOL_MIN` `DB_CONN_TIMEOUT_MS` `DB_IDLE_TIMEOUT_MS` `DB_MAX_LIFETIME_MS`
- 读库（可选）：`DB_READ_URL` `DB_READ_USER` `DB_READ_PASS`

## 启动
```bash
mvn -q -DskipTests package
```
启动 `OrderApplication`。

## 规范与说明
- 状态流转由状态机统一校验，避免分散逻辑。
- 支付/发货/收货事件化，降低同步耦合。
- 服务端定价避免客户端篡改价格。
- 批量扣减与批量写入降低跨服务与数据库压力。
- 注释遵循阿里巴巴 Java 开发手册。
