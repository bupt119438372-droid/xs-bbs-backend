# 新生论坛技术计划书

## 1. 建设目标

- 做成一个能支撑秋招面试深问的 Java 后端项目，而不是只停留在 CRUD。
- 让业务复杂度、工程化能力、AI 集成能力同时成立。
- 采用“当前能做出来的可运行方案 + 可继续演进的大厂式架构路线”。

## 2. 终版技术选型

### 后端

- Java 21
- Spring Boot 3.4.x
- Spring Validation
- Spring Actuator
- MyBatis-Plus
- MapStruct
- Lombok

### 存储与中间件

- MySQL 8
- Redis 7
- Elasticsearch 8
- RocketMQ 5
- MinIO

### 安全与接口

- Sa-Token
- OpenAPI / Swagger
- 幂等防重组件

### AI 能力层

- Spring AI 或自研 LLM Gateway
- Prompt 模板管理
- Embedding 接口
- 结构化 JSON 输出约束
- 规则模板降级兜底

### 前端

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Element Plus
- ECharts

### 运维与观测

- Docker Compose
- Nginx
- Prometheus + Grafana
- Loki / ELK

## 3. 架构原则

- 单体优先：先把核心业务链路做扎实，便于开发、调试和讲解。
- 领域拆分：即使在单体内，也按 `user / thought / match / social / insight / audit / notification` 分模块。
- 异步优先：匹配、推送、AI 文案、索引构建都不阻塞主发布链路。
- 可降级：AI 超时、MQ 堆积、ES 异常时，主流程仍可运行。

## 4. 核心链路设计

### 4.1 念头发布

1. 请求校验
2. 敏感词预审
3. 写入念头表
4. 写入 outbox_event
5. 事务提交
6. 异步消费事件：
   - 生成标签/摘要
   - 更新索引
   - 执行相似匹配
   - 推送通知

### 4.2 相似匹配

1. 粗排召回：
   - 标签召回
   - 主题桶召回
   - 活跃时间窗口过滤
2. 精排打分：
   - 文本语义相似度
   - 念度权重匹配
   - 历史相似念头数量
3. 更新用户相似值
4. 判断匿名或实名挡位

### 4.3 每日启发

1. 聚合近 7 天念头
2. 汇总主题标签与情绪画像
3. 调用 LLM 生成启发文案
4. 超时则降级为规则模板
5. 结果缓存，避免重复调用

## 5. 数据模型

核心表建议：

- `user_profile`
- `user_follow`
- `thought_post`
- `thought_tag`
- `thought_match_record`
- `user_similarity`
- `daily_insight`
- `notification_message`
- `audit_record`
- `outbox_event`

## 6. 一致性与稳定性

- 发布主链路使用本地事务保障。
- 异步扩散通过 Outbox + MQ 保证最终一致。
- 对 AI 调用增加超时、重试、熔断和缓存。
- 对通知和匹配任务增加幂等消费设计。
- Outbox 事件增加退避重试、分区键和死信状态。

## 7. 性能设计

- 发布接口目标：P99 小于 200ms
- 热门用户主页、消息计数、相似推荐结果进 Redis
- 复杂查询走 ES，核心事务走 MySQL
- 大对象内容与媒体资源分离存储

## 8. 风控与审核

- 敏感词命中直接拦截
- AI 审核结果打标签
- 预留人工复核状态位
- 用户拉黑、限流、举报全链路预留

## 9. 当前落地策略

### 当前代码阶段

- 先实现可运行的单体后端 demo
- 默认使用 MySQL 持久化，测试环境使用 H2 保证自动化测试可跑
- 已接入 Sa-Token 登录态，关键写接口由登录用户驱动
- 已接入 Spring Cache，默认本地 simple cache，预留 Redis profile
- 已落地 Outbox 事件表、relay 扫描器、事件分发器与失败退避逻辑
- 已落地 `LlmGateway + EmbeddingGateway`，运行时默认 `compatible`，配置示例切到智谱兼容接口，测试环境使用本地替身，上游异常时走结构化降级
- 已落地念头级 AI 画像表 `thought_ai_profile`，存储摘要、标签、审核结果和 embedding
- 已将 AI 接入每日启发、内容审核、公开流过滤、同频匹配
- 已增加管理员审核台，支持查看待复核内容并对 AI 审核结果做人工改判
- 已增加 `audit_record` 审核留痕表，记录 AI 初判和人工改判
- 已增加审核运营汇总指标，展示 AI 初判量、人工复核量、改判次数与今日决策量
- 已增加审核日志检索接口，支持按来源、操作人、结果、日期范围回溯审核链路
- 已增加审核日志分页与 CSV 导出能力，便于运营排查和离线复盘
- 已增加 Notification Center，具备未读计数、已读流转与消息投递抽象
- 已接入 RocketMQ producer / consumer 骨架，默认本地直投，`rocketmq` profile 下切到 “Outbox relay -> MQ listener -> Handler” 链路
- 已补充 Outbox 的分区键、有序投递、退避重试与死信出口

### 下一阶段

- 引入 Elasticsearch 或向量库，补足粗排召回与搜索
- 增加 Outbox 运营查询、人工重放与告警面板
- 增加审核中心与运营后台

## 10. 面试表达重点

- 为什么用单体起步，而不是一上来微服务
- 为什么匹配链路要异步化
- 为什么要用粗排 + 精排
- AI 为什么必须设计成可替换、可降级
- 如果 DAU 上来，系统如何平滑演进
