# 部署边界说明

## 1. 公网入口边界

统一项目对浏览器暴露的入口只有两层：

- `apps/frontend`：前端应用开发入口，默认本地地址为 `http://localhost:5173`
- `apps/gateway-service`：统一网关入口，默认本地地址为 `http://localhost:8080`

浏览器请求应始终先进入网关，再由网关转发到论坛或商城服务。

## 2. 内部服务边界

论坛服务与商城服务在部署层面处于同一级，不存在“商城挂在论坛下面”的隐藏调用链。

当前约定如下：

- 服务对服务调用走内部地址。
- 网关只负责浏览器入口与统一鉴权，不负责内部服务中转。
- 内部地址全部通过环境变量声明，便于本地、测试、生产环境切换。

已落地的典型修复：

- `services/mall/auth-service` 已改为通过 Feign 直接调用 `services/forum/community-user-service`
- `services/mall/product-service`、`services/mall/order-service`、`services/mall/cart-service` 的内部依赖地址已统一外置

## 3. 数据边界

- 论坛侧共享 `study` 数据库。
- 商城侧按服务拆分数据库：`ecommerce_auth`、`ecommerce_product`、`ecommerce_inventory`、`ecommerce_order`、`ecommerce_cart`、`ecommerce_support`。
- Redis、Kafka、RabbitMQ、Elasticsearch、Nacos 全部视为基础设施边界，通过环境变量接入。

## 4. 第三方能力边界

以下内容属于部署环境需要提供的外部能力：

- SMTP 邮件服务
- `wkhtmltoimage`
- 七牛云上传域名、Bucket 与密钥
- OpenAI / LLM 接口地址与密钥

这些内容已经从应用默认配置中抽离到环境变量，不再需要改代码才能切环境。

## 5. 历史归档边界

- `legacy/community-gateway` 仅保留历史实现说明。
- 它不再属于当前统一项目的编译、部署和启动链路。
