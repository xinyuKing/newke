# 认证服务（auth-service）

## 功能概述
提供用户注册、登录、权限管理与 JWT 鉴权能力，统一登录态与权限控制。
支持个人资料维护与用户地址管理。

## 端口
- `18081`

## 依赖
- MySQL（`ecommerce_auth`）

## 核心配置
- JWT 密钥与有效期：`security.jwt.secret` `security.jwt.expire-minutes`
- 数据库连接池：`DB_POOL_MAX` `DB_POOL_MIN` `DB_CONN_TIMEOUT_MS` `DB_IDLE_TIMEOUT_MS` `DB_MAX_LIFETIME_MS`

## 启动
```bash
mvn -q -DskipTests package
```
启动 `AuthApplication`。

## 规范与说明
- 代码注释遵循阿里巴巴 Java 开发手册，面向外部接口使用 Javadoc。
- 对外接口不返回敏感信息，异常统一由全局异常处理器管理。
