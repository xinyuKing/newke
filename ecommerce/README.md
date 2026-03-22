# 实习电商微服务（shixi）

## 项目简介
基于 Spring Boot 的电商微服务示例，覆盖登录鉴权、商品、库存、订单、购物车与客服支持等核心链路。
整体以性能优先为目标，强调跨服务调用的批量化、缓存化与异步化，同时引入熔断限流与指标监控。

## 模块与端口
- `common`：公共 DTO、异常与安全组件
- `auth-service`（`18081`）：用户认证与权限（对接社区用户系统）
- `product-service`（`18082`）：商品与评价
- `inventory-service`（`18083`）：库存
- `order-service`（`18084`）：订单与下单链路
- `cart-service`（`18085`）：购物车与结算
- `support-service`（`18086`）：客服与退款支持
- `gateway-service`（`8080`）：统一网关（路由/鉴权/限流/熔断/灰度）

## 技术栈
- Java 17 / Spring Boot 3.3.2
- Spring Data JPA + MySQL
- Redis（含 Lua 脚本）
- RabbitMQ（评价摘要/统计/索引异步化）
- Caffeine 本地缓存
- Resilience4j 熔断/重试/舱壁
- Micrometer + Prometheus 指标
- 可选：OpenSearch/Elasticsearch（商品搜索）

## 关键设计（性能优先）
- 下单价格以服务端为准，防止客户端篡改；商品/库存均支持批量接口减少跨服务调用。
- 库存扣减使用 Redis + Lua 脚本配合数据库落库，缓存击穿时采用短锁回源。
- 评价摘要/统计异步队列化，降低写入路径延迟。
- 订单限流支持固定窗口/滑动窗口策略。
- 商品搜索支持 OpenSearch（优先）与 DB（降级）。
- 订单与商品支持读写分离配置（读库可选）。
- 核心服务启用本地缓存与熔断隔离，降低抖动影响。

## 运行前置
本地需准备以下基础设施：
- MySQL（各服务独立库）
- Redis
- RabbitMQ（`product-service` 使用）

## 快速启动
1. 构建：
   ```bash
   mvn -q -DskipTests package
   ```
2. 分别启动各服务的 Spring Boot 应用（端口见上）。

## 常用环境变量
- 数据库连接池（Hikari）：
  - `DB_POOL_MAX` `DB_POOL_MIN`
  - `DB_CONN_TIMEOUT_MS` `DB_IDLE_TIMEOUT_MS` `DB_MAX_LIFETIME_MS`
- Redis 连接池：
  - `REDIS_POOL_MAX` `REDIS_POOL_MAX_IDLE` `REDIS_POOL_MIN_IDLE`
  - `REDIS_POOL_MAX_WAIT_MS` `REDIS_TIMEOUT_MS`
- 库存锁 TTL：`INVENTORY_STOCK_LOCK_TTL`
- 订单限流：`ORDER_RATE_WINDOW` `ORDER_RATE_USER_MAX` `ORDER_RATE_IP_MAX` `ORDER_RATE_ALGO`
- RabbitMQ：`RABBIT_HOST` `RABBIT_PORT` `RABBIT_USER` `RABBIT_PASS`

## 编码规范与注释
项目代码遵循阿里巴巴 Java 开发手册的注释规范：
- 对外可见的类与方法使用 Javadoc 注释。
- 注释表达意图与约束，避免重复“显而易见”的信息。
- 复杂逻辑使用行内注释说明设计权衡或边界行为。
