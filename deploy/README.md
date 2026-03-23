# 部署目录说明

`deploy/` 目录用于承载统一论坛 + 商城项目的部署资产与边界文档，当前已经按“公网入口”和“内部服务边界”拆分完成。

## 目录内容

- `deploy/local/docker-compose.yml`：本地基础设施编排，负责启动 MySQL、Redis、Kafka、Zookeeper、Nacos、RabbitMQ、Elasticsearch。
- `deploy/local/.env.example`：本地运行时的统一环境变量模板，已经覆盖数据库、Redis、Kafka、Nacos、JWT、内部服务 URL、Qiniu、OpenAI 等关键边界。
- `deploy/local/README.md`：本地联调步骤、启动顺序、边界说明与未容器化部分说明。

## 当前边界约定

- 浏览器只访问 `apps/frontend` 和 `apps/gateway-service` 暴露的入口。
- 服务间调用不再反向穿透网关，典型示例是 `services/mall/auth-service` 直接调用 `services/forum/community-user-service`。
- 基础设施地址、JWT 密钥、跨服务 URL、七牛上传域名等都已外置到环境变量，避免把部署环境写死在仓库中。
- `legacy/community-gateway` 仅保留为历史参考，不再属于当前运行链路。
