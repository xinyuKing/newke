# 网关服务（gateway-service）

## 功能概述
统一路由、鉴权、限流与熔断。支持灰度路由（通过 Header 触发），并在网关层注入用户信息头。

## 端口
- `8080`

## 依赖
- Redis（限流）
- 各业务服务（auth/product/order/cart/support）

## 核心配置
- JWT：`security.jwt.secret` `security.jwt.expire-minutes`
- 限流：`GATEWAY_RL_REPLENISH` `GATEWAY_RL_BURST`
- 灰度路由：`PRODUCT_GRAY_URL` + 请求头 `X-Canary: true`
- Redis：`REDIS_TIMEOUT_MS` `REDIS_POOL_MAX` `REDIS_POOL_MAX_IDLE` `REDIS_POOL_MIN_IDLE`

## 启动
```bash
mvn -q -DskipTests package
```
启动 `GatewayApplication`。

## 规范与说明
- 网关统一鉴权，未认证请求返回 401。
- 默认对所有路由开启限流与熔断。
- 注释遵循阿里巴巴 Java 开发手册。
