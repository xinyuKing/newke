# Newke 融合社区与商城微服务平台

## 项目介绍

这是一个把论坛社区和电商商城放进同一仓库的融合项目。

论坛侧保留了原来的社区能力，包括注册登录、帖子发布、评论回复、点赞关注、私信通知、媒体上传、站点统计、热帖刷新、搜索和分享。商城侧按业务域拆成认证、商品、库存、订单、购物车、客服、网关等服务，覆盖从登录、浏览商品、加入购物车、下单、库存扣减到售后支持的一整条链路。

这套仓库现在最有意思的地方，不是单纯把两个项目放在一起，而是已经把用户系统打通了。商城的 `auth-service` 不再自己独立维护一套主账号体系，而是通过论坛侧的 `community-user-service` 完成注册和登录，再把社区用户同步成商城本地账号，并继续签发 JWT 给商城各服务使用。换句话说，论坛账号已经是商城账号的上游身份源。

当前仓库仍然处在“能跑、能联调、但还没完全统一治理”的阶段。论坛侧偏 Spring Boot 2.7 / MyBatis / Nacos，商城侧偏 Spring Boot 3.3 / JPA / JWT。两边已经整合到一个代码仓库里，但在构建体系、端口规划、初始化脚本这些方面，还保留着明显的过渡痕迹。README 会把这些情况写清楚，方便你部署、展示和继续迭代。

## 仓库结构

```text
newke
├─ community-common              # 论坛公共模块
├─ community-user-service        # 论坛用户服务
├─ community-post-service        # 论坛主站、帖子、评论、搜索、分享
├─ community-message-service     # 论坛消息服务
├─ community-social-service      # 论坛点赞、关注
├─ community-media-service       # 论坛媒体上传
├─ community-data-service        # 论坛 UV / DAU 统计
├─ community-gateway             # 论坛网关骨架，当前仍是预留模块
├─ common                        # 商城公共模块
├─ auth-service                  # 商城认证与账户资料
├─ product-service               # 商城商品与评价
├─ inventory-service             # 商城库存
├─ order-service                 # 商城订单
├─ cart-service                  # 商城购物车
├─ support-service               # 商城客服与售后
├─ gateway-service               # 商城统一网关
├─ frontend                      # Vue 3 前端
├─ pom.xml                       # 仓库根聚合 POM
└─ README.md
```

## 子系统总览

### 论坛侧模块

| 模块 | 默认端口 | 作用 | 关键依赖 |
| --- | --- | --- | --- |
| `community-post-service` | `8080` | 论坛主站入口，负责帖子、评论、搜索、分享、Thymeleaf 页面渲染 | MySQL `study`、Redis、Kafka、Elasticsearch、Quartz、邮件、七牛云、wkhtmltoimage |
| `community-user-service` | `8081` | 注册、登录、验证码、激活、密码修改、头像修改、论坛用户资料 | MySQL `study`、Redis、邮件、Nacos |
| `community-message-service` | `8082` | 私信、系统通知、消息详情、分享相关能力 | MySQL `study`、Kafka、Elasticsearch、Nacos、七牛云、wkhtmltoimage |
| `community-social-service` | `8083` | 点赞、关注、粉丝、关注列表 | Redis、Kafka、Nacos |
| `community-media-service` | `8084` | 图片和视频上传、媒体类型校验、媒体目录管理 | Redis、Nacos、本地文件目录 |
| `community-data-service` | `8085` | UV / DAU 统计与后台数据面板 | Redis、Nacos |
| `community-gateway` | 未配置 | 论坛网关骨架，当前只有启动类，没有完整路由配置 | Spring Cloud Gateway、Nacos |
| `community-common` | 无 | 论坛公共实体、工具类、统一响应结构 | 被论坛服务依赖 |

### 商城侧模块

| 模块 | 默认端口 | 作用 | 关键依赖 |
| --- | --- | --- | --- |
| `gateway-service` | `8080` | 商城统一网关，负责 `/api/**` 路由、JWT 校验、限流、熔断 | Redis |
| `auth-service` | `18081` | 商城登录、注册、JWT 签发、个人资料、地址管理 | MySQL `ecommerce_auth`、论坛用户服务 |
| `product-service` | `18082` | 商品、评价、搜索、推荐、评价摘要与统计 | MySQL `ecommerce_product`、Redis、RabbitMQ、库存服务、可选 OpenSearch |
| `inventory-service` | `18083` | 库存初始化、扣减、释放、批量库存操作 | MySQL `ecommerce_inventory`、Redis、Lua |
| `order-service` | `18084` | 下单、取消、支付、发货、收货、物流、订单事件 | MySQL `ecommerce_order`、Redis、RabbitMQ、商品服务、库存服务 |
| `cart-service` | `18085` | 购物车增删改查、结算、转订单 | MySQL `ecommerce_cart`、Redis、商品服务、订单服务 |
| `support-service` | `18086` | 客服对话、售后单、退款意图识别、退款规则与模型配置 | MySQL `ecommerce_support`、Redis |
| `common` | 无 | 商城公共 DTO、安全组件、异常和统一响应 | 被商城服务依赖 |

### 前端模块

| 模块 | 默认端口 | 作用 |
| --- | --- | --- |
| `frontend` | `5173` | Vue 3 + Vite 前端开发入口，默认代理论坛主站后端 |

## 用户系统融合说明

这是目前整个仓库最关键的一条整合链路。

1. 论坛用户服务开放了内部认证接口：
   - `POST /community/api/auth/login`
   - `POST /community/api/auth/register`
2. 商城 `auth-service` 中的 `CommunityUserClient` 会调用论坛用户服务完成注册和登录。
3. 商城登录成功后，会把论坛用户同步到本地 `user_account` 表，并直接使用论坛用户 ID 作为商城用户 ID。
4. 商城侧再基于这个统一用户签发 JWT，由 `gateway-service` 统一解析，并透传 `X-User-Id`、`X-User-Role` 给下游服务。

这意味着论坛和商城已经共享主账号身份，不是两套互相独立的用户系统。

### 当前角色映射

商城 `auth-service` 里对论坛角色做了一个简单映射：

| 论坛用户 `type` | 商城角色 |
| --- | --- |
| `0` | `USER` |
| `1` | `ADMIN` |
| `2` | `SUPPORT` |

也就是说，论坛侧的版主角色目前在商城侧会被映射成客服/支持角色。如果后面你想改成 `MERCHANT` 或者单独新增映射逻辑，需要继续调整 `auth-service`。

## 项目亮点

### 1. 同仓库融合两套业务系统

这不是单一论坛，也不是单一商城，而是把“内容社区”和“交易系统”放进了一个仓库里。社区负责内容、关系、互动和用户沉淀，商城负责商品、库存、订单和售后，两边既能单独运行，也能围绕统一账号逐步联动。

### 2. 用户体系已经完成第一阶段打通

账号不再重复注册。商城认证服务已经接到论坛用户服务上，社区用户就是商城用户的身份源，登录注册逻辑不需要再维护两份。这一步很关键，因为后续无论做“帖子带货”“用户画像联动”“社区账号直达商城订单页”，都建立在统一身份之上。

### 3. 论坛侧功能相对完整

论坛部分不是一个只剩登录和发帖的壳，而是已经覆盖了比较完整的社区能力：

- 注册、激活、验证码、登录、退出
- 发帖、评论、回复、帖子详情页
- 点赞、关注、粉丝与关注列表
- 私信、系统通知
- 搜索、分享、热帖分数刷新
- 媒体上传和数据统计

这部分还保留了传统站点风格的服务端渲染页面，适合做教学、答辩和演示。

### 4. 商城侧按业务域拆分得比较清楚

商城不是“大单体商城”写法，而是按认证、商品、库存、订单、购物车、客服拆成独立服务。每个服务只关心自己的业务边界，比如库存服务只管库存一致性，订单服务只管下单流转和状态机，商品服务只管商品和评价，这样后面继续扩展时比较顺手。

### 5. 库存与订单链路考虑了并发问题

库存服务使用 Redis + Lua 脚本做扣减，订单服务也做了批量调用、状态机和事件发布，说明这个项目不是只做“接口能通”，而是已经开始考虑热点场景下的数据一致性和吞吐问题。

### 6. 商城侧有比较明显的性能优化意识

商城部分已经放进了一些很实用的工程化手段：

- 批量商品查询和批量库存扣减，减少跨服务调用次数
- Caffeine 本地缓存，降低热点读取开销
- RabbitMQ 异步化评价统计、订单事件和索引更新
- Resilience4j 熔断、重试、舱壁隔离
- Micrometer + Prometheus 指标暴露

这些东西不是为了堆名词，而是能看出作者已经在往“真实系统”方向推。

### 7. AI 相关能力不是装饰项

论坛 `community-post-service` 里已经预留了兼容 OpenAI 的内容审核配置，商城 `support-service` 里也有退款意图识别、RAG 资料库和多模型配置。这部分还谈不上完整产品化，但已经不是单纯写在 README 里的概念，而是进入了代码和配置层。

### 8. 同时保留传统页面和前后端分离入口

论坛侧目前既有基于 Thymeleaf 的页面，也有 `frontend` 里的 Vue 3 前端。前者适合快速演示和现成页面复用，后者适合后续继续前后端分离改造。

## 技术栈

### 论坛侧

- Java 11
- Spring Boot 2.7.18
- Spring Cloud 2021
- Spring Cloud Alibaba Nacos
- OpenFeign
- MyBatis
- Redis
- Kafka
- Elasticsearch
- Quartz
- Thymeleaf
- Caffeine
- Qiniu
- wkhtmltoimage

### 商城侧

- Java 17
- Spring Boot 3.3.2
- Spring Cloud 2023
- Spring Security
- JWT
- Spring Data JPA
- Redis
- RabbitMQ
- Caffeine
- Resilience4j
- Micrometer / Prometheus
- 可选 OpenSearch

### 前端

- Vue 3
- Vite
- Pinia
- Vue Router
- Axios

## 部署前准备

### 1. 基础环境

建议准备下面这些基础组件：

- JDK 17
- Maven 3.8+
- Node.js 18+
- MySQL 8.x
- Redis 6.x 或更高
- Kafka
- Nacos 2.x
- RabbitMQ 3.x

可选但强烈建议准备：

- Elasticsearch 或 OpenSearch
- wkhtmltoimage
- 邮箱 SMTP 服务
- 七牛云对象存储

### 2. 数据库

本仓库里当前没有现成的 SQL 初始化脚本，因此要区分两类情况：

- 商城侧大多数服务使用 `spring.jpa.hibernate.ddl-auto=update`，只要数据库存在，服务启动后可以自动建表或更新表结构。
- 论坛侧使用 MyBatis，默认依赖现成的 `study` 库表结构。仓库没有附带完整建表 SQL，所以部署论坛时需要你自己准备 `study` 库以及相关表，尤其是用户、帖子、评论、消息、登录凭证、Quartz 调度表等。

建议先手动创建这些数据库：

```sql
CREATE DATABASE study DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE ecommerce_auth DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE ecommerce_product DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE ecommerce_inventory DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE ecommerce_order DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE ecommerce_cart DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE ecommerce_support DEFAULT CHARACTER SET utf8mb4;
```

### 3. 配置安全项

当前配置文件里有一些明显不适合直接用于共享环境或生产环境的敏感信息，比如：

- SMTP 用户名和授权码
- 七牛云 Access Key / Secret Key
- JWT Secret

本地演示可以先沿用，真正部署或开源前建议全部迁移到环境变量或私有配置中心。

## 部署说明

### 先说结论

当前仓库最稳妥的启动方式，不是“根目录一把梭全部跑起来”，而是按子系统、按模块逐个启动。原因很简单：

- 论坛侧和商城侧的技术栈版本还没有完全统一
- 论坛主站和商城网关默认都占用 `8080`
- 论坛库初始化脚本没有在仓库里提供

也就是说，这个仓库已经完成了“代码级整合”和“用户系统融合”，但部署治理还处在过渡阶段。

### 方案一：只启动论坛侧

适合做论坛功能演示、社区模块开发或 Vue 前端联调。

#### 前置依赖

- MySQL：准备 `study`
- Redis
- Kafka
- Nacos
- 可选：Elasticsearch、wkhtmltoimage、七牛云、SMTP

#### 推荐启动顺序

1. `community-user-service`
2. `community-message-service`
3. `community-social-service`
4. `community-media-service`
5. `community-data-service`
6. `community-post-service`

#### 启动命令示例

下面是最直白的模块内启动方式：

```bash
cd community-user-service
mvn spring-boot:run
```

```bash
cd community-message-service
mvn spring-boot:run
```

```bash
cd community-social-service
mvn spring-boot:run
```

```bash
cd community-media-service
mvn spring-boot:run
```

```bash
cd community-data-service
mvn spring-boot:run
```

```bash
cd community-post-service
mvn spring-boot:run
```

#### 访问入口

- 服务端渲染页面：`http://localhost:8080/community`
- Vue 开发前端：`http://localhost:5173`

#### 前端启动

```bash
cd frontend
npm install
npm run dev
```

当前 `frontend/vite.config.js` 默认把 `/api`、`/kaptcha`、`/share` 代理到 `http://localhost:8080/community`，所以论坛主站端口如果改动，前端代理也要一起改。

### 方案二：只启动商城侧

适合做商品、订单、库存、售后联调。

#### 前置依赖

- MySQL：准备 `ecommerce_auth`、`ecommerce_product`、`ecommerce_inventory`、`ecommerce_order`、`ecommerce_cart`、`ecommerce_support`
- Redis
- RabbitMQ
- 可选：OpenSearch

#### 一个必须注意的点

商城 `auth-service` 的注册和登录已经依赖论坛用户服务，所以如果你要完整跑商城登录链路，至少还要额外启动：

- `community-user-service`，默认端口 `8081`

#### 推荐启动顺序

1. `community-user-service`
2. `inventory-service`
3. `product-service`
4. `order-service`
5. `cart-service`
6. `support-service`
7. `auth-service`
8. `gateway-service`

#### 启动命令示例

```bash
cd auth-service
mvn spring-boot:run
```

```bash
cd product-service
mvn spring-boot:run
```

```bash
cd inventory-service
mvn spring-boot:run
```

```bash
cd order-service
mvn spring-boot:run
```

```bash
cd cart-service
mvn spring-boot:run
```

```bash
cd support-service
mvn spring-boot:run
```

```bash
cd gateway-service
mvn spring-boot:run
```

#### 默认接口入口

- 商城网关：`http://localhost:8080`
- 商城认证直连：`http://localhost:18081`
- 商品服务直连：`http://localhost:18082`
- 库存服务直连：`http://localhost:18083`
- 订单服务直连：`http://localhost:18084`
- 购物车服务直连：`http://localhost:18085`
- 客服服务直连：`http://localhost:18086`

### 方案三：论坛和商城一起联调

这才是这个仓库真正想实现的效果，但你要先处理端口冲突。

#### 当前冲突

- `community-post-service` 默认 `8080`
- `gateway-service` 默认 `8080`

两个服务不能直接同时启动。

#### 推荐做法

推荐保留论坛主站 `8080` 不动，把商城网关改成别的端口，比如 `19000`。这样改动成本最低，因为：

- 论坛侧多个地方默认把主站域名写成了 `http://localhost:8080`
- Vue 前端也默认代理到 `8080/community`

你只需要改商城网关配置：

文件：`gateway-service/src/main/resources/application.yml`

```yaml
server:
  port: 19000
```

改完以后，联调入口会比较清晰：

- 论坛主站：`http://localhost:8080/community`
- 商城网关：`http://localhost:19000`
- 前端开发页：`http://localhost:5173`

## 关键配置位置

### 论坛主站配置

- `community-post-service/src/main/resources/application.properties`

重点包括：

- 主站端口和上下文路径
- `study` 数据库
- Redis
- Kafka
- Elasticsearch
- Quartz
- LLM 内容审核配置
- 七牛云
- wkhtmltoimage

### 论坛用户服务配置

- `community-user-service/src/main/resources/application.yml`

重点包括：

- `study` 数据库
- Redis
- Nacos
- 邮件服务
- `community.path.domain`

### 商城认证配置

- `auth-service/src/main/resources/application.yml`

重点包括：

- `ecommerce_auth` 数据库
- JWT Secret
- 论坛用户服务地址 `community.user-service.base-url`

### 商城网关配置

- `gateway-service/src/main/resources/application.yml`

重点包括：

- 网关端口
- 各服务路由
- JWT 校验
- Redis 限流
- 熔断配置

## 建议的本地联调顺序

如果你想尽量少踩坑，我建议按下面这个顺序来：

1. 先把 MySQL、Redis、Kafka、Nacos、RabbitMQ 起好
2. 单独跑通 `community-user-service`
3. 跑通论坛主站 `community-post-service`
4. 确认前端 `frontend` 能访问论坛接口
5. 跑通 `auth-service`
6. 再逐步补齐 `inventory-service`、`product-service`、`order-service`、`cart-service`、`support-service`
7. 最后处理商城网关端口，把论坛和商城同时挂起来

这个顺序的好处是，用户系统这条主线最先打通，后面其它业务服务即使还没全部准备好，也不会把问题搅成一团。

## 当前已知问题和部署边界

这部分不写漂亮话，直接说现状。

### 1. 根仓库已经聚合，但还不适合宣称“完全统一构建”

论坛侧和商城侧的 Spring Boot / Java 版本还不一致。论坛侧以 Boot 2.7 为主，商城侧以 Boot 3.3 为主。它们现在已经在一个仓库中，但并不代表已经完成统一父 POM、统一插件链和统一部署脚本治理。

### 2. 论坛库初始化脚本缺失

商城侧多数表可以靠 JPA 自动更新，论坛侧不行。部署论坛前你需要自己准备 `study` 库表结构。

### 3. 论坛网关模块还只是骨架

`community-gateway` 目前只有基础工程结构，没有像商城网关那样完整的路由配置，所以论坛当前真正的访问入口还是 `community-post-service`。

### 4. 配置中存在硬编码敏感信息

这点本地没问题，但不适合直接进生产，更不适合继续公开扩散。

### 5. 论坛和商城还没有统一成一个总网关入口

现在论坛和商城分别有自己的访问方式。论坛偏传统站点入口，商城偏 API 网关入口。真正做成一个统一门户，还需要继续做路由与前端整合。

## 适合拿这个项目做什么

这个仓库挺适合以下几类场景：

- 课程设计、毕业设计、实训项目
- 微服务拆分和服务协同演示
- 社区 + 商城融合方向的原型验证
- 用户系统整合、登录体系打通的练手项目
- 内容社区引流到交易闭环的业务探索

如果只是想找一个“开箱即用、一次启动、零配置”的项目，这个仓库现在还不是那个状态。但如果你想找一个真实地处在整合过程中的项目，并继续往工程化、统一网关、统一部署和统一前端推进，它反而很有改造价值。

## 后续建议

如果继续往下做，我最建议优先补这几件事：

1. 统一论坛和商城的端口规划，先解决 `8080` 冲突。
2. 整理论坛 `study` 库的初始化 SQL。
3. 把敏感配置迁移到环境变量或私有配置文件。
4. 统一根级构建链路，减少混合版本带来的维护成本。
5. 把论坛入口和商城入口收敛到一个统一网关或统一前端里。

做到这一步，这个项目就会从“整合中的仓库”变成“可以稳定交付和演示的平台”。
