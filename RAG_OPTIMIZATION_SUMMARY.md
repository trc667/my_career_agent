# RAG 检索系统优化全记录

> 项目：AI 计算机学生职规大师智能体  
> 日期：2026-08-04  
> 知识库：629 段（750 条 chunk） | 评估集：328 QA 对 | 向量库：PostgreSQL pgvector (HNSW) + Lucene BM25

---

## 一、代码改动清单

### 1.1 架构升级（pgvector 迁移）

| 文件 | 改动 |
|---|---|
| `pom.xml` | 加 `spring-ai-pgvector-store` + PostgreSQL 驱动 |
| `config/DataSourceConfig.java`（新增） | 双数据源：MySQL（@Primary 业务数据）+ PostgreSQL（向量库） |
| `config/RagConfig.java` | `SimpleVectorStore` → `PgVectorStore`（`new JdbcTemplate(vectorDataSource)` 包装 + `dimensions(1024)` + `initializeSchema(true)`） |
| `rag/RagInitRunner.java` | `@Profile("dev")` → `@Profile({"dev","prod"})`，修复生产环境 RAG 不加载 |
| `application.yml` | 新增 `app.vector-datasource.*` 配置；切分参数 `500→250` |
| `application-dev.yml` | PG 密码经环境变量注入（不入库，见 .gitignore） |

### 1.2 检索优化

| 文件 | 改动 |
|---|---|
| `rag/RagDocumentLoader.java` | ① 入库前 `TRUNCATE`（防重复堆积）② 分批写入 `batch=8`（DashScope API 单次上限 **10 条**）③ 长段按句号二级切分（`splitLongParagraph`，300 字阈值，`(?<=[。；！？])` 零宽后行断言）④ 自动打标签 `autoTag`（关键词映射 → metadata JSON 存 `category`） |
| `rag/QueryRewriter.java`（新增） | ① 规则版 Query 改写（SPOKEN_PATTERN 去口语词 + DOMAIN_MAP 领域词映射）② HyDE 策略（`hydeRewrite`：LLM 生成假设答案 → 用答案做检索） |
| `rag/Bm25Retriever.java`（新增） | Lucene 内存索引（**IK-analyzer ik_smart 词语分词** + BM25Similarity）稀疏检索，与 pgvector 稠密检索做加权 RRF 融合 |
| `rag/KnowledgeBatchGenerator.java`（新增） | LLM 批量生成知识段：主题清单 → qwen-plus → JSON 解析 → 落盘（`raggen` profile 触发） |
| `pom.xml` | 加 `lucene-core` / `lucene-analysis-common` / `lucene-queryparser` 9.11.1 |

### 1.3 Prompt 优化

| 文件 | 改动 |
|---|---|
| `config/CareerMasterPrompt.java` | 新增约束：「仅依据知识库回答」「无知识拒答」「回答末尾标注来源」 |

### 1.4 评估体系

| 文件 | 改动 |
|---|---|
| `rag/RagEvaluatorTest.java`（新增） | 131 QA 对 + `evaluateRecall()`（baseline）+ `compareRewrite()`（三路对比）+ `compareOptimized()`（A/B 视图）+ `rrfFuse()`（RRF 融合）+ `matchesAny` / `computeReciprocalRank`（命中判断） |

---

## 二、知识库规模变化

| 阶段 | 段落数 | chunk 数 | 评估集 QA 数 |
|---|---|---|---|
| 初始 | 10 段 | 10 条 | — |
| 第一次扩容 | 30 段 | 31 条 | 64 对（原文关键词版） |
| 企业级基线 | 50 段 | 49 条 | 101 对（同义词版） |
| 最终 | 204 段 | 202 条 | 131 对 |
| LLM 批量生成扩容 | 228 段 | 237 条 | 131 对 |
| 评估集扩容（+70 术语类 +50 语义类） | 228 段 | 237 条 | **251 对** |
| LLM 批量生成 24 主题 + 网络抓取 | **629 段** | **750 条** | **328 对** |

---

## 三、优化实验数据面板

### 3.1 Recall 全览

| 规模 | 策略 | Recall@1 | Recall@3 | Recall@5 | MRR | 结论 |
|---|---|---|---|---|---|---|
| 50 段(49 条) | baseline | 68.3% | 89.1% | 92.1% | 0.78 | 数据小，虚高 |
| 137 段(135 条) | baseline | 63.4% | 79.2% | 86.1% | 0.72 | 回归真实 |
| 171 段(171 条) | baseline | 62.4% | 79.2% | 86.1% | 0.71 | — |
| 171 段 | **HyDE-only** | 60.4% | **84.2%** | 86.1% | 0.72 | ✅ **+5%** |
| 204 段(202 条) | baseline | 70.2% | 84.0% | 87.8% | 0.77 | 公平评估 |
| 204 段 | HyDE-only | 68.7% | 82.4% | 89.3% | 0.77 | — |
| 204 段 | HyDE+RRF | 70.2% | 83.2% | 88.5% | 0.78 | — |
| 228 段(237 条) | baseline | 70.2% | 84.7% | 88.5% | 0.77 | 扩容后基线 |
| 228 段(251 QA) | baseline | 68.5% | 85.3% | 89.2% | 0.77 | 新评估集基线 |
| 228 段(251 QA) | Hybrid(k60 等权) | 67.7% | 83.7% | **93.2%** | 0.77 | ✅ Recall@5 **+4.0%** |
| 629 段(328 QA) | baseline | 60.6% | 76.8% | 83.5% | 0.69 | 大库基线回归真实 |
| 629 段(328 QA) | Hybrid(k60 等权) | 64.5% | 85.0% | 89.6% | 0.75 | ✅ 全指标反超 +6.1% |
| 629 段(328 QA) | Hybrid + qwen3-rerank 精排 | **78.0%** | **92.4%** | **93.9%** | **0.85** | ✅✅ 精排全面飞跃 R@1 +13.5% |

### 3.2 关键发现

| 实验 | 结果 | 原因分析 |
|---|---|---|
| 规则版 Query 改写 | Recall@1 **-4%** | embedding-v4 对口语查询已足够鲁棒，过度清洗损失语义 |
| HyDE（171 段） | Recall@3 **+5%**（79.2→84.2） | 大知识库下假设文档语义匹配生效 |
| autoTag 类别硬过滤 | Recall@3 **暴跌 37%** | 关键词分类不准，filterExpression 直接丢弃正确文档 |
| 切分参数 500→250 | 持平 | 段落长度多在 250 token 内，二次切分无显著影响 |
| 相似度阈值 0.5 | 持平/略降 | 202 条数据量下阈值筛选效果有限 |
| 向量+BM25 混合检索 | Recall@5 **+1.6%**（88.5→90.1），Recall@1 回落 4.6% | 稀疏+稠密互补：7/131 query 仅 BM25 命中向量漏检；BM25 噪音拉低首位排序，降权 w2=0.3 后打底指标最优 |
| 评估集扩容对优化的影响 | 术语类 QA 加入后 BM25 增益 1.6%→**4.0%**，最优参数从降权变等权 | 评估集构成决定策略价值：131 对偏语义测不出 BM25，+70 条术语类后互补 case 7→19，证明"先扩评估集再评策略" |
| 数据量增大对混合检索的影响（629 段验证） | 增益 0.8%→4.0%→**6.1%**，互补 case 7→19→**39**；Baseline 随规模回归真实（R@5 88.5→83.5） | 候选池增大→向量漏检率上升→BM25 救回价值放大；**629 段下 Hybrid 全指标反超**（R@1 +3.9%、MRR +0.053） |
| Rerank 精排（qwen3-rerank） | Recall@1 **+13.5%**（64.5→78.0），MRR +0.104，R@5 89.6→93.9 | 交叉编码器捕捉 query-doc 细粒度交互，排序质量质变；精排是"召回"到"排得准"的分水岭（R@1 提升远大于 R@5） |
| 中文分词器对比（327 QA） | BM25-only R@1 61.5%→67.3%（CJK→SmartCN）、R@5 82.0%→84.7%（→IK） | CJK bigram 切碎专有名词/术语；词语分词全面胜出，**IK(ik_smart) Hybrid R@5 89.6%→91.7%（+2.1%）**，定为最终分词器 |

### 3.3 踩过的坑

- DashScope Embedding API：Spring AI 客户端限制 25 条 → **实际限制 10 条**（API 400 `batch size should not be larger than 10`）
- Maven 阿里云镜像缺少 `spring-ai-vector-store-pgvector` → 正确 artifactId 是 `spring-ai-pgvector-store`
- PgVectorStore 类路径：`org.springframework.ai.vectorstore.pgvector.PgVectorStore`（多一层 `pgvector` 子包）
- `PgVectorStore.builder()` 第一个参数是 `JdbcTemplate`（不是 `DataSource`）
- `EmbeddingModel` 的方法是 `dimensions()`（无 `get` 前缀）
- 每次重启 `loadAndIndex` 会追加不清理 → 加 `TRUNCATE` 解决

---

## 四、简历话术（可直接引用）

> 独立构建 RAG 检索评估体系（204 段知识库 + 131 QA 对），在 PostgreSQL pgvector（HNSW 索引 + MySQL/PG 双数据源）架构上实验了多策略优化。
>
> **核心发现**：
> ① 数据规模显著影响评估结果——50 段时 Recall@3=89.1%（因 topK 覆盖率 10% 虚高），扩至 204 段回归 84.0%。
> ② HyDE 假设文档生成策略在 171 段规模下将 Recall@3 从 79.2% 提升至 84.2%（+5%），验证了大知识库场景下语义鸿沟弥补的收益。
> ③ 规则版关键词分类器做检索过滤导致 Recall 暴跌 37%，证明元数据过滤依赖高精度意图识别。
> ④ 知识库扩至 629 段后启用向量+BM25 混合检索（pgvector 稠密 + Lucene 稀疏，RRF 融合），在 328 QA 评估集上 Recall@5 从 83.5% 提升至 89.6%（+6.1%），且 Recall@1/MRR 同步反超；三组规模实验（228/629 段）证明数据量越大混合检索优势越显著（增益 0.8%→6.1%）。
> ⑤ 接入 qwen3-rerank 交叉编码器精排后，Recall@1 从 64.5% 提升至 78.0%（+13.5%）、MRR 从 0.75 提升至 0.85——证明"多路召回 → RRF 粗排 → 交叉编码器精排"三层架构中，精排是排序质量提升的核心。
> ⑥ 关键方法论：评估集构成决定策略评估结论——131 对偏语义时 BM25 仅 +1.6%，加入术语类 QA 后放大至 +4.0%，说明"先扩评估集再评优化策略"。
> Prompt 侧引入"仅依据上下文回答 + 无知识拒答"约束减少生成幻觉。

---

## 五、技术栈速查

| 层 | 技术 |
|---|---|
| 向量库 | PostgreSQL 18 + pgvector 0.8.6（HNSW 索引，`vector_cosine_ops`） |
| Embedding | DashScope text-embedding-v4（1024 维） |
| 稀疏检索 | Lucene 9.11（**IK-analyzer ik_smart 词语分词** + BM25Similarity） |
| Rerank | DashScope qwen3-rerank（交叉编码器，text-rerank API） |
| LLM | DashScope qwen-plus |
| 框架 | Spring Boot 3.4.5 + Spring AI 1.0.1 |
| 业务库 | MySQL 8.0（MyBatis-Plus 3.5.5） |
| 切分 | TokenTextSplitter(250, 60) + 句号二级切分 |
| 评估指标 | Recall@1/@3/@5 + MRR |

---

## 六、待做事项（后续优化方向）

- [ ] Hallucination 率对比测试（Prompt 优化前后生成质量量化）
- [ ] 相似度阈值多组对比（0.3 / 0.5 / 0.7）
- [ ] FAQ 精确匹配拦截层
- [x] 向量 + BM25 混合检索（知识库 200+ 段后启用）——228 段已实现，Recall@5 +1.6%
- [x] Rerank 精排（qwen3-rerank）——629 段验证 Recall@1 +13.5%
- [x] BM25 中文分词升级（CJK bigram → IK ik_smart）——Hybrid R@5 89.6%→91.7%
- [ ] 多粒度三层切片 + 三路 RRF（知识库 500+ 段后启用）
