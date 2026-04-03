# 技术计划书迭代记录

## V1 技术方案

### 目标

打造一个适合秋招展示的“Java 后端主导型”项目，兼顾业务复杂度、系统设计深度与 AI 特色。

### V1 技术选型

- 后端：Java 21、Spring Boot、MyBatis-Plus、MySQL、Redis
- 安全认证：JWT
- 搜索：Elasticsearch
- MQ：RabbitMQ
- AI：调用大模型 API 完成标签提取、摘要和每日启发
- 前端：Vue 3 + TypeScript + Element Plus
- 部署：Docker Compose

### V1 架构设计

- 单体应用优先，按领域拆分模块：
  - user
  - thought
  - match
  - follow
  - insight
  - notification
- 数据流：
  - 用户发念头
  - 后端调用 LLM 做标签和摘要
  - 写入 MySQL
  - 异步写入 ES
  - 计算相似度并触发通知

## 第 1 轮面试官锐评

### 锐评

1. 技术栈太“学生作业式堆料”，但没有解释为何这样选，缺乏取舍意识。
2. JWT 太泛，没有体现国内大厂项目常见的权限框架思路。
3. RabbitMQ 在国内 Java 求职语境里不如 RocketMQ 更能体现主流度。
4. 只说“调用 LLM API”，没有把 AI 能力设计成可替换的工程模块。
5. 没有区分 MVP 与生产增强版，风险边界不清晰。

### 修订动作

- 认证方案升级为 `Sa-Token + Token 持久化`，更贴近国内常见实践。
- MQ 改为 `RocketMQ`，突出异步削峰、通知解耦、任务编排。
- AI 层增加 `LLM Gateway` 与 `Prompt Template` 抽象。
- 增加 `MVP / Advanced / Interview Plus` 三层建设路线。
- 明确“先单体、后服务拆分”的演进路径。

## 第 2 轮面试官锐评

### 锐评

1. 还是偏“技术名词堆积”，缺少核心高并发链路和热点数据设计。
2. 相似匹配如果完全实时计算，复杂度高，数据量稍大就会失控。
3. Elasticsearch 能搜，但不能代表语义召回，你的“同频匹配”故事不够硬。
4. 没有风控和审核设计，社区类产品一定会被问到内容安全。
5. 缺少可观测性，面试时很难展开“线上问题怎么排查”。

### 修订动作

- 引入“粗排 + 精排”匹配链路：
  - 粗排：关键词/标签/主题桶召回
  - 精排：语义相似度 + 念度权重公式
- 增加内容审核流水线：
  - 敏感词
  - AI 审核标签
  - 人工复核预留位
- 增加缓存与热点设计：
  - Redis 缓存用户画像、热门匹配结果、通知计数
- 增加可观测性：
  - Spring Boot Actuator
  - Micrometer + Prometheus + Grafana
  - TraceId 日志

## 第 3 轮面试官锐评

### 锐评

1. 你说自己是 Java 后端项目，但如果一上来就微服务，面试官会怀疑是否真正做得动。
2. 没有讲数据库表设计与分库分表前的边界。
3. 缺少一致性策略，尤其是“发念头 -> 触发匹配 -> 发通知”这一链路。
4. 你说有 AI，但没有讲降级策略，一旦模型超时怎么办。
5. 没有测试策略，项目故事容易停在 PPT 层。

### 修订动作

- 最终定位为“大单体 + 明确领域边界”，后续按服务能力拆分。
- 一致性采用“事务 + Outbox/Event 表 + MQ 异步消费”的可演进方案。
- 增加 AI 降级：
  - 主模型超时后走规则模板
  - 每日启发支持缓存兜底
- 加入测试策略：
  - 单元测试覆盖匹配公式
  - 集成测试覆盖核心接口
  - 压测重点覆盖发布与匹配链路

## Final 技术计划书

### 1. 项目定位

- 形态：面向校招展示的高质量 Java 后端项目
- 路线：单体优先，领域清晰，保留拆分空间
- 核心亮点：社区业务、推荐匹配、异步架构、缓存设计、AI 集成

### 2. 最终技术栈

#### 后端核心

- Java 21
- Spring Boot 3.4.x
- Maven
- Spring Validation
- MapStruct
- Lombok

#### 数据层

- MySQL 8
- MyBatis-Plus
- Redis 7
- Elasticsearch 8

#### 中间件

- RocketMQ 5
- XXL-JOB 或 Spring Scheduler
- MinIO

#### 安全与接口

- Sa-Token
- OpenAPI / Swagger
- 接口幂等组件

#### AI 能力层

- Spring AI 或自定义 LLM Gateway
- Embedding 接口
- Prompt 模板管理
- 内容审核与文案生成双通道

#### 前端

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Element Plus
- ECharts

#### 运维与观测

- Docker Compose
- Nginx
- Prometheus
- Grafana
- ELK / Loki

### 3. 系统架构

```text
Web/App
  -> API Gateway / Nginx
  -> xs-bbs-app (大单体)
     -> user/follow
     -> thought
     -> match
     -> insight
     -> notification
     -> audit
  -> MySQL
  -> Redis
  -> RocketMQ
  -> Elasticsearch
  -> LLM Gateway
```

### 4. 核心领域模块

- `user`：用户、主页、画像、黑名单
- `thought`：念头发布、标签、摘要、可见性
- `match`：相似召回、精排、相似值更新、实名解锁
- `social`：关注、双向关系、主页访问权限
- `insight`：每日启发、推荐素材、历史推送
- `notification`：站内信、推送任务
- `audit`：内容审核、风险拦截、申诉预留

### 5. 关键链路设计

#### 5.1 发布念头链路

1. 参数校验
2. 敏感词初筛
3. 写入念头主表
4. 写入 outbox 事件表
5. 事务提交
6. 异步消费者处理：
   - LLM 提取标签/摘要
   - 构建召回索引
   - 计算相似用户
   - 生成通知

#### 5.2 相似匹配链路

1. 粗排召回：
   - 标签倒排
   - 热主题桶
   - 近 30 天活跃用户优先
2. 精排打分：
   - 文本语义相似度
   - 念度权重
   - 相似念头数量
3. 更新用户相似值
4. 判断匿名/实名挡位

### 6. 数据库设计

核心表：

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

设计原则：

- 核心查询均有联合索引。
- 先不分库分表，按单表千万级以下进行设计。
- 通过冷热分离、归档表、ES 检索承接历史查询压力。

### 7. 非功能设计

- 性能：发布接口 P99 < 200ms，匹配链路异步完成
- 可用性：AI 超时后规则兜底
- 一致性：本地事务 + Outbox + MQ 最终一致
- 安全性：审核、限流、黑名单、内容风控
- 可观测：慢 SQL、接口耗时、消费者积压、模型调用时长

### 8. 面试讲述重点

- 为什么先做大单体而不是微服务
- 为什么匹配链路要异步化
- 为什么“粗排 + 精排”比全量实时相似度更合理
- AI 接入如何做到可替换、可降级、可缓存
- 如果 DAU 上升，如何演进到服务化与向量召回

### 9. 开发阶段规划

#### 第一阶段

- 单体后端
- 念头发布/列表/相似匹配
- 用户相似值与匿名/实名规则
- 每日启发 mock

#### 第二阶段

- MySQL + Redis + MyBatis-Plus 落库
- Sa-Token 登录鉴权
- RocketMQ 异步通知
- ES 搜索

#### 第三阶段

- LLM Gateway
- Embedding 召回
- 审核系统
- 监控告警
- 压测与性能调优
