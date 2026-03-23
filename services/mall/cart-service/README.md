# 购物车服务（cart-service）

## 功能概述
提供购物车增删改查与结算能力，结算时批量组装订单行并调用订单服务创建订单。

## 端口
- `18085`

## 依赖
- MySQL（`ecommerce_cart`）
- Redis（购物车列表缓存、限流）
- 商品服务（`product-service`）
- 订单服务（`order-service`）

## 核心配置
- 商品服务地址：`product.service.url`
- 订单服务地址：`order.service.url`
- Redis：`REDIS_TIMEOUT_MS` `REDIS_POOL_MAX` `REDIS_POOL_MAX_IDLE` `REDIS_POOL_MIN_IDLE`
- 数据库连接池：`DB_POOL_MAX` `DB_POOL_MIN` `DB_CONN_TIMEOUT_MS` `DB_IDLE_TIMEOUT_MS` `DB_MAX_LIFETIME_MS`
- 购物车限流：`CART_RATE_WINDOW` `CART_RATE_USER_MAX` `CART_RATE_IP_MAX`

## 启动
```bash
mvn -q -DskipTests package
```
启动 `CartApplication`。

## 规范与说明
- 购物车写入保存商品价格快照，用于展示与结算参考。
- 购物车写接口单独限流，避免被订单高峰影响。
- 结算走内部下单接口，减少业务分散。
- 注释遵循阿里巴巴 Java 开发手册。
