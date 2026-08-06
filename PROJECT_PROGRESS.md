# 项目进度与简介（新对话上下文速览）

> 用途：新开会话时先读本文件，快速恢复项目上下文。完整 RAG 实验细节见 `RAG_OPTIMIZATION_SUMMARY.md`。

## 一、项目简介

**AI 计算机学生职规大师智能体**：Spring Boot 后端 + Vue3 前端的 AI 对话应用，核心卖点是**企业级 RAG 检索增强生成**。

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.4.5 / Spring AI 1.0.1 / JDK 17（`C:\Users\tan\.jdks\ms-17.0.16`） |
| LLM/Embedding | DashScope qwen-plus / text-embedding-v4(1024维) / qwen3-rerank |
| 向量库 | PostgreSQL 18 + pgvector 0.8.6（HNSW，双数据源：MySQL 业务 + PG 向量） |
| 稀疏检索 | Lucene 9.11（IK-analyzer ik_smart 中文分词）+ BM25Similarity |
| 前端 | Vue3 + TS + Vite + Element Plus（`ai-love-master-web`，端口 5175） |
| 存储 | MySQL（用户/公告/反馈/会话/消息/简历评分）、OSS（头像，阿里云 web-tlias-122） |

## 二、RAG 六流程（生产链路已全部接入）

```
① Query 预处理 → ② embedding → ③ 向量+BM25 多路召回(RRF k60) → ④ qwen3-rerank 精排 → ⑤ Prompt拼装 → ⑥ 生成+【来源】标注
```
- 核心组件：`HybridRetriever`（生产检索）、`Bm25Retriever`（IK 分词）、`Reranker`（qwen3-rerank）、`RagDocumentLoader`（入库/向量检索）
- 入口：`CareerMasterServiceImpl.chatWithRag`（同步）/ `chatWithRagStream`（SSE 流式）

## 三、已上线功能

1. **AI 职规大师**（/career-master）：RAG 流式对话，回答带【来源】
2. **AI 超级智能体**（/super-agent）：ReAct 多步规划 + MCP 工具（高德/联网/PDF/文件）
3. **AI 八股练习场**（/bagu）：629 段知识库分类浏览 + 搜索 + 随机抽题 + AI 讲解（`BaguService`/`BaguController`/`BaguView.vue`）
4. **头像系统**：用户头像（`/api/user/avatar`）+ 管理员 AI 头像（`/api/admin/ai-avatar`，全局覆盖式）+ 公开配置（`/api/config/ai-avatar`），OSS 存储
5. **AI 简历评分**（/resume-review）：粘贴简历 → 结合 RAG 知识库 6 维度评分 + 亮点/不足 + 优化版简历，结果入库历史回看（`ResumeReviewService`/`ResumeController`/`ResumeReviewView`）
6. **聊天历史管理 + 停止生成**：会话/消息全量入库 MySQL，跨设备同步；职规大师与超级智能体流式可中断（`ConversationController`/`DbConversationMemoryStore`/`MemoryConfig`）
7. **用户协议/隐私政策**：/agreement 页面 + 注册强制勾选（前后端双重校验）+ 首页/登录页入口
8. 其他：登录注册(JWT)、个人中心、管理后台(公告/反馈/用户/AI设置)、意见反馈、限流

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
| Git 提交（安全审查通过，分模块推送 GitHub） | ✅ |

## 五、量化成果（面试数据）

- 629 段知识库 + 328 QA 评估集（131 语义 + 70 术语 + 50 补盲区 + 77 第二批）
- Baseline → Hybrid → +Rerank：Recall@5 83.5%→89.6%→93.9%；Recall@1 60.6%→64.5%→78.0%；MRR 0.69→0.85
- 混合检索增益随数据量放大：228 段 +0.8% / 629 段 +6.1%
- BM25 互补性：39/328 查询仅 BM25 命中向量漏检（全为专有名词类）

## 六、关键文件清单

- RAG：`rag/HybridRetriever`、`Bm25Retriever`、`Reranker`、`KnowledgeBatchGenerator`、`QueryRewriter`、`RagDocumentLoader`
- 评估：`test/.../RagEvaluatorTest`（compareHybrid 三分词器对比）
- 头像：`service/OssStorageService`、`controller/AppConfigController`、前端 `ChatMessageList`/`UserCenterView`/`AdminView`/`authStore`
- 八股：`service/BaguService`、`controller/BaguController`、`views/BaguView`
- 简历：`service/ResumeReviewService`、`controller/ResumeController`、`dto/ResumeReview*`、前端 `views/ResumeReviewView`、`api/resume.ts`、表 `resume_review`
- 聊天历史：`controller/ConversationController`、`memory/DbConversationMemoryStore`、`config/MemoryConfig`、前端 `store/loveMasterStore`、`api/conversation.ts`、表 `conversation`/`conversation_message`
- 协议：前端 `views/AgreementView`、路由 `/agreement`
- 像素风：`styles/pixel.css`（工具类）、`components/PixelIcon.vue`（pixelarticons 像素图标）、依赖 `pixelarticons`/`@fontsource/press-start-2p`
- 知识库：`resources/rag/career-tips.txt`（629 段，唯一事实源）
- 配置：`application.yml`（公共）、`application-dev.yml`（敏感，不入库）、`application-raggen.yml`（批量生成）

## 七、运行环境与启动

```powershell
$env:JAVA_HOME="C:\Users\tan\.jdks\ms-17.0.16"; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"
# 后端（MCP 偶发初始化超时，可加 --spring.ai.mcp.client.enabled=false）
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=dev"
# 前端
cd ai-love-master-web; npm run dev   # 5175，对接 8080
```
- 测试账号：testuser01 / test123456；admin 账号 demo 需已在库（提升 ADMIN）
- 知识库扩容：`mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=raggen"`（topics 在 application-raggen.yml）

## 八、待办（按优先级）

- [x] Git 提交本轮全部改动（安全审查：AK/Secret 不入库，已分模块推送）
- [ ] 八股练习场错题本 + 学习进度/打卡（P1）
- [ ] 聊天内问答反馈（点赞/点踩）+ 重新生成（P1）
- [ ] 上下文压缩/token 预算（P1）
- [ ] FAQ 精确匹配拦截层（P1）
- [ ] 多轮历史融合检索（P2）、语义缓存（P2）
- [ ] 幻觉率/端到端问答正确率评估（P3，暂缓中）
- [ ] 500 段后三层切片 + 三路 RRF（知识库已达 629，可评估启用）

## 九、踩坑备忘（本会话）

- OSS `SignatureDoesNotMatch` = AK/Secret 错（Secret 必须 30 位随机串，控制台"显示"复制）
- OSS AI 头像 key 双扩展名 bug：固定 key 上传不能走通用 upload（已修复）
- Spring AI MCP 初始化 180s 超时 → 启动参数禁用
- SearchReplace 偶发"partial success/save failed"误报 → 用 Grep/Read 验证实际写入
- 8080 端口被旧实例占用 → `Get-NetTCPConnection` 查 PID + Stop-Process
- 简历评分同步接口实际耗时 30-40s > 前端 axios 全局超时 20s → 评分接口单独设 `timeout: 90000`
- Vite 端口被占自动漂移（5175→5176）时后端 CORS 白名单需含该端口
- 前端 `api/chatStream.ts` 用 fetch+ReadableStream，停止生成靠 AbortController，后端 `isClientDisconnected` 已兼容断连
