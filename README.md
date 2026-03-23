# Newke

Newke 是一个把“论坛社区”和“电商商城”真正整合到同一仓库中的多模块项目。

这个仓库不是把两个系统简单并排摆放，而是围绕“统一入口、统一用户体系、统一网关、独立服务边界”重新整理后的工程结构：

- 前端统一为 `apps/frontend`，论坛与商城都在同一个 Vue 3 应用中承载。
- 后端统一通过 `apps/gateway-service` 对外暴露入口。
- 论坛域与商城域在服务层面平级运行，互相调用走内部地址，不反向穿透网关。
- 用户体系以论坛 `community-user-service` 为主账号中心，商城 `auth-service` 负责商城侧 JWT、地址簿、角色与状态扩展能力。
- 根目录 `pom.xml` 作为聚合工程，统一管理论坛父 POM、商城父 POM 和所有服务模块。

## 项目亮点

### 1. 论坛与商城前后端一体化整合

- 单仓库维护前端、网关、论坛服务、商城服务、共享模块、SQL 与部署脚本。
- Vue 3 前端同时承载论坛页面与商城页面，路由层已融合。
- 网关统一转发 `/community/**` 与 `/api/**` 两类请求，浏览器只需要认识一个后端入口。

### 2. 用户体系重点融合

当前用户链路按“主账号 + 业务扩展”设计：

- 论坛 `community-user-service` 负责账号注册、登录、基础资料、头像、激活等主账号能力。
- 商城 `auth-service` 通过内部 Feign 直接调用论坛用户中心完成登录/注册协同。
- 商城侧继续负责 JWT 签发、商城用户资料、收货地址、管理后台用户状态与角色维护。
- 网关完成 JWT 校验后，统一向下游透传 `X-User-Id`、`X-User-Role`。

这意味着：

- 浏览器层看起来是一个站点。
- 认证层是一套主账号。
- 商城和论坛仍保留各自清晰的业务边界。

### 3. 服务边界清晰，不再互相套娃

本项目明确区分三层边界：

- 公网入口边界：只有前端和网关对浏览器开放。
- 内部服务边界：服务与服务之间直接通信，不反向走网关。
- 数据边界：论坛和商城数据库按职责拆分，基础设施统一外置配置。

典型例子：

- 商城 `auth-service` 不再通过 `gateway-service` 去调用论坛登录接口。
- 商品、订单、购物车、库存之间的内部 URL 都通过环境变量声明。
- 七牛上传域名、JWT 密钥、Redis/Kafka/Nacos/Elasticsearch 地址不再写死在代码里。

### 4. 结构整理完成，旧商城目录已彻底移除

仓库已经完成以下结构清理：

- 移除 `ecommerce/` 旧目录。
- 移除 `ecommerce-parent/`，商城父 POM 已并入 `platform/mall-parent`。
- 旧论坛网关实现迁移到 `legacy/community-gateway/`，仅作归档参考，不再参与当前启动链路。

## 仓库结构

```text
newke
|-- apps/
|   |-- frontend/
|   `-- gateway-service/
|-- services/
|   |-- forum/
|   |   |-- community-user-service/
|   |   |-- community-post-service/
|   |   |-- community-social-service/
|   |   |-- community-message-service/
|   |   |-- community-media-service/
|   |   `-- community-data-service/
|   `-- mall/
|       |-- auth-service/
|       |-- product-service/
|       |-- inventory-service/
|       |-- order-service/
|       |-- cart-service/
|       `-- support-service/
|-- shared/
|   |-- forum/community-common/
|   `-- mall/mall-common/
|-- platform/
|   |-- forum-parent/
|   `-- mall-parent/
|-- deploy/
|   `-- local/
|-- docs/
|   `-- architecture/
|-- sql/
|-- scripts/
|   `-- dev/
|-- legacy/
|   `-- community-gateway/
`-- pom.xml
```

## 模块说明

### 前端与网关

| 模块 | 作用 | 默认端口 |
| --- | --- | --- |
| `apps/frontend` | 统一前端应用，承载论坛与商城界面 | `5173` |
| `apps/gateway-service` | 统一鉴权、限流、熔断、路由转发入口 | `8080` |

### 论坛域服务

| 模块 | 作用 | 默认端口 |
| --- | --- | --- |
| `services/forum/community-user-service` | 用户、登录、注册、资料、头像、激活 | `8081` |
| `services/forum/community-message-service` | 私信、通知、分享图生成、消息消费 | `8082` |
| `services/forum/community-social-service` | 点赞、关注、粉丝关系 | `8083` |
| `services/forum/community-media-service` | 论坛媒体资源管理 | `8084` |
| `services/forum/community-data-service` | UV/DAU、后台统计数据 | `8085` |
| `services/forum/community-post-service` | 帖子、评论、搜索、分享、内容审核 | `8086` |

### 商城域服务

| 模块 | 作用 | 默认端口 |
| --- | --- | --- |
| `services/mall/auth-service` | 商城认证、JWT、用户资料、地址、后台用户管理 | `18081` |
| `services/mall/product-service` | 商品、商家商品、评价、搜索、推荐 | `18082` |
| `services/mall/inventory-service` | 库存初始化、扣减、释放、锁定 | `18083` |
| `services/mall/order-service` | 下单、支付、发货、确认收货、物流查询 | `18084` |
| `services/mall/cart-service` | 购物车、结算前聚合 | `18085` |
| `services/mall/support-service` | 售后、客服会话、退款意图、AI 辅助能力 | `18086` |

### 共享模块

| 模块 | 作用 |
| --- | --- |
| `shared/forum/community-common` | 论坛公共实体、常量、工具类、事件定义 |
| `shared/mall/mall-common` | 商城公共 DTO、异常、JWT、安全能力 |
| `platform/forum-parent` | 论坛域父 POM |
| `platform/mall-parent` | 商城域父 POM |

## 当前技术栈与版本基线

### 后端

- JDK 17
- Maven 3.8+
- Spring Boot 3.2.9
- Spring Cloud 2023.0.1
- Spring Cloud Alibaba 2023.0.1.3
- Spring Security 6
- Spring Cloud Gateway
- OpenFeign
- MyBatis + JPA（论坛与商城分别保留原有数据访问风格）
- Redis
- Kafka
- RabbitMQ
- Nacos
- Elasticsearch 8.x / 可扩展 OpenSearch

### 前端

- Vue 3
- Vue Router 4
- Pinia
- Axios
- Vite 5

### 基础设施（本地模板）

`deploy/local/docker-compose.yml` 当前包含：

- MySQL 8.0.36
- Redis 7.2.4
- Zookeeper 3.9
- Kafka 3.6
- Nacos 2.3.2
- RabbitMQ 3.13 Management
- Elasticsearch 8.13.4

## 版本说明

### 为什么当前必须使用 JDK 17

本项目已经统一到 Spring Boot 3.2.9。

Spring Boot 3.x 的官方运行基线就是 JDK 17，因此当前仓库不能再以 JDK 11 作为运行标准。换句话说：

- `Spring Boot 3.x + JDK 11` 不是一个可持续的组合。
- 当前仓库的正确基线是 `JDK 17 + Spring Boot 3.2.9`。

如果后续继续升级 Spring 生态，JDK 17 仍然是更稳妥的长期基线。

## 用户体系融合设计

这一部分是整个整合工程最关键的地方。

### 设计原则

- 主账号只保留一套。
- 商城不再重复建设另一套基础账号体系。
- 商城保留自己的业务扩展字段和认证令牌能力。
- 浏览器访问仍然只走统一网关。
- 内部服务调用直接走内网地址。

### 当前落地方式

1. 用户注册时，商城 `auth-service` 会通过内部 Feign 调用论坛 `community-user-service`。
2. 用户登录时，商城仍复用论坛用户校验结果。
3. 登录成功后，商城签发自己的 JWT，供 `/api/**` 体系使用。
4. 网关校验 JWT 后，把用户 ID 和角色透传给商城服务。
5. 论坛保留自己的社区页面、社区接口与资料体系。
6. 商城保留地址簿、角色、启停状态、后台用户管理等商城专属能力。

### 带来的收益

- 用户不需要在论坛和商城分别注册两次。
- 论坛仍然可以独立演进帖子、消息、社交能力。
- 商城仍然可以独立演进订单、库存、售后能力。
- 用户中心的职责划分比“所有东西都塞到一个服务里”更清晰。

## 本地开发与部署

### 1. 环境准备

在启动项目之前，请先确认本机具备以下环境：

- JDK 17
- Maven 3.8+
- Node.js 18+
- Docker / Docker Compose

### 2. 启动基础设施

项目已经提供本地基础设施编排文件：

```bash
cd deploy/local
docker compose --env-file .env.example up -d
```

该编排会自动挂载以下 SQL：

- `sql/mall/00-create-databases.sql`
- `sql/forum/00-study-schema.sql`
- `sql/forum/01-quartz-tables.sql`

### 3. 关键环境变量

建议把 `deploy/local/.env.example` 复制为你自己的 `.env` 再修改。

重点关注这些变量：

- `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_USERNAME`、`MYSQL_PASSWORD`
- `REDIS_HOST`、`REDIS_PORT`
- `KAFKA_BOOTSTRAP_SERVERS`
- `NACOS_SERVER_ADDR`
- `RABBIT_HOST`、`RABBIT_PORT`
- `ELASTICSEARCH_URIS`
- `JWT_SECRET`、`JWT_EXPIRE_MINUTES`
- `COMMUNITY_USER_BASE_URL`
- `FORUM_*_SERVICE_URL`
- `MALL_*_SERVICE_URL`
- `COMMUNITY_PUBLIC_DOMAIN`
- `COMMUNITY_CORS_ALLOWED_ORIGINS`
- `QINIU_UPLOAD_HOST`
- `OPENAI_API_KEY`

### 4. 后端推荐启动顺序

论坛服务：

1. `services/forum/community-user-service`
2. `services/forum/community-message-service`
3. `services/forum/community-social-service`
4. `services/forum/community-media-service`
5. `services/forum/community-data-service`
6. `services/forum/community-post-service`

商城服务：

1. `services/mall/auth-service`
2. `services/mall/product-service`
3. `services/mall/inventory-service`
4. `services/mall/order-service`
5. `services/mall/cart-service`
6. `services/mall/support-service`

最后再启动：

1. `apps/gateway-service`
2. `apps/frontend`

仓库中提供了辅助脚本：

- `scripts/dev/print-startup-order.ps1`
- `scripts/dev/verify-structure.py`

### 5. 常用启动命令

后端服务示例：

```bash
cd services/forum/community-user-service
mvn spring-boot:run
```

网关示例：

```bash
cd apps/gateway-service
mvn spring-boot:run
```

前端示例：

```bash
cd apps/frontend
npm install
npm run dev
```

### 6. 本地访问入口

- 前端开发入口：`http://localhost:5173`
- 统一网关入口：`http://localhost:8080`
- Nacos：`http://localhost:8848`
- RabbitMQ 管理台：`http://localhost:15672`
- Elasticsearch：`http://localhost:9200`

## 网关路由约定

当前网关主要承载两组流量：

- `/community/**`：论坛相关页面与接口
- `/api/**`：商城相关接口

前端路由层则统一承载：

- 论坛页面，例如 `/`、`/post/:id`、`/messages`、`/settings`
- 商城页面，例如 `/mall`、`/mall/cart`、`/mall/orders`、`/mall/support`

## SQL 与初始化说明

### 论坛侧

- `sql/forum/00-study-schema.sql`：论坛 `study` 数据库基础结构
- `sql/forum/01-quartz-tables.sql`：`community-post-service` 的 Quartz JDBC 表

### 商城侧

- `sql/mall/00-create-databases.sql`：创建商城各服务数据库

商城各服务目前保留 JPA 自动建表能力，因此数据库脚本主要负责建库。

## 部署边界说明

### 公网入口边界

浏览器只应该访问：

- `apps/frontend`
- `apps/gateway-service`

不要让浏览器直接访问论坛或商城某个后端服务端口。

### 内部服务边界

服务内部互调直接走内部地址，不经过网关。

当前已经落实的边界修复包括：

- 商城 `auth-service` 直接调用论坛 `community-user-service`
- 商品、订单、购物车、库存的内部依赖地址全部环境变量化
- 网关只负责浏览器入口，不负责内部服务的二次转发

### 数据边界

- 论坛共享 `study` 数据库
- 商城按服务拆分数据库：`ecommerce_auth`、`ecommerce_product`、`ecommerce_inventory`、`ecommerce_order`、`ecommerce_cart`、`ecommerce_support`
- Redis、Kafka、RabbitMQ、Nacos、Elasticsearch 统一视作外部基础设施

### 第三方能力边界

以下能力需要部署环境自己准备真实凭据：

- SMTP 邮件服务
- 七牛云 Bucket 与密钥
- `wkhtmltoimage`
- OpenAI / 其它 LLM 服务密钥

## 代码规范

本仓库当前按照“先统一关键链路，再逐步覆盖全仓”的方式推进代码规范治理。

本轮已经重点整理了以下方面：

- 统一使用构造器注入核心依赖，减少字段注入。
- 补充控制器、网关、跨服务调用接口的类注释与方法注释。
- 统一关键配置项命名，避免魔法值散落。
- 把部署相关常量抽离到环境变量，减少硬编码。
- 为仓库新增 `.editorconfig`，统一 Java、Vue、YAML、Markdown 等文件的基础缩进与编码规则。

规范参考：

- 《阿里巴巴 Java 开发手册》
- Spring Boot 3 / Spring Security 6 当前推荐实践

## 兼容性说明

### 1. 旧论坛 Thymeleaf 页面仍保留

目前推荐主入口是 `apps/frontend` 的统一前端应用。

同时，为了兼容论坛原有页面链路，论坛服务中仍保留部分 Thymeleaf 页面和静态模板。这些内容不影响统一前端的使用，但后续如果要彻底前端单页化，还可以继续收敛。

### 2. `legacy/community-gateway` 仅作归档

- 不参与当前编译链路
- 不参与当前启动链路
- 不建议作为新功能开发入口

## 当前建议

如果你接下来还要继续把项目往“更像生产项目”的方向推进，优先顺序建议是：

1. 补充统一配置中心方案，把 `.env` 与 Nacos 配置正式接起来。
2. 给各服务补充最基础的启动自检、健康检查和数据库迁移策略。
3. 为前端统一补齐登录态恢复、权限页面守卫和错误页体验。
4. 为核心链路补测试，优先覆盖用户登录、下单、购物车、帖子发布四条主流程。

## 本轮已额外完成的边界修复

针对此前 README 中列出的“已知问题”和“部署边界”，仓库已经继续完成以下收口：

1. 论坛与商城核心服务的数据库、Redis、Kafka、Nacos、RabbitMQ、Elasticsearch 地址均已外置。
2. 网关中的论坛与商城路由地址已改为环境变量可覆盖。
3. 商城 `auth-service` 保持直接调用论坛用户中心，不再通过网关反调。
4. 商城各服务共享的 JWT 密钥与过期时间已经统一外置。
5. 七牛头像上传域名已由后端配置注入前端页面，不再写死在前端脚本中。
6. 本地部署模板 `deploy/local/.env.example` 已补齐关键变量。
7. 部署与架构文档已经同步更新到 `deploy/` 与 `docs/architecture/`。
