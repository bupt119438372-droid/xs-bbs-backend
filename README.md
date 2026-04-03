# 新生论坛 xs-bbs

一个面向秋招展示的 Java 后端项目，主题是“念头沉淀、同频匹配、关系建立、AI 启发”。

## 当前能力

- `Spring Boot 3.4 + Java 21`
- `MyBatis-Plus + MySQL`
- `Flyway` 管理 MySQL 表结构、索引与种子数据
- `Sa-Token` 登录注册与接口鉴权
- `Spring Cache` 缓存抽象，预留 Redis profile
- `Outbox` 事件表、投递 relay、重试退避与死信治理骨架
- `Notification Center` 站内通知与未读计数
- `Relation Center` 我关注的人、关注我的人、用户主页与关系视图
- 实名同频后可进入对方主页，双向关注后开放查看彼此全部公开念头
- `Thought AI Profile` 持久化存储摘要、标签、审核结果与 embedding 向量
- `LLM Gateway` 抽象，运行时默认直连兼容协议网关
- `Embedding Gateway` 抽象，匹配链路已升级为 `Embedding + 词法相似度` 混合打分
- `AI` 已落地到每日启发、标签提取、摘要生成、审核过滤、相似匹配
- `AI` 上游异常时自动降级为结构化兜底结果，避免发帖和启发接口直接 500
- `RocketMQ` 生产者 / 消费者链路，支持按 profile 切换
- `Outbox -> RocketMQ -> Handler` 的事件分发主链路
- `Outbox` 分区键、有序投递、失败回退与 `DEAD` 状态流转
- 新增独立前端工程：`Vue 3 + TypeScript + Vite + Pinia + Vue Router`

## 主要接口

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `POST /api/v1/thoughts`
- `GET /api/v1/thoughts/me/analysis`
- `GET /api/v1/thoughts/{thoughtId}/analysis`
- `GET /api/v1/thoughts/me`
- `GET /api/v1/matches/me`
- `GET /api/v1/audit/summary`
- `GET /api/v1/audit/thoughts`
- `GET /api/v1/audit/thoughts/{thoughtId}/records`
- `POST /api/v1/audit/thoughts/{thoughtId}/decision`
- `POST /api/v1/social/follow`
- `GET /api/v1/social/following/me`
- `GET /api/v1/social/followers/me`
- `GET /api/v1/users/{userId}/home`
- `GET /api/v1/insights/daily/me`
- `GET /api/v1/notifications/me`
- `GET /api/v1/notifications/me/unread-count`
- `POST /api/v1/notifications/{notificationId}/read`

## 本地启动

### 1. 默认开发模式

默认使用 MySQL 持久化存储，本地建议先保证 `3306` 上有可用 MySQL：

```bash
./scripts/run-backend.sh
```

如果只是把 `MySQL / Redis / RocketMQ` 放进 Docker，而 Java 应用仍然跑在宿主机，当前默认连接配置无需修改，继续通过宿主机映射端口访问即可。应用启动时会通过 Flyway 自动创建库内表结构、索引和种子数据。

启动后可直接访问：

- `http://127.0.0.1:8080/api/v1/users` 示例接口

默认 AI 模式是 `compatible`，当前示例默认指向智谱兼容接口；启动前需要准备可用的模型接口配置和 API Key。

### 1.1 启动独立前端工程

如果你希望以前后端分离方式本地联调，可在后端保持运行的前提下，再启动 `frontend`：

```bash
cd frontend
npm install
cd ..
./scripts/run-frontend.sh
```

启动后访问：

- `http://127.0.0.1:5173/` 独立前端开发地址

说明：

- 默认通过 Vite Proxy 把 `/api` 转发到 `VITE_API_BASE_URL`，未配置时使用 `http://127.0.0.1:8080`
- 后端已补充 `UTF-8` 编码强制和本地开发跨域配置，减少中文乱码和联调跨域问题
- Dashboard 已展示最近一次 AI 分析、我的念头摘要/标签/审核状态
- `linxi / 123456` 当前是演示管理员账号，可在前端看到审核台、审核时间线和审核日志筛选
- 关系中心当前遵循“实名解锁进主页、互相关注看全量公开念头”的产品规则
- 如需直连其他后端地址，可在启动前设置 `VITE_API_BASE_URL`
- 后端原静态页已移除，`http://127.0.0.1:8080/` 不再承载论坛前端
- 审核日志已经支持分页查看和 CSV 导出

生产构建：

```bash
cd frontend
npm run build
```

构建产物会输出到 `frontend/dist`

测试：

```bash
./mvnw test
```

### 2. 启动 MySQL + Redis + RocketMQ

```bash
docker compose -f docker-compose.local.yml up -d
```

### 3. 使用 MySQL + Redis profile 启动

```bash
SPRING_PROFILES_ACTIVE=mysql,redis ./scripts/run-backend.sh
```

如果需要回退到 H2 内存模式，可显式启用：

```bash
SPRING_PROFILES_ACTIVE=h2 ./scripts/run-backend.sh
```

### 4. 使用 RocketMQ profile

如果已经通过 `docker compose` 启动 NameServer / Broker，可开启：

```bash
SPRING_PROFILES_ACTIVE=rocketmq ./scripts/run-backend.sh
```

或组合：

```bash
SPRING_PROFILES_ACTIVE=mysql,redis,rocketmq ./scripts/run-backend.sh
```

如果 Java 应用也运行在 Docker 容器内，建议额外加上 `docker` profile，这样会自动改用容器服务名：

```bash
SPRING_PROFILES_ACTIVE=mysql,redis,rocketmq,docker ./scripts/run-backend.sh
```

RocketMQ 这边还需要注意 `brokerIP1`：

- 宿主机访问中间件时，保持默认 `XS_BBS_RMQ_BROKER_IP=127.0.0.1`
- 容器内访问中间件时，启动 Compose 前改成 `XS_BBS_RMQ_BROKER_IP=rocketmq-broker`

如需覆盖连接信息，可设置：

- `XS_BBS_DB_HOST`
- `XS_BBS_DB_PORT`
- `XS_BBS_DB_NAME`
- `XS_BBS_DB_USERNAME`
- `XS_BBS_DB_PASSWORD`
- `XS_BBS_REDIS_HOST`
- `XS_BBS_REDIS_PORT`
- `XS_BBS_REDIS_PASSWORD`
- `XS_BBS_REDIS_DATABASE`
- `XS_BBS_RMQ_NAME_SERVER`
- `XS_BBS_RMQ_PRODUCER_GROUP`
- `XS_BBS_RMQ_OUTBOX_CONSUMER_GROUP`
- `XS_BBS_RMQ_OUTBOX_DLQ_TOPIC`
- `XS_BBS_RMQ_CONSUMER_GROUP`
- `XS_BBS_RMQ_BROKER_IP`

## AI 配置

运行时默认使用兼容协议网关，当前示例默认切到智谱 AI：

```bash
XS_BBS_AI_MODE=compatible
XS_BBS_AI_PROVIDER=zhipu
XS_BBS_AI_BASE_URL=https://open.bigmodel.cn/api/paas/v4
XS_BBS_AI_API_KEY=
XS_BBS_AI_CHAT_PATH=/chat/completions
XS_BBS_AI_CHAT_MODEL=glm-4.7
XS_BBS_AI_EMBEDDING_PATH=/embeddings
XS_BBS_AI_EMBEDDING_MODEL=embedding-3
```

说明：

- 推荐直接设置 `XS_BBS_AI_API_KEY`，chat 和 embedding 会共用这一份密钥
- 默认会使用 `XS_BBS_AI_BASE_URL=https://open.bigmodel.cn/api/paas/v4`
- 如果你要切到别的兼容厂商，只改 `XS_BBS_AI_PROVIDER / XS_BBS_AI_BASE_URL / 模型名` 即可
- `chat` 接口用于结构化生成每日启发、念头摘要、标签和审核结果
- `embedding` 接口用于语义向量生成，参与同频匹配打分
- 为避免泄漏，API Key 不应该写进仓库文件，建议只通过环境变量注入
- 如果上游模型接口超时、限流或额度不足，系统会保留真实模型接入方式，同时返回结构化降级结果并把念头标记为 `REVIEW`
- 即使切到真实模型，AI 元数据也会统一落到 `thought_ai_profile` 表，便于追踪和复用
- 审核留痕会统一落到 `audit_record` 表，记录 AI 初判和人工改判
- 审核台汇总卡片会统计 AI 初判量、人工复核量、改判次数和今日决策量
- 审核日志接口支持按 `sourceType / operatorKeyword / currentStatus / thoughtId / dateFrom / dateTo` 检索
- 审核日志分页接口为 `GET /api/v1/audit/records/page`，导出接口为 `GET /api/v1/audit/records/export`
- 自动化测试环境使用 `app.ai.mode=test` 的本地替身，不会调用外部模型接口

## 外部环境文件

项目默认从仓库外的 [xs-bbs.local.env](/Users/woolcurls/Developer/projects/java/xs-bbs.local.env) 读取环境变量。

- 模板文件在 [xs-bbs.local.env.example](/Users/woolcurls/Developer/projects/java/xs-bbs/ops/env/xs-bbs.local.env.example)
- 后端启动脚本是 [run-backend.sh](/Users/woolcurls/Developer/projects/java/xs-bbs/scripts/run-backend.sh)
- 前端启动脚本是 [run-frontend.sh](/Users/woolcurls/Developer/projects/java/xs-bbs/scripts/run-frontend.sh)

默认规则：

- 不传额外参数时，脚本会读取 `/Users/woolcurls/Developer/projects/java/xs-bbs.local.env`
- 如果你想改路径，可以先设置 `XS_BBS_ENV_FILE=/你的/env文件路径`

## 演进路线

- 接入 Redis 真缓存与热点计数
- 为 Outbox 增加运营查询页和人工重放能力
- 接入向量库与召回粗排
- 增加审核复核台、多通道通知与 AI 运营看板
