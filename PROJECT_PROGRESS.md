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
15. **AI 面试模拟**（/interview）：选岗位 → 按 autoTag 分类从知识库抽 5 段知识点（LLM 统一改写成可直接作答的面试问句，失败降级原文）→ 逐题作答 AI 点评打分（RAG 参考要点对照）→ 总结报告（总分/分维度均值/题目明细 + canvas 自绘雷达图）；FREE 每日 2 次、VIP 不限次 + qwen-max 深度点评（4 维度）；会话 Caffeine 缓存 30 分钟；**完成即落库 interview_record，个人中心「我的面试」历史回看 + SVG 进步趋势折线 + 低分题（<60）一键加入错题本（幂等）**（`InterviewService`/`InterviewController`/`InterviewView.vue`/`InterviewRecordsView.vue`）
16. **积分商城**（/shop）：积分兑换出口（断点①修复）——简历模板/校招时间线/面试高频题 TOP50 资料（30/50/80 分）+ 7 天 VIP 体验卡（200 分）；原子扣分 + point_log 流水 + redeem_record 记录双写审计；VIP 卡兑换即开通，个人中心积分卡片入口（`ShopService`/`ShopController`/`ShopView.vue`）
17. **用户学习周报**（/weekly-report）：本周（周一起）聚合对话主题/签到/八股打卡/错题/简历评分/积分账本/成就，规则生成建议（零 LLM 成本）；个人中心入口条（`WeeklyReportService`/`GET /api/user/weekly-report`/`WeeklyReportView.vue`）
18. **运营看板**（管理后台 tab）：用户规模（总数/本周新增/VIP）、今日活跃（对话∪签到去重）、对话/签到/八股打卡、本周积分发放消耗、商城兑换统计、积分消耗去向 Top5、**面试模拟使用量（VIP 卖点）**、**转化漏斗（注册→首聊→首签→兑换→VIP 相对转化率）**（`AdminStatsService`/`GET /api/admin/stats`/`AdminView`「运营看板」tab）
19. **首页数据面板**：左侧学习仪表盘（积分渐变数字 + 签到 7 天 SVG 环形进度 + 连续签到/邀请/成就指标 + count-up 动效）+ 右侧动态天气面板（Open-Meteo 免费 API 后端代理，WMO 代码→动效，canvas 雨滴/雪花/闪电 + CSS 云层/雾，城市 chips 切换 + 浏览器定位）（`WeatherService`/`GET /api/weather`（公开）/`DashboardPanel`/`WeatherPanel`）
20. 其他：登录注册(JWT)、个人中心、管理后台(公告/反馈/用户/AI设置/错误日志)、意见反馈、限流
21. **模型切换 + 差异化计费**（Qoder 模式）：聊天页模型选择器（qwen-turbo/plus/max + deepseek-v3/r1，5 模型白名单），不同模型按**实际 token 消耗 × 模型费率（积分/千 token）**结算积分；调用前预检余额（≥1 分）、结束后按 usage 结算（usage 缺失按输出长度估算防白嫖）；VIP/ADMIN 免扣；FAQ/缓存命中不扣；非法模型名回落默认；`/api/models` 公开展示模型与费率；选择 localStorage 持久化（`ModelCatalog`/`PointService.precheckChat+settleChat`/`ChatInputBar` 模型下拉/`LoveMasterView`）
22. **邀请海报（分享裂变补全）**：个人中心邀请卡片新增「保存海报」——canvas 绘制品牌海报（品牌蓝渐变 + 装饰圆环 + 标题/宣传语 + 白底圆角二维码卡片 + 邀请链接），弹窗展示支持下载 PNG（600x850 适配手机长按保存），配合既有专属链接/复制/二维码形成完整分享链路（`UserCenterView.drawPoster`/`openPoster`/`downloadPoster`）
23. **忘记密码（找回密码）**：登录页新增「忘记密码」两步弹窗（发验证码 → 重置）；后端按账号（用户名或邮箱）查用户，**按注册渠道（register_channel: EMAIL/PHONE）分发验证码**——邮箱渠道复用 EmailCodeService（场景化邮件文案），手机号渠道预留（短信服务接入后启用）；验证码 5 分钟有效 + 60s 冷却 + IP 限流；app_user 新增 phone/register_channel 列（兼容 DDL）（`AuthService.forgotSendCode/forgotReset`/`EmailCodeService.sendCode(email,ip,scene)`/`ForgotSendCodeRequest`/`ForgotResetRequest`/`LoginView` 忘记密码弹窗）
24. **八股随机题改疑问句**：八股练习场「随机一题」抽题后经 LLM 改写为可直接作答的面试问句（失败降级原文）；弹窗题目高亮展示 + 「查看参考答案」折叠原文知识点；错题本存改写后的题目、AI 讲解结合题目+原文（`BaguService.randomQuestion/toQuestion`/`GET /api/bagu/random` 返回 question+content/`BaguView` 随机弹窗）
25. **八股题库扩充（小林 coding 风格）**：新增 36 条后端八股知识点（网络/OS/MySQL/Redis/Java/Spring 六大主题，行业公共知识自行组织语言，参考小林coding/JavaGuide 知识体系），知识库 629 → 665 段（`scripts/insert-bagu-knowledge.sql` 批量导入 knowledge 表 + rebuild 全量重建）
26. **AI 功能积分计费全覆盖（防爆刷 LLM）**：简历评分、职规报告补上积分校验（此前可被 0 分用户无限免费调用 LLM）；八股 AI 讲解因复用聊天接口（chatWithRag）天然继承预检+按 token 结算；PointService 抽公共逻辑新增 `precheckFeature`/`settleFeature`（场景化流水原因，如「AI 简历评分:qwen-plus」）；ResumeReviewService.review 加 username 参数（Controller 从 JWT 传）、generateReport 预检/结算（`PointService.precheckBalance/settleBalance`/`ResumeController`/`CareerMasterServiceImpl.generateReport`）
27. **新手引导任务（漏斗缺口修复）**：4 个新手任务（首次对话 +10/首次签到 +5/首次面试 +15/首次兑换 +10，共 40 分），完成状态从业务表实时判定（conversation/sign_in/interview_record/redeem_record），领取记录落 user_task 表（唯一约束幂等防刷）；个人中心「🎯 新手任务」卡片（领取按钮/已领取/未完成三态）+ 首页醒目引导——**首页主视觉区「🎯 新手任务 x/4」橙色进度条**（可点击展开内联领取，全部领取后自动隐藏）+ 顶栏积分徽章可领取红点（`GuideTaskService`/`GET /api/user/tasks`+`POST /api/user/tasks/{key}/claim`/`UserTask` 实体/`UserCenterView` 任务卡片/`HomePage` 进度条+红点）
28. **一键启动脚本**：`scripts/start-all.ps1` 一条命令拉起后端（JDK17）+前端（Vite）+cpolar 双隧道，跳过已运行服务（幂等），等待后端健康后自动打印本地地址与新隧道域名（日志正则提取，双隧道都出现才输出）；冷启动实测通过；配套 `stop-all.ps1` 一键停止
29. **简历评分长文本修复**：3000 字简历要输出「优化版完整简历」，默认 max_tokens 2048 被截断 → JSON 不完整 → 解析失败；修复：ChatOptions.maxTokens(8192) + JSON 解析容错（截断时截取到最后一个 } 保住评分维度）+ 前端评审中友好提示（loading 图标 + 时长说明）（`ResumeReviewService`/`ResumeReviewView`）

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
| 积分商城（兑换出口：资料 + VIP 体验卡，双写审计） | ✅ |
| 用户学习周报（本周聚合对话/签到/错题/积分 + 建议） | ✅ |
| 运营看板（用户/活跃/积分消耗统计，管理后台 tab） | ✅ |
| 首页数据面板（学习仪表盘 + 动态天气面板） | ✅ |
| 面试记录落库 + 历史回看（周报/看板面试维度） | ✅ |
| 面试进步趋势图 + 低分题一键进错题本（幂等） | ✅ |
| 运营看板转化漏斗（注册→首聊→首签→兑换→VIP） | ✅ |
| 骨架屏打磨（天气/面试记录/周报/商城加载占位） | ✅ |
| 首页充实（本周概览 + 快捷入口扩充 + 协议去重） | ✅ |
| 天气动效增强（太阳/云层/雨滴修复 + 晴天改天蓝） | ✅ |
| 模型切换 + 差异化计费（deepseek 试点：白名单 + 按 token × 费率结算 + 预检/结算 + 前端选择器） | ✅ |
| 启动跳过非空向量库 embedding（知识库重建 30-40s → 253ms，管理接口仍全量） | ✅ |
| 登录/注册页 CodePen 风格改造（SVG blob 背景 + 白卡片 + 下划线输入框 + 渐变胶囊按钮，仿 afgprogrammer mYQQJV，配色换站点商业蓝） | ✅ |
| Git 提交（安全审查通过，分模块 commit；push 已同步 GitHub） | ✅ |

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
- 面试模拟：`service/InterviewService`（抽题 drawQuestions 分类优先+全库兜底/题目 LLM 改写问句 toInterviewQuestions 降级原文/会话 Caffeine TTL 30min/点评 RAG 参考要点对照/VIP qwen-max 深度 4 维度/FREE 每日 2 次/完成即落库 interview_record+records/recordDetail）、`controller/InterviewController`（/api/interview/start|answer|report|quota|records|records/{id}）、`entity/InterviewRecord`、前端 `views/InterviewView.vue`+`InterviewRecordsView.vue`+路由 /interview|/interview-records
- 积分商城：`service/ShopService`（原子扣分 UPDATE...WHERE points>=cost 防超扣 + point_log/redeem_record 双写 + VIP_CARD 调 grantVip）、`controller/ShopController`（/api/shop/items|redeem|records）、`entity/RedeemItem`+`RedeemRecord`、表 `redeem_item`/`redeem_record`（schema.sql 内置 4 个初始商品）、前端 `views/ShopView.vue`+`api/shop.ts`+路由 /shop+个人中心入口
- 周报：`service/WeeklyReportService`（本周一 00:00 起聚合 conversation/sign_in/bagu_*/point_log/redeem_record/resume_review + 规则建议）、`GET /api/user/weekly-report`、前端 `views/WeeklyReportView.vue`+路由 /weekly-report+个人中心入口条
- 运营看板：`service/AdminStatsService`（用户/今日活跃 DISTINCT 去重/积分账本/兑换/消耗去向 Top5）、`GET /api/admin/stats`、前端 `AdminView.vue`「运营看板」tab
- 模型切换计费：`config/ModelCatalog`（5 模型白名单 + 费率 积分/千token + resolve 回落默认 + /api/models 公开）、`service/PointService.precheckChat`（余额≥1 预检）+`settleChat`（cost=max(1,ceil(tokens/1000)×费率)，余额不足扣到 0 保审计）、`CareerMasterServiceImpl`（chatWithRag/chatWithRagStream 接收 model + ChatOptions 运行时切模型 + settleChatQuietly 结算不中断主流程）、`ChatRequest.model`、前端 `ChatInputBar`（可选模型下拉+费率展示）、`LoveMasterView`（localStorage 记住选择）
- 首页面板：`service/WeatherService`（Open-Meteo 代理 + 内置 10 城经纬度 + WMO→动效映射）、`GET /api/weather`（SecurityConfig 白名单公开）、前端 `components/DashboardPanel.vue`（SVG 环形 + count-up）+ `components/WeatherPanel.vue`（canvas 雨雪/闪电粒子 + CSS 云层）+ `HomePage`「home__panels」布局（1fr+320px，<900px 单列）
- 登录/注册页：`components/AuthDecor.vue`（SVG 渐变 blob + 浮动代码符号）、`views/LoginView.vue`/`RegisterView.vue`（仿 afgprogrammer mYQQJV：浅紫蓝底 + 白卡片 20px 圆角 + 圆形渐变 logo + 无边框下划线输入框聚焦变主色 + 蓝青渐变胶囊按钮居中，全站商业蓝 #2f6bff→#17c3f8，含 theme-dark 适配）
- 知识库：`resources/rag/career-tips.txt`（629 段种子源，仅首次导入用）；`entity/Knowledge` + `mapper/KnowledgeMapper` + `service/KnowledgeService`（DB 事实源，在线增删改查 + 异步全量重建 pgvector/BM25/八股缓存）；表 `knowledge`；启动优化：`RagDocumentLoader.isVectorStoreEmpty()` 判断向量库非空则跳过 embedding 复用持久化向量（`rebuildIndexesSync(skipVectorIfExists=true)`），管理接口 rebuildAsync 仍强制全量（知识变更必须重建）；向量库为空（首次/被清空）才全量向量化，启动重建 253ms（原 30-40s）
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
- [x] 新手引导任务（漏斗最大流失点：注册→首聊掉 50%、首聊→首签掉 25%——完成首聊/首签/首面/首兑得积分，把新用户推入留存闭环）
- [ ] 移动端整体打磨（面板/漏斗/仪表盘已适配；聊天页与管理后台表格待过一遍）
- [ ] 管理后台与聊天页骨架屏（低优先级，用户可见度低）
- [ ] 公网部署 + 固定域名（商业化前提：Docker Compose 编排 MySQL+PG+后端 → 腾讯云部署 + 正式域名 + HTTPS，解决 cpolar 临时域名无法 SEO/稳定分享的问题）
- [ ] 游客受限试用（每日 3 次聊天体验 → 引导注册，补注册转化漏斗顶部；需防刷设计）
- [ ] SEO 内容落地页（665 段八股生成静态题目+答案页，长尾搜索流量获客，配合公网部署）

### P2 RAG 进阶 + 架构 + 商业化起步
- [x] 多轮历史融合检索、语义缓存
- [x] Prompt 结构优化 + HyDE 分流（简单问题标准 RAG，复杂问题 HyDE）
- [x] 知识库管理入口（管理后台在线增删改查知识段）
- [ ] docker-compose 编排 —— 【暂缓】待部署时一并实施
- [x] 商业化：积分/会员体系（签到积分 + VIP 分级限流 + 流水审计）
- [x] 商业化：分享裂变（邀请码 + 首聊奖励 + 二维码卡片）
- [x] 商业化：VIP 卖点（AI 面试模拟 FREE 每日 2 次 / VIP 不限次 + qwen-max 深度点评）
- [x] 商业化：积分商城（兑换出口：简历模板/时间线/高频题 TOP50/7 天 VIP 卡，原子扣分 + 双表审计）
- [x] 商业化：运营看板（管理后台用户活跃/留存/积分消耗统计 + 消耗去向 Top）
- [x] 商业化：用户学习周报（断点②：每周汇总对话主题/错题进步/连续签到/成就解锁，数据资产沉淀）
- [x] 商业化：模型切换 + 差异化计费（deepseek 试点：白名单 + 按 token × 积分费率结算，参考 Qoder）
- [ ] 运营看板补留存指标（DAU/WAU/MAU + 周留存，复用现有表判断产品健康度）
- [ ] 跨会话长期记忆/用户画像（记住目标岗位/简历背景/薄弱知识点，个性化对话——AI 助手差异化核心）
- [ ] 简历→薄弱点→推荐练习闭环（评分发现薄弱 → 推荐八股/面试 → 练后复评，打通孤立功能成教练闭环）
- [ ] 错题本遗忘曲线复习（艾宾浩斯 1/3/7/15 天提醒重练，从记录工具升级为学习系统）
- [ ] 学习成果分享卡（周报/成就/连续签到做成分享图，社交传播获客，复用邀请海报 canvas 能力）
- [ ] 点踩→知识库优化闭环（踩的回答 → 后台标注 → 补知识 → 重新评估，让知识库越用越准）

### P3 暂缓/锦上添花
- [ ] 商业化：小程序/移动端（等真实用户数据）
- [ ] PWA、邮件模板参数化、操作审计日志
- [ ] 幻觉率/端到端问答正确率评估
- [ ] 500 段后三层切片 + 三路 RRF（知识库已达 629，评估后启用）
- [ ] 埋点/增长分析
- [ ] 商业化：简历深度报告（VIP 专属，现有 6 维评分升级为逐段点评 + 改写对比）
- [ ] 商业化：知识点掌握度地图（八股按主题雷达图可视化）
- [ ] 商业化：支付接入（个人项目先 admin 手动开 VIP，等真实用户再上微信/支付宝）
- [ ] 商业化：模型费率运营化（ModelCatalog 静态配置 → DB 表可后台调价）+ 扩充百炼第三方模型（glm 需单独接智谱 Key）

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
- 模型计费验证：testuser01 曾兑换 7 天 VIP → 聊天不扣分属预期（VIP 免扣），验证 FREE 扣分需用 FREE 账号（klu/123456，0 分可直接验预检拦截）；`mvn spring-boot:run` 若报 RunMojo class version 61.0 需先设 `$env:JAVA_HOME=C:\Users\tan\.jdks\ms-17.0.16`（当前终端默认 Java 11）；沙箱环境 mysql.exe 写操作 Access denied，SELECT 可读，改测试账号状态优先走 admin API（/api/admin/points|vip）而非直连 DB
- 向量库启动重建：知识库 DB 事实源 + 启动全量重建曾导致每次重启都 TRUNCATE 后重新 embedding（30-40s + API 费）；向量库持久化在 pgvector 本就该复用 → 启动时 `isVectorStoreEmpty()` 非空则跳过 embedding 只重建内存索引（BM25/八股毫秒级），知识变更仍由管理接口全量重建保证一致性，重启验证日志「向量库非空，跳过 embedding」即生效
