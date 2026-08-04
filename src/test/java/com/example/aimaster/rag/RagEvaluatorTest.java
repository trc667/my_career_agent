package com.example.aimaster.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

/**
 * RAG 检索评估（企业级）：
 * - 关键词使用同义/抽象描述，不是原文原词，防止评估集信息泄露
 * - 引入 MRR（Mean Reciprocal Rank）衡量检索排序质量
 * - 50 段知识库 + 80+ QA 对，模拟真实企业 RAG 评估规模
 * - 支持 HyDE 策略对比（LLM 生成假设答案 → 检索）
 * <p>
 * 运行方式：mvn test -Dtest=RagEvaluatorTest
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
class RagEvaluatorTest {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private ChatModel chatModel;

    /**
     * 80+ QA 对，expectedKeywords 是「语义同义/抽象描述」，不是原文原词。
     * 例如原文有"一页纸"，expected 写"篇幅限制"——必须靠语义理解召回。
     */
    private static final Object[][] QA_PAIRS = {
            // ── 就业方向（expected 为原文存在但问题未提及的词） ──
            {"毕业后可以选哪些技术岗位",                "运维"},
            {"刚入行选什么语言比较稳妥",                "求稳"},
            {"技术岗位除了写代码还有哪些方向",          "产品"},
            // ── 校招时间线 ──
            {"秋季招聘季最密集的月份是哪段",            "8～10"},
            {"暑假前该准备几份工作经历",                "实习"},
            {"面试文档最晚什么时候定下来",              "定稿"},
            // ── 算法复习 ──
            {"刷算法题大概要刷到多少量",                "100"},
            {"笔试最常见的考点涉及哪些数据结构",        "链表"},
            {"除了算法还要背哪些理论知识",              "八股"},
            // ── 简历 ──
            {"校招简历篇幅建议控制在多少",              "一页纸"},
            {"描述实习经历用什么方法写",                "STAR"},
            // ── 面试 ──
            {"一开始的自我介绍讲多久合适",              "1～2 分钟"},
            {"面试最后可以问些什么",                    "培养体系"},
            // ── 实习 ──
            {"选大公司还是小公司实习好",                "规范化流程"},
            {"实习最少做多久才有意义",                  "2～3 个月"},
            // ── 学习环境 ──
            {"在校学习去什么地方效率最高",              "自习室"},
            {"有什么时间管理的小技巧",                  "番茄工作法"},
            // ── 开源 ──
            {"参与开源社区从什么低级任务入手",          "修 typo"},
            {"怎么在代码托管平台参与协作",              "issue"},
            // ── 博客与面经 ──
            {"写技术文章对个人有什么好处",              "巩固"},
            {"面试经验可以在哪里找到",                  "知乎"},
            // ── 后端语言选择 ──
            {"做后端开发选哪门语言合适",                "生态"},
            {"Python适合做什么领域",                     "数据分析"},
            // ── 后端技能 ──
            {"学完框架后应该掌握哪些中间件",            "消息队列"},
            {"异步通信中间件有哪些",                    "Kafka"},
            // ── 前端技能 ──
            {"前端主流框架有哪些选择",                  "React"},
            {"组件间的数据共享怎么管理",                "Pinia"},
            // ── 算法岗 ──
            {"大厂算法岗位对学历有什么要求",            "研究生"},
            {"算法方向需要懂哪些模型理论",              "SVM"},
            // ── 测试岗 ──
            {"测试岗位需要掌握什么自动化框架",          "Selenium"},
            {"测试岗的求职竞争激烈吗",                  "竞争相对小"},
            // ── 运维 ──
            {"运维方向要会什么容器技术",                "Docker"},
            {"现在的云基础设施用什么编排工具",          "K8s"},
            // ── 产品岗 ──
            {"有技术背景转行做产品有优势吗",            "懂技术"},
            {"产品设计师常用什么软件画原型",            "Axure"},
            // ── 考研 ──
            {"本科毕业直接上班还是继续读书",            "本科就业性价比更高"},
            {"什么情况下读研究生更值得",                "大厂核心研发"},
            // ── 投递 ──
            {"校招应该大范围撒网还是精挑细选",          "海投"},
            {"投了简历多久没消息该换目标",              "一周内"},
            // ── 笔试 ──
            {"遇到不会的编程题先做什么",                "暴力解"},
            {"有什么平台可以模拟真实笔试环境",          "牛客网"},
            // ── 群面与HR ──
            {"小组讨论怎么让别人看到你的思路",          "先定框架"},
            {"人力资源面试考察哪些方面",                "稳定性"},
            // ── Offer ──
            {"选offer主要看哪几个维度",                 "晋升空间"},
            {"应届生第一份工作最该看重什么",            "平台比薪资更重要"},
            // ── 项目 ──
            {"如果没有工作经历简历怎么充实",            "高质量项目"},
            {"做项目时最重要的技术思考是什么",          "技术选型"},
            // ── 竞赛 ──
            {"国家级编程竞赛哪个认可度最高",            "ACM"},
            {"不参加比赛怎么证明算法实力",              "LeetCode"},
            // ── 转正 ──
            {"假期实习结束后留用概率大吗",              "转正率"},
            {"转正评审时要展示哪些成果",                "成果量化"},
            // ── 八股 ──
            {"系统底层原理高频考点有哪些",              "内存管理"},
            {"网络协议中连接建立为什么要多个步骤",        "三次握手"},
            {"缓存中请求不存在时怎么处理",              "缓存穿透"},
            // ── 数据库 ──
            {"学数据库从哪个产品入手比较合适",          "MySQL"},
            {"索引的底层存储结构是什么",                "B+树"},
            {"内存缓存的常用数据类型有哪些",            "五种数据结构"},
            // ── 分布式 ──
            {"微服务里怎么知道有哪些可用服务",          "Nacos"},
            {"本地调用和远程调用本质区别在哪",          "RPC"},
            // ── 英语 ──
            {"互联网公司对外语水平有什么硬性要求",      "六级"},
            {"跨国公司面试需要准备什么外语内容",        "英文自我介绍"},
            // ── 心态 ──
            {"屡次面试失败后怎么调整自己",              "复盘"},
            {"找工作期间怎么维持良好的生活节奏",        "规律作息"},
            // ── 前端性能 ──
            {"如何让网页首屏打开更快",                  "懒加载"},
            {"有什么工具可以诊断网页性能瓶颈",          "Lighthouse"},
            // ── 后端性能 ──
            {"数据库查询太慢怎么排查优化",              "慢查询"},
            {"高并发场景下怎么减轻服务压力",            "削峰"},
            // ── 计算机网络 ──
            {"网络分层模型有哪些",                      "OSI"},
            {"网页加密传输的原理是什么",                "HTTPS"},
            // ── 操作系统 ──
            {"操作系统里任务和资源分配的基本单位",      "进程"},
            {"多个任务同时进行怎么避免冲突",            "死锁"},
            // ── 设计模式 ──
            {"怎么保证一个类只有一个实例",              "单例"},
            {"运行中如何给对象动态增加功能",            "代理模式"},
            // ── 数据结构进阶 ──
            {"数据库索引用的是什么数据结构",            "B+树"},
            {"最近最少使用的缓存淘汰用什么数据结构实现", "LRU"},
            // ── Git ──
            {"代码提交历史怎么保持整洁",                "rebase"},
            {"多分支协作时怎么选择合并策略",            "cherry-pick"},
            // ── 软技能 ──
            {"工作中怎么让别人知道你的进展",            "周报"},
            {"给别人检查代码时重点看什么",              "review"},
            // ── AI 趋势 ──
            {"人工智能应用工程化有哪些岗位方向",        "RAG"},
            {"怎么基于预训练模型做一些定制优化",        "LoRA"},
            // ── 学习资源 ──
            {"自学技术看什么教程比较靠谱",              "慕课网"},
            {"查技术方案应该看什么资料最权威",          "官方"},
            // ── 证书 ──
            {"计算机行业有什么值得考的专业证书",        "软考"},
            {"外语证书对校招有多大帮助",                "六级"},
            // ── 春招 ──
            {"错过秋招后还有什么招聘机会",              "春招"},
            {"上半年招聘季的关键时间点",                "金三银四"},
            // ── 内推 ──
            {"怎么找到公司内部的人帮忙推荐",            "学长学姐"},
            {"通过内部推荐投简历真的有用吗",            "免简历初筛"},
            // ── 大厂 vs 创业 ──
            {"毕业第一份工作去知名公司还是初创",        "体系化培养"},
            {"独角兽企业有什么优劣势",                  "股权"},
            // ── WLB ──
            {"互联网公司的工作强度大不大",              "996"},
            {"怎么在面试时了解团队是否经常加班",        "反问环节"},
            // ── 语言深度对比 ──
            {"什么语言适合做高并发网络服务",            "Go"},
            {"哪些语言适合做底层系统或游戏开发",        "C++"},
            // ── 安全方向 ──
            {"保护网站免遭黑客攻击要关注哪些方面",      "XSS"},
            {"有什么展示安全技能的比赛活动",            "CTF"},
            // ── 数据科学 ──
            {"数据类岗位和算法岗有什么不同",            "业务分析"},
            {"衡量页面优化效果常用什么实验方法",        "AB 实验"},
            // ── 职业发展 ──
            {"工作前三年应该重点积累什么能力",          "独立交付"},
            {"技术岗位的职业天花板和转型路径",          "架构师"},
            // ── 分布式协议 ──
            {"ZooKeeper用的什么一致性协议",             "ZAB"},
            {"Raft协议和ZAB有什么主要区别",              "Raft"},
            // ── Sentinel ──
            {"Sentinel熔断后的半开状态怎么工作",          "半开"},
            // ── Redis Cluster ──
            {"Redis集群怎么知道数据存在哪个节点上",       "slot"},
            // ── Service Mesh ──
            {"服务网格的sidecar模式是什么意思",           "Sidecar"},
            // ── Nacos ──
            {"Nacos和Eureka在一致性上有什么不同",         "AP"},
            // ── MapReduce ──
            {"MapReduce的Shuffle阶段做什么",              "Shuffle"},
            // ── Flink ──
            {"Flink怎么处理乱序到达的数据",                "Watermark"},
            // ── 布隆过滤器 ──
            {"布隆过滤器为什么会有假阳性",                "假阳性"},
            // ── LSM树 ──
            {"LSM树为什么写入快读取慢",                   "SSTable"},
            // ── CDN ──
            {"CDN边缘节点没有缓存时怎么处理",             "回源"},
            // ── HTTPS证书 ──
            {"HTTPS证书怎么验证是不是可信机构签发",        "证书链"},
            // ── 正向代理 ──
            {"公司内网上外网一般用哪种代理",              "正向代理"},
            // ── DNS劫持 ──
            {"有什么方案能绕过运营商DNS劫持",              "HTTP DNS"},
            // ── 连接池 ──
            {"数据库连接池最大连接数怎么算",              "连接池"},
            // ── 一致性哈希 ──
            {"一致性哈希的核心原理是什么",                "一致性哈希"},
            // ── 服务降级 ──
            {"双十一大促时怎么保障核心交易可用",          "降级"},
            // ── 索引失效 ──
            {"什么时候MySQL索引会失效不走索引",           "隐式转换"},
            // ── 压测 ──
            {"全链路压测一般分哪几个阶段做",              "压测"},
            // ── OOM类型 ──
            {"Metaspace溢出通常是哪里出了问题",           "Metaspace"},
            // ── 代码优化 ──
            {"循环里频繁调数据库怎么优化",                "批量查询"},
            // ── MyBatis ──
            {"什么场景下用MyBatis比JPA更合适",            "MyBatis"},
            // ── 接口文档 ──
            {"怎么保证后端接口文档和实际返回一致",        "Swagger"},
            // ── Mock ──
            {"单元测试怎么隔离数据库等外部依赖",          "Mockito"},
            // ── 故障定级 ──
            {"线上故障P0级别意味着什么",                   "P0"},
            // ── 令牌桶 ──
            {"令牌桶算法怎么允许短时间的突发流量",        "令牌桶"},
            // ── WebSocket ──
            {"实时聊天适合用轮询还是什么方案",            "WebSocket"},
            // ── Maven ──
            {"Maven怎么查依赖冲突和解决版本不一致",       "dependency:tree"},
            // ── Git撤销 ──
            {"git怎么撤销已经commit但还没push的提交",    "git reset"},
            // ── Linux工具链 ──
            {"Linux下怎么跟踪一个进程的系统调用",         "strace"},
    };

    /**
     * 评估入口：遍历 QA 测试对，计算 Recall@K。
     * <p>
     * 实现步骤（面试可讲）：
     * 1) 遍历 QA_PAIRS，对每个问题调用 vectorStore.similaritySearch(query, topK)
     * 2) 对 K=1,3,5 分别判断 topK 文档中是否存在 content 包含 expectedKeywords 的文档
     * 3) 命中累加，最终计算 Recall = 命中数 / QA_PAIRS.length
     * 4) 输出基线数字到控制台，供后续优化对比
     * <p>
     * 注意：similaritySearch 的 SearchRequest.topK 一次检索返回所有文档，
     * 评估 Recall@5 时没必要调 3 次；一次检索 topK=5，然后取前 1/3/5 分别判断即可。
     */
    @Test
    void evaluateRecall() {
        int maxK = 5;
        int hitAt1 = 0, hitAt3 = 0, hitAt5 = 0;
        double mrr = 0; // Mean Reciprocal Rank：平均倒数排名

        for (Object[] pair : QA_PAIRS) {
            String question = (String) pair[0];
            String expected = (String) pair[1];

            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder().query(question).topK(maxK).build());

            if (docs.isEmpty()) continue;

            // Recall@1
            if (matchesAny(docs.subList(0, 1), expected)) hitAt1++;

            // Recall@3
            int top3 = Math.min(3, docs.size());
            if (matchesAny(docs.subList(0, top3), expected)) hitAt3++;

            // Recall@5
            if (matchesAny(docs.subList(0, docs.size()), expected)) hitAt5++;

            // MRR：找正确答案第一次出现的位置，取倒数累加
            mrr += computeReciprocalRank(docs, expected);
        }

        int total = QA_PAIRS.length;
        System.out.printf("========== RAG 检索质量基线（企业级 50 段知识库 + 90 个同义 QA 对）==========%n");
        System.out.printf("Recall@1: %d/%d = %.1f%%%n", hitAt1, total, 100.0 * hitAt1 / total);
        System.out.printf("Recall@3: %d/%d = %.1f%%%n", hitAt3, total, 100.0 * hitAt3 / total);
        System.out.printf("Recall@5: %d/%d = %.1f%%%n", hitAt5, total, 100.0 * hitAt5 / total);
        System.out.printf("MRR    : %.4f%n", mrr / total);
    }

    /** 任一文档 content 包含 keyword 即命中 */
    private static boolean matchesAny(List<Document> docs, String keyword) {
        for (Document doc : docs) {
            if (doc.getText() != null && doc.getText().contains(keyword)) return true;
        }
        return false;
    }

    /** 计算倒数排名：返回 1 / (正确答案位置+1)；未命中返回 0 */
    private static double computeReciprocalRank(List<Document> docs, String keyword) {
        for (int i = 0; i < docs.size(); i++) {
            if (docs.get(i).getText() != null && docs.get(i).getText().contains(keyword)) {
                return 1.0 / (i + 1);
            }
        }
        return 0;
    }

    // ==================== 三路对比：Baseline / HyDE / HyDE+RRF ====================

    /** 每路检索取 topN 用于 RRF 融合（大于评估 maxK，确保融合后够 5 条） */
    private static final int RRF_TOP_N = 10;
    /** RRF 常数 k：平滑各路由的排名差异 */
    private static final int RRF_K = 60;

    @Test
    void compareRewrite() {
        int maxK = 5;
        int[] b1 = {0, 0, 0}; // baseline: [hit1, hit3, hit5]
        int[] h1 = {0, 0, 0}; // HyDE-only
        int[] r1 = {0, 0, 0}; // HyDE + 原始 RRF 融合
        double bMRR = 0, hMRR = 0, rMRR = 0;

        for (Object[] pair : QA_PAIRS) {
            String question = (String) pair[0];
            String expected = (String) pair[1];
            String hydeQuery = QueryRewriter.hydeRewrite(question, chatModel);

            // 路 1：基线（原始 query）
            List<Document> docsB = vectorStore.similaritySearch(
                    SearchRequest.builder().query(question).topK(maxK).build());
            stats(b1, docsB, expected); bMRR += mrrc(docsB, expected);

            // 路 2：HyDE-only
            List<Document> docsH = vectorStore.similaritySearch(
                    SearchRequest.builder().query(hydeQuery).topK(maxK).build());
            stats(h1, docsH, expected); hMRR += mrrc(docsH, expected);

            // 路 3：HyDE + 原始 RRF 融合
            List<Document> docsR1 = vectorStore.similaritySearch(
                    SearchRequest.builder().query(question).topK(RRF_TOP_N).build());
            List<Document> docsR2 = vectorStore.similaritySearch(
                    SearchRequest.builder().query(hydeQuery).topK(RRF_TOP_N).build());
            List<Document> fused = rrfFuse(docsR1, docsR2, RRF_K, maxK);
            stats(r1, fused, expected); rMRR += mrrc(fused, expected);
        }

        int total = QA_PAIRS.length;
        System.out.println("========== 三路对比：Baseline / HyDE / HyDE+RRF ==========");
        System.out.printf("%-15s %-12s %-12s %-12s%n", "指标", "Baseline", "HyDE-only", "HyDE+RRF");
        System.out.printf("%-15s %-12s %-12s %-12s%n", "-----", "--------", "---------", "---------");
        printRow("Recall@1", b1[0], h1[0], r1[0], total);
        printRow("Recall@3", b1[1], h1[1], r1[1], total);
        printRow("Recall@5", b1[2], h1[2], r1[2], total);
        printRowHR("MRR", bMRR, hMRR, rMRR, total);
    }

    private static void stats(int[] hits, List<Document> docs, String expected) {
        if (docs.isEmpty()) return;
        if (matchesAny(docs.subList(0, 1), expected)) hits[0]++;
        int top3 = Math.min(3, docs.size());
        if (matchesAny(docs.subList(0, top3), expected)) hits[1]++;
        if (matchesAny(docs.subList(0, docs.size()), expected)) hits[2]++;
    }

    private static double mrrc(List<Document> docs, String expected) {
        return docs.isEmpty() ? 0 : computeReciprocalRank(docs, expected);
    }

    private static void printRow(String label, int b, int h, int r, int total) {
        System.out.printf("%-15s %-12s %-12s %-12s%n", label,
                String.format("%.1f%%", 100.0 * b / total),
                String.format("%.1f%%", 100.0 * h / total),
                String.format("%.1f%%", 100.0 * r / total));
    }

    private static void printRowHR(String label, double b, double h, double r, int total) {
        System.out.printf("%-15s %-12s %-12s %-12s%n", label,
                String.format("%.4f", b / total),
                String.format("%.4f", h / total),
                String.format("%.4f", r / total));
    }

    /**
     * RRF（Reciprocal Rank Fusion）融合两路检索结果。
     * 公式：score(d) = Σ 1/(k + rank_i(d))，其中 rank_i(d) 是文档 d 在第 i 路的排名（从 1 开始）。
     * 按 score 降序取 topK。
     */
    static List<Document> rrfFuse(List<Document> list1, List<Document> list2, int k, int topK) {
        Map<String, Double> scores = new java.util.LinkedHashMap<>();
        Map<String, Document> seen = new java.util.LinkedHashMap<>();
        for (int i = 0; i < list1.size(); i++) {
            String id = "r1_" + i;
            scores.put(id, 1.0 / (k + i + 1));
            seen.put(id, list1.get(i));
        }
        for (int i = 0; i < list2.size(); i++) {
            String id = "r2_" + i;
            scores.merge(id, 1.0 / (k + i + 1), Double::sum);
            seen.putIfAbsent(id, list2.get(i));
        }
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> seen.get(e.getKey()))
                .toList();
    }

    // ==================== A/B 对比：Baseline vs Optimized（阈值+类别过滤） ====================

    @Test
    void compareOptimized() {
        int maxK = 5;
        int[] bHit = {0,0,0}, oHit = {0,0,0};
        double bMRR = 0, oMRR = 0;
        for (Object[] pair : QA_PAIRS) {
            String q = (String) pair[0]; String exp = (String) pair[1];
            String cat = RagDocumentLoader.autoTag(q);
            // baseline
            List<Document> bDocs = vectorStore.similaritySearch(SearchRequest.builder().query(q).topK(maxK).build());
            stats(bHit, bDocs, exp); bMRR += mrrc(bDocs, exp);
            // optimized: similarityThreshold 0.5 + category filter
            List<Document> oDocs = vectorStore.similaritySearch(SearchRequest.builder().query(q).topK(maxK)
                    .similarityThreshold(0.5).filterExpression("category == '"+cat+"'").build());
            stats(oHit, oDocs, exp); oMRR += mrrc(oDocs, exp);
        }
        int total = QA_PAIRS.length;
        System.out.println("========== Baseline vs Optimized（阈值+类别过滤）155 段知识库 ==========");
        System.out.printf("%-12s %-12s %-12s%n","指标","Baseline","Optimized");
        System.out.printf("%-12s %-12s %-12s%n","----","--------","---------");
        printRow2("Recall@1",bHit[0],oHit[0],total);
        printRow2("Recall@3",bHit[1],oHit[1],total);
        printRow2("Recall@5",bHit[2],oHit[2],total);
        printRowHR2("MRR",bMRR,oMRR,total);
    }
    private static void printRow2(String l,int b,int o,int t){System.out.printf("%-12s %-12s %-12s%n",l,String.format("%.1f%%",100.0*b/t),String.format("%.1f%%",100.0*o/t));}
    private static void printRowHR2(String l,double b,double o,int t){System.out.printf("%-12s %-12s %-12s%n",l,String.format("%.4f",b/t),String.format("%.4f",o/t));}
}
