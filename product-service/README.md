# 商品服务（product-service）

## 功能概述
负责商品管理、评价、评价摘要与统计快照生成。支持批量商品查询、游标分页与缓存，评价摘要/统计异步队列化以降低写入路径延迟。
商品支持短视频展示（`videoUrl`），并提供搜索与推荐接口（支持 OpenSearch/DB 两种模式）。

## 端口
- `18082`

## 依赖
- MySQL（`ecommerce_product`）
- RabbitMQ（评价摘要/统计/索引异步）
- Redis（评价列表缓存、限流）
- 库存服务（`inventory-service`）
- 可选：OpenSearch/Elasticsearch（搜索与索引）

## 核心配置
- 库存服务地址：`inventory.service.url`
- RabbitMQ：`RABBIT_HOST` `RABBIT_PORT` `RABBIT_USER` `RABBIT_PASS`
- Redis：`REDIS_TIMEOUT_MS` `REDIS_POOL_MAX` `REDIS_POOL_MAX_IDLE` `REDIS_POOL_MIN_IDLE`
- 数据库连接池：`DB_POOL_MAX` `DB_POOL_MIN` `DB_CONN_TIMEOUT_MS` `DB_IDLE_TIMEOUT_MS` `DB_MAX_LIFETIME_MS`
- 读库（可选）：`DB_READ_URL` `DB_READ_USER` `DB_READ_PASS`
- 搜索：`SEARCH_PROVIDER=db|opensearch`、`OPENSEARCH_URL`、`OPENSEARCH_INDEX`
- 评价限流：`REVIEW_RATE_WINDOW` `REVIEW_RATE_USER_MAX` `REVIEW_RATE_IP_MAX`

## 启动
```bash
mvn -q -DskipTests package
```
启动 `ProductApplication`。

## 规范与说明
- 评价摘要与统计通过消息队列异步更新，降低写入路径延迟。
- 商品搜索支持 OpenSearch（优先）与 DB（降级）双通道。
- 商品批量接口用于降低跨服务调用次数。
- 注释遵循阿里巴巴 Java 开发手册。
