# Newke

Newke 是一个把论坛社区和电商商城放在同一仓库里协同开发的多模块项目。

这个仓库的重点不是“把两个系统放在一起”，而是把它们整理成一套更清楚的工程结构：

- 前端统一放在 `apps/frontend`
- 浏览器统一通过 `apps/gateway-service` 进入后端
- 论坛域和商城域在服务层面平级运行，内部调用直接走服务地址
- 用户主账号以论坛用户中心为核心，商城保留自己的认证和业务扩展能力
- 根目录 `pom.xml` 负责聚合全部 Maven 模块

## 当前结构

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
|-- scripts/
|   `-- dev/
|-- sql/
|-- legacy/
|   `-- community-gateway/
`-- pom.xml
```

## 技术栈

### 后端

- JDK 17
- Maven 3.8+
- Spring Boot 3.2.9
- Spring Cloud 2023.0.1
- Spring Cloud Alibaba 2023.0.1.3
- Spring Security 6
- Spring Cloud Gateway
- OpenFeign
- MyBatis / JPA
- Redis
- Kafka
- RabbitMQ
- Nacos
- Elasticsearch

### 前端

- Vue 3
- Vue Router 4
- Pinia
- Axios
- Vite 5

### 本地基础设施

`deploy/local/docker-compose.yml` 当前会拉起：

- MySQL 8
- Redis 7
- Zookeeper
- Kafka
- Nacos
- RabbitMQ
- Elasticsearch

## 环境要求

启动项目前，请先确认本机满足以下条件：

- `JAVA_HOME` 指向 JDK 17
- Maven 3.8 及以上
- Node.js 18 及以上
- Docker / Docker Compose

这份仓库已经统一到 Spring Boot 3，因此默认构建基线就是 JDK 17。用 JDK 11 或更低版本启动、编译或导入 IDE，都会遇到兼容性问题。

## 快速开始

### 1. 启动基础设施

```bash
cd deploy/local
docker compose --env-file .env.example up -d
```

本地编排会自动挂载这些初始化脚本：

- `sql/mall/00-create-databases.sql`
- `sql/forum/00-study-schema.sql`
- `sql/forum/01-quartz-tables.sql`

更详细的本地部署说明见 [deploy/local/README.md](deploy/local/README.md)。

### 2. 检查后端构建环境

在仓库根目录执行：

```bash
mvn validate
```

这一步除了做 Maven 校验，也会执行项目已经接入的规范检查：

- Spotless
- Checkstyle

### 3. 推荐启动顺序

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

最后启动：

1. `apps/gateway-service`
2. `apps/frontend`

仓库里有一个辅助脚本可以直接打印启动顺序：

```powershell
pwsh scripts/dev/print-startup-order.ps1
```

### 4. 常用启动命令

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

### 5. 本地访问入口

- 前端开发入口：`http://localhost:5173`
- 网关入口：`http://localhost:8080`
- Nacos：`http://localhost:8848`
- RabbitMQ 管理台：`http://localhost:15672`
- Elasticsearch：`http://localhost:9200`

## 模块说明

### 前端与网关

| 模块 | 说明 | 端口 |
| --- | --- | --- |
| `apps/frontend` | 统一前端应用，承载论坛与商城页面 | `5173` |
| `apps/gateway-service` | 对外统一入口，负责网关路由、鉴权、限流、熔断 | `8080` |

### 论坛域

| 模块 | 说明 | 端口 |
| --- | --- | --- |
| `community-user-service` | 用户、注册、登录、资料、激活 | `8081` |
| `community-message-service` | 私信、通知、消息消费、分享图生成 | `8082` |
| `community-social-service` | 点赞、关注、粉丝关系 | `8083` |
| `community-media-service` | 论坛媒体资源管理 | `8084` |
| `community-data-service` | UV/DAU 和后台统计 | `8085` |
| `community-post-service` | 帖子、评论、搜索、分享、内容审核 | `8086` |

### 商城域

| 模块 | 说明 | 端口 |
| --- | --- | --- |
| `auth-service` | 商城认证、JWT、用户资料、地址、后台用户管理 | `18081` |
| `product-service` | 商品、评价、搜索、推荐 | `18082` |
| `inventory-service` | 库存初始化、扣减、释放、锁定 | `18083` |
| `order-service` | 下单、支付、发货、物流查询 | `18084` |
| `cart-service` | 购物车与结算前聚合 | `18085` |
| `support-service` | 售后、客服会话、退款意图、AI 辅助能力 | `18086` |

### 共享模块

| 模块 | 说明 |
| --- | --- |
| `shared/forum/community-common` | 论坛公共实体、常量、工具类、事件定义 |
| `shared/mall/mall-common` | 商城公共 DTO、异常、JWT、安全能力 |
| `platform/forum-parent` | 论坛父 POM |
| `platform/mall-parent` | 商城父 POM |

## 路由与服务边界

项目当前有两组主要外部路由：

- `/community/**`：论坛页面与论坛接口
- `/api/**`：商城接口

开发时建议始终记住这几条边界：

- 浏览器只直接访问前端和网关
- 服务与服务之间走内部地址，不反向穿透网关
- 基础设施地址统一通过环境变量配置
- JWT、七牛上传域名、服务间 URL 等敏感或可变配置不要写死

## 用户体系说明

当前用户链路按“主账号 + 业务扩展”设计：

- 论坛 `community-user-service` 负责主账号能力
- 商城 `auth-service` 通过内部调用复用论坛用户中心
- 商城保留自己的 JWT、地址簿、角色和状态管理
- 网关完成认证后，把用户上下文透传给下游服务

这样做的直接好处是：

- 用户不需要在论坛和商城分别注册两次
- 论坛和商城仍然可以各自独立演进
- 用户中心的职责边界更清楚

## 代码规范

仓库当前已经有这几层基础规范：

- `.editorconfig` 统一基础缩进、换行和编码规则
- Maven `validate` 阶段自动执行 Spotless 和 Checkstyle
- 后端统一按 JDK 17 编译

日常开发建议至少先跑一次：

```bash
mvn validate
```

如果你在做仓库结构调整，也可以运行：

```bash
python scripts/dev/verify-structure.py
```

## 文档入口

- 本地部署说明：[deploy/local/README.md](deploy/local/README.md)
- 架构说明：[docs/architecture/README.md](docs/architecture/README.md)
- 部署边界：[docs/architecture/deployment-boundaries.md](docs/architecture/deployment-boundaries.md)
- 服务边界：[docs/architecture/service-boundaries.md](docs/architecture/service-boundaries.md)
- 开发脚本说明：[scripts/dev/README.md](scripts/dev/README.md)

## 兼容性与遗留目录

- 论坛服务里仍保留了一部分 Thymeleaf 页面链路，主要用于兼容旧页面
- `legacy/community-gateway` 只作归档参考，不参与当前主链路开发

## 接下来可以优先做什么

如果你准备继续往下推进，这几个方向会比较值：

1. 补更完整的自动化测试，先覆盖登录、发帖、下单、购物车
2. 继续收口旧页面控制器和服务层的规范问题
3. 把配置中心、健康检查、数据库迁移这类生产化能力补齐
4. 持续减少硬编码配置，把环境差异都收进统一配置入口
