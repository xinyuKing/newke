# 本地部署说明

## 目标

当前本地部署方案只负责把基础设施拉起，并约束统一项目的部署边界：

1. 浏览器入口固定为 `apps/frontend` 与 `apps/gateway-service`。
2. 论坛服务与商城服务在内部网络中平级运行，服务间调用直接走内部地址，不走公网网关。

## 1. 启动基础设施

```bash
cd deploy/local
docker compose --env-file .env.example up -d
```

本地编排会自动挂载以下初始化脚本：

- `sql/mall/00-create-databases.sql`
- `sql/forum/00-study-schema.sql`
- `sql/forum/01-quartz-tables.sql`

其中：

- 商城库会自动创建 `ecommerce_auth`、`ecommerce_product`、`ecommerce_inventory`、`ecommerce_order`、`ecommerce_cart`、`ecommerce_support`。
- 论坛库会自动创建 `study` 以及 `community-post-service` 所需的 Quartz JDBC 表。

## 2. 环境变量模板

`deploy/local/.env.example` 已经补齐以下边界配置：

- 基础设施地址：`MYSQL_HOST`、`REDIS_HOST`、`KAFKA_BOOTSTRAP_SERVERS`、`NACOS_SERVER_ADDR`、`RABBIT_HOST`、`ELASTICSEARCH_URIS`
- 统一安全配置：`JWT_SECRET`、`JWT_EXPIRE_MINUTES`
- 内部服务地址：`FORUM_*_SERVICE_URL`、`MALL_*_SERVICE_URL`、`COMMUNITY_USER_BASE_URL`
- 前后端边界：`COMMUNITY_PUBLIC_DOMAIN`、`COMMUNITY_CORS_ALLOWED_ORIGINS`
- 第三方依赖：`FORUM_MAIL_*`、`QINIU_*`、`OPENAI_*`

建议做法：

- 启动任何 Java 服务前，先确认 `JAVA_HOME` 指向 JDK 17。
- 本地开发直接复制 `.env.example` 为 `.env` 使用。
- 生产环境不要使用默认 JWT 密钥和默认数据库口令。
- 如果七牛地域或上传域名变化，只改 `QINIU_UPLOAD_HOST`，不再修改前端脚本。

## 3. 推荐启动顺序

1. `services/forum/community-user-service`
2. `services/forum/community-message-service`
3. `services/forum/community-social-service`
4. `services/forum/community-media-service`
5. `services/forum/community-data-service`
6. `services/forum/community-post-service`
7. `services/mall/auth-service`
8. `services/mall/product-service`
9. `services/mall/inventory-service`
10. `services/mall/order-service`
11. `services/mall/cart-service`
12. `services/mall/support-service`
13. `apps/gateway-service`
14. `apps/frontend`

仓库中保留了 `scripts/dev/print-startup-order.ps1`，可直接输出推荐启动顺序。

## 4. 已落地的边界修复

- `apps/gateway-service` 的论坛与商城路由地址已全部改为环境变量可覆盖。
- 商城 `auth-service` 不再通过网关反调论坛登录，而是直接调用 `community-user-service`。
- 论坛与商城的数据库、Redis、Kafka、Nacos、RabbitMQ、Elasticsearch 地址均已外置。
- 七牛头像上传域名已由后端配置注入前端页面，不再硬编码在 `setting.js` 中。

## 5. 仍需外部准备的能力

以下依赖属于业务外部能力，仓库不会自动替你准备真实账号：

- SMTP 邮箱账号与授权码
- `wkhtmltoimage` 可执行程序
- 七牛云存储空间与密钥
- OpenAI 或其它 LLM 服务密钥
- 生产环境数据库账号、备份、监控与日志采集
