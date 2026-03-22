# 客服服务（support-service）

## 功能概述
提供客服对话、退款意图识别与相关辅助能力，包含模型配置与规则化策略。

## 端口
- `18086`

## 依赖
- MySQL（`ecommerce_support`）
- Redis（会话与状态）

## 核心配置
- 退款意图识别配置：`refund.intent.*`
- 模型配置：`refund.model.profiles.*`
- Redis 连接池：`REDIS_POOL_MAX` `REDIS_POOL_MAX_IDLE` `REDIS_POOL_MIN_IDLE` `REDIS_POOL_MAX_WAIT_MS` `REDIS_TIMEOUT_MS`
- 数据库连接池：`DB_POOL_MAX` `DB_POOL_MIN` `DB_CONN_TIMEOUT_MS` `DB_IDLE_TIMEOUT_MS` `DB_MAX_LIFETIME_MS`

## 启动
```bash
mvn -q -DskipTests package
```
启动 `SupportApplication`。

## 规范与说明
- 规则与模型配置通过配置文件解耦。
- 注释遵循阿里巴巴 Java 开发手册。