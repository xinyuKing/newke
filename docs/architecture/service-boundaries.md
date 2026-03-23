# 服务边界说明

## 外部入口层

- `apps/frontend`：统一前端承载论坛与商城视图。
- `apps/gateway-service`：统一鉴权、限流、路由入口，是浏览器访问后端的唯一公网层。

## 论坛域

- `services/forum/community-user-service`：论坛用户、登录、注册、资料与头像上传。
- `services/forum/community-post-service`：帖子、评论、搜索、分享、内容审核。
- `services/forum/community-social-service`：点赞、关注、粉丝关系。
- `services/forum/community-message-service`：私信、系统通知、分享图生成与消息消费。
- `services/forum/community-media-service`：论坛媒体资源管理。
- `services/forum/community-data-service`：站点统计、UV/DAU 等数据能力。

## 商城域

- `services/mall/auth-service`：商城账号体系与 JWT 签发，同时复用论坛用户体系。
- `services/mall/product-service`：商品、评价、商家侧管理、搜索索引。
- `services/mall/inventory-service`：库存初始化、扣减、释放。
- `services/mall/order-service`：订单创建、状态流转、物流协同。
- `services/mall/cart-service`：购物车与下单前聚合。
- `services/mall/support-service`：售后、客服、AI 辅助处理。

## 共享模块

- `shared/forum/community-common`：论坛公共实体、常量、工具与消息事件。
- `shared/mall/mall-common`：商城公共 DTO、异常、JWT 与通用安全能力。

## 历史归档

- `legacy/community-gateway`：旧论坛网关的归档实现，仅作为迁移参考。
