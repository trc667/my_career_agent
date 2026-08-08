# 项目进度与简介（新对话上下文速览）

> 用途：新开会话时先读本文件，快速恢复项目上下文。完整 RAG 实验细节见 `RAG_OPTIMIZATION_SUMMARY.md`。

## 一、项目背景与简介

**项目缘起**：计算机专业学生求职竞争激烈，普遍面临「方向迷茫、学习无体系、简历不会写、八股记不住」四大痛点；市面 AI 助手回答泛泛、不贴合 CS 求职场景。本项目定位为「**计算机学生的一站式求职 AI 助手**」，用企业级 RAG 技术把 629 段高质量职规知识变成个性化回答，并提供练习/评分/记录等留存闭环。

**目标用户**：计算机专业在读学生、应届毕业生（校招/实习求职者）。

**核心价值**：
1. 回答可信：RAG 检索增强 + 【来源】标注，拒绝瞎编（无知识拒答）
2. 场景齐全：职规咨询 / 简历评分 / 八股练习 / 超级智能体跑任务
3. 留存闭环：错题本 + 每日打卡 + 聊天历史跨设备同步 + 签到积分
4. 工程可运维：自建错误监控 + 数据库备份 + 全站像素风 UI

**技术概览**：Spring Boot 后端 + Vue3 前端，核心卖点是**企业级 RAG 检索增强生成**（Recall@1 78%、MRR 0.85）。

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.4.5 / Spring AI 1.0.1 / JDK 17（`C:\Users\tan\.jdks\ms-17.0.16`） |
| LLM/Embedding | DashScope qwen-plus / text-embedding-v4(1024维) / qwen3-rerank |
| 向量库 | PostgreSQL 18 + pgvector 0.8.6（HNSW，双数据源：MySQL 业务 + PG 向量） |
| 稀疏检索 | Lucene 9.11（IK-analyzer ik_smart 中文分词）+ BM25Similarity |
| 本地缓存 | Caffeine（SemanticCache 语义问答 / ContextCompressor 会话摘要，容量+过期原生管理） |
| 前端 | Vue3 + TS + Vite + Element Plus（`ai-love-master-web`，端口 5175），商用落地风 UI（单主色/大圆角/柔和渐变/仪表盘卡片） |
| 存储 | MySQL（用户/公告/反馈/会话/消息/简历评分/知识库/积分流水/签到）、OSS（头像，阿里云 web-tlias-122） |

## 二、RAG 六流程（生产链路已全部接入）

```
① Query 预处理 → ② embedding → ③ 向量+BM25 多路召回(RRF k60) → ④ qwen3-rerank 精排 → ⑤ Prompt拼装 → ⑥ 生成+【来源】标注
```
- 核心组件：`HybridRetriever`（生产检索）、`Bm25Retriever`（IK 分词）、`Reranker`（qwen3-rerank）、`RagDocumentLoader`（入库/向量检索）
- 检索 query 决策链：多轮历史融合 → 复杂度分流 →（复杂）HyDE 假设文档（`CareerMasterServiceImpl.buildSearchQuery` + `ComplexityClassifier`）
- 入口：`CareerMasterServiceImpl.chatWithRag`（同步）/ `chatWithRagStream`（SSE 流式）

## 三、已上线功能

1. **AI 职规大师**（/career-master）：RAG 流式对话，回答带【来源】
2. **AI 超级智能体**（/super-agent）：ReAct 多步规划 + MCP 工具（高德/联网/PDF/文件）
3. **AI 八股练习场**（/bagu）：629 段知识库分类浏览 + 搜索 + 随机抽题 + AI 讲解（`BaguService`/`BaguController`/`BaguView.vue`）
4. **头像系统**：用户头像（`/api/user/avatar`）+ 管理员 AI 头像（`/api/admin/ai-avatar`，全局覆盖式）+ 公开配置（`/api/config/ai-avatar`），OSS 存储
5. **AI 简历评分**（/resume-review）：粘贴简历 → 结合 RAG 知识库 6 维度评分 + 亮点/不足 + 优化版简历，结果入库历史回看（`ResumeReviewService`/`ResumeController`/`ResumeReviewView`）
6. **聊天历史管理 + 停止生成**：会话/消息全量入库 MySQL，跨设备同步；职规大师与超级智能体流式可中断（`ConversationController`/`DbConversationMemoryStore`/`MemoryConfig`）
7. **用户协议/隐私政策**：/agreement 页面 + 注册强制勾选（前后端双重校验）+ 首页/登录页入口
8. **知识库管理入口**（管理后台「知识库管理」tab）：629 段知识在线增删改查 + 启停，变更后异步重建 pgvector/BM25/八股三处索引（`KnowledgeService`/`AdminController`/`AdminView.vue`）
9. **多轮历史融合检索 + 语义缓存**：检索 query 并入最近 1-2 轮历史问题（指代性问题召回提升）；相同独立问题 30 分钟内直接命中缓存答案（20ms/0 token）（`CareerMasterServiceImpl.buildRagQuery`/`SemanticCache`）
10. **Prompt 结构优化 + HyDE 分流**：回答策略分层（事实依据/来源标注/拒答区分）；复杂问题先 LLM 生成假设文档再检索，简单问题标准 RAG（`CareerMasterPrompt`/`ComplexityClassifier`）
11. **积分/会员体系**：每日签到（连续 7 天奖励）+ 聊天点赞积分 + 流水可审计 + **聊天消耗积分**（职规 1 分/次、超级智能体 2 分/次，VIP/ADMIN 免扣，FAQ/缓存命中不扣）+ 首页顶栏积分徽章/状态条快捷签到 + VIP 分级限流（游客 10/FREE 20/VIP 60 次/分）（`PointService`/`RateLimitByLevelFilter`/`HomePage`/`UserCenterView`）
12. **分享裂变**：注册页支持邀请码预填（/register?invite=xxx）+ 二维码邀请卡片（复制链接），被邀人完成首聊 → 邀请人 +50 积分（invite_reward 唯一键防刷）（`AuthServiceImpl`/`PointService.rewardInviterOnFirstChat`/`UserCenterView` 邀请卡片）
13. **商用落地风 UI**：全局主题 token 重构（单主色商业蓝 #2f6bff + 大圆角 16/24 + 柔和阴影 + 大面积留白渐变）；像素风全部移除（字体/硬阴影/装饰）；首页仪表盘化（数据统计卡片网格：积分/连续签到/会员/邀请 + 功能卡片 hover 上浮）（`global.css`/`pixel.css` 语义重写/`HomePage`）
14. **成就徽章 + 签到进度 + 动效**：9 枚成就（初次对话/常驻咨询/七天连胜/月度铁人/引荐/人气/社交/积分两档）实时数据判定；签到 7 天周期进度条（再签 N 天解锁 +10）；动效升级（聊天气泡弹入/积分 count-up/卡片 hover）（`AchievementService`/`GET /api/user/achievements`/`useCountUp`/`ChatMessageList`）
15. **AI 面试模拟**（/interview）：选岗位 → 按 autoTag 分类从知识库抽 5 段知识点（LLM 统一改写成可直接作答的面试问句，失败降级原文）→ 逐题作答 AI 点评打分（RAG 参考要点对照）→ 总结报告（总分/分维度均值/题目明细 + canvas 自绘雷达图）；FREE 每日 2 次、VIP 不限次 + qwen-max 深度点评（4 维度）；会话 Caffeine 缓存 30 分钟（`InterviewService`/`InterviewController`/`InterviewView.vue`）
16. 其他：登录注册(JWT)、个人中心、管理后台(公告/反馈/用户/AI设置/错误日志)、意见反馈、限流

## 四、本轮任务进度（已完成并验证）

| 事项 | 状态 |
|---|---|
| 知识库扩容 228→629 段（LLM 批量生成 24 主题 + 网络抓取） | ✅ |
| 向量+BM25 混合检索（RRF）+ 数据量趋势验证（增益 0.8%→6.1%） | ✅ |
| qwen3-rerank 精排接入（Recall@1 +13.5%） | ✅ |
| BM25 分词升级（CJK bigram → IK ik_smart，Hybrid R@5 +2.1%） | ✅ |
| 六流程接入生产 chatWithRag（端到端验证通过） | ✅ |
| 头像系统（OSS + 用户/AI 双头像 + 全局显示 + AI头像 key bug 修复） | ✅ |
| 八股练习场（浏览/搜索/抽题/AI讲解） | ✅ |
| 首页卡片 ToC 化文案 | ✅ |
| 聊天历史管理（会话/消息入库 MySQL + 跨设备同步） | ✅ |
| 流式输出停止生成（职规大师 + 超级智能体） | ✅ |
| 用户协议/隐私政策（注册勾选 + 页面 + 页脚入口） | ✅ |
| AI 简历评分（RAG 结合 6 维度评分 + 优化版简历 + 历史入库） | ✅ |
| 全站像素风排版改造（像素图标/字体/按钮/卡片/装饰/动效） | ✅ |
| 八股错题本 + 每日打卡 + 学习统计（/bagu-practice 独立页） | ✅ |
| 聊天问答反馈（👍/👎）+ 重新生成 + 复制（两个聊天页） | ✅ |
| 上下文压缩 + token 预算（历史超预算早期对话 LLM 摘要占位，会话缓存复用） | ✅ |
| FAQ 精确匹配拦截层（高频产品/账号问题直接命中，跳过 RAG+LLM，命中 <100ms/0 token） | ✅ |
| 知识库管理入口（管理后台在线增删改查知识段，DB 事实源 + 变更后异步重建三处索引） | ✅ |
| 多轮历史融合检索 + 语义缓存（指代性问题融合历史 query；相同问题 20ms/0token 命中） | ✅ |
| 缓存升级 Caffeine（语义/摘要缓存容量上限 + 过期原生管理，替代手写 ConcurrentHashMap 淘汰） | ✅ |
| Prompt 结构优化 + HyDE 分流（分层回答策略；复杂问题 HyDE 假设文档检索，失败降级） | ✅ |
| 积分/会员体系（签到积分 + 流水审计 + VIP 分级限流 + 个人中心卡片） | ✅ |
| 聊天消耗积分 + 首页积分组件（FREE 扣分/VIP 免扣/FAQ 缓存不扣 + 顶栏徽章/快捷签到） | ✅ |
| 分享裂变（注册邀请码绑定 + 首聊奖励 +50 + 二维码卡片，幂等防刷） | ✅ |
| 商用落地风 UI 改造（单主色/大圆角/柔和阴影/仪表盘首页，像素风移除） | ✅ |
| 成就徽章 + 签到 7 天进度 + 动效升级（气泡弹入/count-up） | ✅ |
| AI 面试模拟（抽题/点评/报告/次数限制 + 首页第 5 卡） | ✅ |
| Git 提交（安全审查通过，分 5 模块 commit；push 待本地执行） | ✅ |

## 五、量化成果（面试数据）

- 629 段知识库 + 328 QA 评估集（131 语义 + 70 术语 + 50 补盲区 + 77 第二批）
- Baseline → Hybrid → +Rerank：Recall@5 83.5%→89.6%→93.9%；Recall@1 60.6%→64.5%→78.0%；MRR 0.69→0.85
- 混合检索增益随数据量放大：228 段 +0.8% / 629 段 +6.1%
- BM25 互补性：39/328 查询仅 BM25 命中向量漏检（全为专有名词类）
- 知识库管理入口验证：新增→异步重建 33s→八股同步；启动重建 39s
- 语义缓存验证：相同问题第二次命中 20ms / 0 token（对比首次 5.2s / 1335 token）
- HyDE 分流验证（50 QA 子集）：全 HyDE 反而降 R@1 2pp → 不能无脑全量；分流 = 全标准效果 + 复杂子集 MRR 0.8125→0.8750（+0.0625），且仅 8% 复杂问题付 HyDE 成本 → 默认开启分流；全量可跑 `mvn test -Dtest=RagEvaluatorTest#compareHydeSplit`（调大 sampleSize）

## 六、关键文件清单

- RAG：`rag/HybridRetriever`、`Bm25Retriever`、`Reranker`、`KnowledgeBatchGenerator`、`QueryRewriter`、`RagDocumentLoader`、`rag/ComplexityClassifier`（规则版复杂度分流）
- 评估：`test/.../RagEvaluatorTest`（compareHybrid 三分词器对比 / compareRewrite HyDE 三路 / compareHydeSplit 分流对比）
- 头像：`service/OssStorageService`、`controller/AppConfigController`、前端 `ChatMessageList`/`UserCenterView`/`AdminView`/`authStore`
- 八股：`service/BaguService`、`controller/BaguController`、`views/BaguView`
- 简历：`service/ResumeReviewService`、`controller/ResumeController`、`dto/ResumeReview*`、前端 `views/ResumeReviewView`、`api/resume.ts`、表 `resume_review`
- 聊天历史：`controller/ConversationController`、`memory/DbConversationMemoryStore`、`config/MemoryConfig`、前端 `store/loveMasterStore`、`api/conversation.ts`、表 `conversation`/`conversation_message`
- 上下文压缩：`memory/ContextCompressor`（token 预算裁剪 + LLM 摘要压缩 + Caffeine 会话缓存）、`CareerMasterServiceImpl.buildPromptMessages`、配置 `app.memory.history-token-budget`/`enable-summary`/`summary-cache-*`
- FAQ 拦截：`service/FaqService`（12 条 FAQ 库 + 归一化 + 精确/包含/相似度三级匹配）、`CareerMasterServiceImpl.tryFaq`（职规大师与超级智能体四个入口均接入）
- 多轮融合检索：`CareerMasterServiceImpl.buildRagQuery`（最近 1-2 轮用户问题并入检索 query，截断 60 字）
- 语义缓存：`service/SemanticCache`（Caffeine：容量 200 + 写入 30min 过期 + recordStats，仅新会话首轮生效）、配置 `app.semantic-cache.*`
- HyDE 分流：`CareerMasterServiceImpl.buildSearchQuery`（融合→复杂度→HyDE→降级）、`rag/ComplexityClassifier`、`QueryRewriter.hydeRewrite`、配置 `app.rag.hyde.*`
- Prompt：`config/CareerMasterPrompt`（分层回答策略：事实依据/来源标注/拒答区分/篇幅结构）
- 积分/会员：`service/PointService`（签到幂等/连续天数/流水审计/VIP 懒回落）、`security/RateLimitByLevelFilter`（游客10/FREE20/VIP60/ADMIN不限）、`entity/PointLog`+`entity/SignIn`+Mapper、接口 `/api/user/points`/`/api/user/sign-in`/`/api/admin/points`/`/api/admin/vip`、前端 `UserCenterView` 积分卡片
- 协议：前端 `views/AgreementView`、路由 `/agreement`
- 像素风：`styles/pixel.css`（已语义重写为商用工具类，类名保留组件零改动）、`components/PixelIcon.vue`（pixelarticons 线性图标保留）
- 商用风：`styles/global.css`（设计 token：主色 #2f6bff/大圆角/柔和阴影/Element Plus 主色覆盖）、`HomePage` 仪表盘（数据统计卡片网格）
- 成就/动效：`service/AchievementService`（成就规则+实时数据判定，9 枚）、`GET /api/user/achievements`、前端 `composables/useCountUp`、`ChatMessageList` 气泡动画、个人中心/首页签到 7 天进度条
- 面试模拟：`service/InterviewService`（抽题 drawQuestions 分类优先+全库兜底/题目 LLM 改写问句 toInterviewQuestions 降级原文/会话 Caffeine TTL 30min/点评 RAG 参考要点对照/VIP qwen-max 深度 4 维度/FREE 每日 2 次）、`controller/InterviewController`（/api/interview/start|answer|report|quota）、`dto/InterviewStartRequest`+`InterviewAnswerRequest`、前端 `views/InterviewView.vue`+`api/interview.ts`+路由 /interview+首页第 5 卡
- 知识库：`resources/rag/career-tips.txt`（629 段种子源，仅首次导入用）；`entity/Knowledge` + `mapper/KnowledgeMapper` + `service/KnowledgeService`（DB 事实源，在线增删改查 + 异步全量重建 pgvector/BM25/八股缓存）；表 `knowledge`
- 知识库管理接口：`controller/AdminController`（/api/admin/knowledge*）+ 前端 `AdminView.vue`「知识库管理」tab + `api/admin.ts`
- 配置：`application.yml`（公共）、`application-dev.yml`（敏感，不入库）、`application-raggen.yml`（批量生成）

## 七、运行环境与启动

```powershell
$env:JAVA_HOME="C:\Users\tan\.jdks\ms-17.0.16"; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"
# 后端（MCP 偶发初始化超时，可加 --spring.ai.mcp.client.enabled=false）
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=dev"
# 前端
cd ai-love-master-web; npm run dev   # 5175，对接 8080
```
- 测试账号：testuser01 / test123456（id=1）；admin 账号 demo / 123456（id=2，已提升 ADMIN）
- 知识库扩容：`mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=raggen"`（topics 在 application-raggen.yml）
- 管理后台：登录 demo → 访问 /admin（或登录页进入），「知识库管理」tab 在线维护知识段
- RAG 评估：`mvn test -Dtest=RagEvaluatorTest`（compareHydeSplit 默认 50 QA 子集约 5 分钟，全量 328 需调大 sampleSize）

## 八、待办（按 P 级排序）

### P0 上线前必备（生产可运维/数据安全）
- [x] 错误监控与日志告警（自建 error_log 上报 + 前端全局捕获 + 管理后台面板）
- [x] 数据库备份策略（backup-db.ps1：MySQL/PG 压缩导出 + 保留7份 + 计划任务）

### P1 留存核心 + 用户体验 + 工程质量
- [x] 八股练习场错题本 + 学习进度/打卡
- [x] 聊天内问答反馈（点赞/点踩）+ 重新生成
- [x] 上下文压缩/token 预算
- [x] FAQ 精确匹配拦截层
- [ ] 自动化测试 + CI/CD + 接口文档 —— 【暂缓】单人开发，此类工程化工具收益低，待有团队协作需求再做

### P2 RAG 进阶 + 架构 + 商业化起步
- [x] 多轮历史融合检索、语义缓存
- [x] Prompt 结构优化 + HyDE 分流（简单问题标准 RAG，复杂问题 HyDE）
- [x] 知识库管理入口（管理后台在线增删改查知识段）
- [ ] docker-compose 编排 —— 【暂缓】待部署时一并实施
- [x] 商业化：积分/会员体系（签到积分 + VIP 分级限流 + 流水审计）
- [x] 商业化：分享裂变（邀请码 + 首聊奖励 + 二维码卡片）
- [x] 商业化：VIP 卖点（AI 面试模拟 FREE 每日 2 次 / VIP 不限次 + qwen-max 深度点评）
- [ ] 商业化：积分商城（断点①：积分只有消耗口无兑换出口）—— 积分兑换简历模板/学习资料/7 天 VIP 体验卡/头像框，不涉支付纯内部闭环
- [ ] 商业化：运营看板（断点③：管理后台用户活跃/留存/积分消耗统计，当前只有错误监控）
- [ ] 商业化：用户学习周报（断点②：每周汇总对话主题/错题进步/连续签到/成就解锁，数据资产沉淀）

### P3 暂缓/锦上添花
- [ ] 商业化：小程序/移动端（等真实用户数据）
- [ ] PWA、邮件模板参数化、操作审计日志
- [ ] 幻觉率/端到端问答正确率评估
- [ ] 500 段后三层切片 + 三路 RRF（知识库已达 629，评估后启用）
- [ ] 埋点/增长分析
- [ ] 商业化：简历深度报告（VIP 专属，现有 6 维评分升级为逐段点评 + 改写对比）
- [ ] 商业化：知识点掌握度地图（八股按主题雷达图可视化）
- [ ] 商业化：支付接入（个人项目先 admin 手动开 VIP，等真实用户再上微信/支付宝）

## 九、踩坑备忘（本会话）

- OSS `SignatureDoesNotMatch` = AK/Secret 错（Secret 必须 30 位随机串，控制台"显示"复制）
- OSS AI 头像 key 双扩展名 bug：固定 key 上传不能走通用 upload（已修复）
- Spring AI MCP 初始化 180s 超时 → 启动参数禁用
- SearchReplace 偶发"partial success/save failed"可能是**真实回滚**：必须用 Grep/javap 验证关键代码是否真写入（AdminController 曾残留旧 class 导致新接口 404；对持续失败文件改用 Write 完整覆盖）；还可能**重复写入**（部分成功时方法插入两次，需 Grep 发现并清理重复）
- 8080 端口被旧实例占用 → `Get-NetTCPConnection` 在该环境不可靠，用 `Get-CimInstance` 定位 java 进程 + `Stop-Process -Force -ErrorAction Stop` 并复核（注意：命令取多个 PID 时可能只停第一个，需逐一核对；CreationDate 时间过滤可能失效，直接按 PID 操作最稳）
- 简历评分同步接口实际耗时 30-40s > 前端 axios 全局超时 20s → 评分接口单独设 `timeout: 90000`
- Vite 端口被占自动漂移（5175→5176）时后端 CORS 白名单需含该端口
- 前端 `api/chatStream.ts` 用 fetch+ReadableStream，停止生成靠 AbortController，后端 `isClientDisconnected` 已兼容断连
- 上下文压缩端到端验证：日志含「上下文预算裁剪/已生成对话摘要」即触发成功（中文日志在部分终端乱码属编码显示问题，不影响功能）
- 知识库变更后异步重建约 30-40s（629 段 embedding），期间检索用旧索引，前端轮询 rebuild-status 感知完成
- 语义缓存验证：日志出现「语义缓存命中」即生效；仅新会话首轮（无历史上下文）才缓存/命中，多轮对话不缓存防错配
- 缓存规范：业务缓存统一用 Caffeine（容量上限 + 过期原生管理），禁止裸 ConcurrentHashMap 手写 TTL/淘汰；Redis 仅在多实例共享/跨重启持久/跨实例会话摘要时引入
- HyDE 假设文档含 `>`/`/`/`（` 等特殊字符会导致 Lucene QueryParser Lexical error → BM25 检索前必须 `QueryParser.escape(query)`（已修复）
- HyDE 分流验证：日志「HyDE 分流生效：query N 字 → HyDE M 字」即触发；默认开启，量化增益后可按数据调 `app.rag.hyde.enabled`
- 积分/会员验证：签到幂等（同日重复 400）；管理员发分写流水可审计；限流日志「分级限流触发」；PowerShell 测试脚本中已 JSON 字符串勿再 `ConvertTo-Json`（会二次转义导致 400）
- 聊天扣分验证：FREE 对话 50→49 扣 1 分；FAQ/语义缓存命中不扣（没花 LLM 钱）；VIP/ADMIN 免扣；扣分用原子 SQL（UPDATE ... WHERE points >= cost）防并发超扣
- 分享裂变验证：被邀人首轮对话（历史=user+assistant 两条）触发邀请人 +50，invite_reward 唯一键防重复奖励；沙箱无法收注册邮件，验证用 SQL 直接绑 inviter_id
- AI 面试岗位映射脱节：`POSITION_CATEGORY` 映射"测试/运维"但知识库 autoTag 无此类（实际 8 类：后端/前端/算法/面试/综合/校招流程/软技能/实习）→ 按分类过滤空池报"该岗位知识不足"；修复为抽题分类优先 + 全库随机兜底；凡依赖 autoTag 分类筛选的功能先查 knowledge 表 category 分布
- PowerShell 5.1 无 BOM 的 UTF-8 脚本按 GBK 误读 → 中文参数（如岗位名）静默变乱码不报错；脚本内 JSON body 中文一律用 \uXXXX 转义；业务失败可能封装在 HTTP 200 + {code:400}（Result.fail），测试脚本必须检查响应 code 而非仅看 HTTP 状态码
- 沙箱 Stop-Process 会静默失败：重启后端必须复核 8080 归属（Get-CimInstance + 直接 PID 停止），否则新代码不生效且旧进程继续占用（曾致新接口 404 与 "Port 8080 was already in use"）；Get-NetTCPConnection 在该环境不可靠可能返回空，以 netstat/Get-CimInstance 为准
- 成就验证：数据实时判定（对话次数按 point_log「AI 对话消耗」计数、累计积分按正数流水求和），规则改代码即生效不落表
