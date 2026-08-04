package com.example.aimaster.rag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
class RagEvaluatorTest {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private RagDocumentLoader ragDocumentLoader;

    @Autowired
    private Reranker reranker;

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
            // ── 术语类（验证 BM25 互补价值）：专有名词/英文标识符，问题不含该词，原文含该词 ──
            {"前端状态管理除了 Pinia 还有什么方案",        "Redux"},
            {"用什么工具画产品原型图",                    "Axure"},
            {"产品原型还有哪些主流设计工具",              "Figma"},
            {"自动化测试框架有哪些选择",                  "Selenium"},
            {"性能压测工具有哪些",                        "JMeter"},
            {"持续集成流水线用什么工具编排",              "Jenkins"},
            {"CI 配置怎么写入代码仓库",                   "GitLab CI"},
            {"云原生监控指标用什么系统收集",              "Prometheus"},
            {"监控数据怎么可视化展示",                    "Grafana"},
            {"容器怎么打包成可移植的镜像",                "Dockerfile"},
            {"容器编排平台有哪些主流选择",                "Kubernetes"},
            {"服务注册发现除了 Nacos 还有谁",             "Eureka"},
            {"服务间远程调用用什么组件",                  "Feign"},
            {"服务熔断降级有什么框架",                    "Sentinel"},
            {"熔断器除了 Sentinel 还有什么",              "Hystrix"},
            {"分布式消息中间件有什么选择",                "Kafka"},
            {"消息队列除了 Kafka 还有什么选择",           "RabbitMQ"},
            {"数据库索引的底层数据结构",                  "B+树"},
            {"MySQL 快照读隔离怎么实现",                 "MVCC"},
            {"分库分表中间件有哪些",                      "ShardingSphere"},
            {"反向代理服务器用什么",                      "Nginx"},
            {"无状态登录鉴权方案是什么",                  "JWT"},
            {"第三方登录授权协议是哪个",                  "OAuth"},
            {"分布式 ID 的经典生成算法",                  "雪花算法"},
            {"ZooKeeper 的一致性协议",                    "ZAB"},
            {"etcd 使用的日志复制协议",                   "Raft"},
            {"大数据离线计算框架有哪些",                  "MapReduce"},
            {"MapReduce 中分组排序的阶段叫什么",          "Shuffle"},
            {"实时流计算框架有哪些",                      "Flink"},
            {"Flink 怎么应对数据乱序",                    "Watermark"},
            {"流处理故障恢复的机制叫什么",                "Checkpoint"},
            {"LSM 树的磁盘存储单元叫什么",                "SSTable"},
            {"基于 LSM 的嵌入式存储引擎有哪些",           "RocksDB"},
            {"静态资源加速用什么网络服务",                "CDN"},
            {"CDN 调度依赖哪个 DNS 记录类型",             "CNAME"},
            {"网页加密传输协议",                          "HTTPS"},
            {"浏览器怎么验证服务器身份",                  "证书链"},
            {"绕过运营商 DNS 劫持的方案",                 "HTTP DNS"},
            {"实时双向通信用什么协议",                    "WebSocket"},
            {"Java 在线排查工具里哪个能看方法耗时",       "Arthas"},
            {"堆转储文件用什么工具分析",                  "MAT"},
            {"查看 JVM 线程堆栈用什么命令",               "jstack"},
            {"JVM 低延迟垃圾回收器",                      "G1"},
            {"类加载的双亲委派怎么被打破",                "SPI"},
            {"保证线程可见性的关键字",                    "volatile"},
            {"可重入锁的实现类",                          "ReentrantLock"},
            {"并发同步器的底层框架",                      "AQS"},
            {"线程隔离变量怎么实现",                      "ThreadLocal"},
            {"异步编程的链式调用类",                      "CompletableFuture"},
            {"一次性的线程同步门闩",                      "CountDownLatch"},
            {"可重用的线程同步栅栏",                      "CyclicBarrier"},
            {"读多写少场景的并发列表",                    "CopyOnWriteArrayList"},
            {"高并发 Map 的替代方案",                     "ConcurrentHashMap"},
            {"手写线程池应该用哪个类",                    "ThreadPoolExecutor"},
            {"Java 内存模型的核心规则",                    "happens-before"},
            {"Spring 控制反转容器接口",                    "IoC"},
            {"Spring 面向切面编程",                        "AOP"},
            {"无接口时 Spring 代理用什么生成子类",        "CGLIB"},
            {"Spring 容器顶层接口",                        "ApplicationContext"},
            {"Spring 中创建复杂 Bean 的工厂",             "FactoryBean"},
            {"Spring 事务传播行为默认值",                  "PROPAGATION_REQUIRED"},
            {"Spring 循环依赖靠什么解决",                  "三级缓存"},
            {"Bean 初始化后的回调注解",                    "@PostConstruct"},
            {"Bean 销毁前的回调注解",                      "@PreDestroy"},
            {"Spring MVC 的前端控制器",                    "DispatcherServlet"},
            {"Spring MVC 中返回 JSON 的转换器",            "HttpMessageConverter"},
            {"Spring 类型转换服务接口",                    "ConversionService"},
            {"Spring Boot 集成测试注解",                   "@SpringBootTest"},
            {"Spring 测试里替换依赖的注解",                "@MockBean"},
            {"单元测试怎么隔离外部依赖",                  "Mockito"},
            // ── 语义类补充（补新增 24 段盲区：Java 并发深入 + Spring 原理） ──
            {"多个线程修改同一个变量怎么保证安全",        "AtomicInteger"},
            {"锁等待超时怎么设置",                        "tryLock"},
            {"可重入锁手动释放的规范写法",                "unlock"},
            {"线程池队列爆了怎么办",                      "CallerRunsPolicy"},
            {"线程池无界队列会导致什么问题",              "OOM"},
            {"怎么优雅地停止一个线程",                    "interrupt"},
            {"异步任务怎么合并多个结果",                  "allOf"},
            {"异步异常怎么兜底处理",                      "exceptionally"},
            {"读写频繁时用什么代替 CopyOnWriteArrayList", "ConcurrentHashMap"},
            {"Spring Bean 默认作用域",                     "单例"},
            {"Spring 通知里必须调用的方法",               "proceed"},
            {"事务注解在自调用时会怎样",                  "失效"},
            {"自动配置的条件注解族",                      "Conditional"},
            {"Spring 配置文件怎么按环境隔离",             "Profile"},
            {"敏感配置怎么避免写进代码",                  "Config Server"},
            {"注册事件监听用什么注解",                    "@EventListener"},
            {"异步监听事件怎么开启",                      "@Async"},
            {"请求参数怎么绑定到方法",                    "@RequestParam"},
            {"只测 Web 层的测试注解",                     "@WebMvcTest"},
            {"MySQL 执行计划分析命令",                    "EXPLAIN"},
            {"缓存击穿怎么解决",                          "互斥锁"},
            {"缓存穿透用什么数据结构过滤",                "布隆过滤器"},
            {"数据库主从切换自动化的组件",                "MHA"},
            {"主从复制依赖的日志",                        "binlog"},
            {"TCP 粘包的一种解法",                        "自定义协议头"},
            {"接口幂等可以用什么做",                      "唯一索引"},
            {"分布式事务的一种落地方式",                  "本地事件表"},
            {"短网址的短码怎么生成",                      "Base62"},
            {"Redis 集群的槽位总数",                      "16384"},
            {"缓存与数据库不一致的解决思路",              "先删缓存"},
            {"线上 CPU 飙升的第一步排查命令",             "top -H"},
            {"OOM 时先配置什么参数",                      "HeapDump"},
            {"查看 GC 频率的命令",                        "jstat"},
            {"日志框架的接口标准",                        "SLF4J"},
            {"日志集中收集的常见方案",                    "ELK"},
            {"分布式链路追踪用什么",                      "SkyWalking"},
            {"秒杀场景怎么防超卖",                        "乐观锁"},
            {"令牌桶的兄弟算法",                          "漏桶"},
            {"唯一 ID 的号段模式是哪个中间件",            "Leaf"},
            {"ID 生成里时钟回拨怎么处理",                "抛异常拒绝"},
            {"Spring 事件解耦的典型场景",                 "发邮件"},
            {"半成品 Bean 存放在哪级缓存",                "earlySingletonObjects"},
            {"IoC 容器的最基础接口",                      "BeanFactory"},
            {"Spring 测试里执行初始化 SQL 的注解",        "@Sql"},
            {"ConcurrentHashMap 为什么不允许空键",        "null key"},
            {"CopyOnWriteArrayList 迭代器为什么不支持删除", "UnsupportedOperationException"},
            {"Kafka 保证顺序的机制",                      "消费者组"},
            {"远程办公的找岗平台",                        "RemoteOK"},
            {"敏捷开发里谁是需求负责人",                  "PO"},
            {"观察性三驾马车里谁负责趋势",                "Metrics"},
            // ── 第二批补充（覆盖新增 20+ 主题，expected 均为原文已验证词） ──
            {"链表过长时 HashMap 如何优化查询",           "红黑树"},
            {"Redis 有序集合的底层结构",                  "ZSET"},
            {"LRU 缓存经典实现组合",                      "双向链表"},
            {"懒汉单例怎么防并发重复创建",                "双重检查锁"},
            {"一致性哈希节点变动最少迁移什么",             "数据迁移"},
            {"秒杀扣库存的原子脚本方案",                  "Lua"},
            {"K8s 最小调度单元",                          "Pod"},
            {"K8s 集群对外流量入口",                      "Ingress"},
            {"K8s 按负载自动扩缩容组件",                  "HPA"},
            {"Spring Cloud 客户端负载均衡组件",           "LoadBalancer"},
            {"MySQL 幻读的锁解决机制",                    "间隙锁"},
            {"Redis 全量快照持久化",                      "RDB"},
            {"Redis 追加日志持久化",                      "AOF"},
            {"缓存雪崩怎么预防",                          "随机过期"},
            {"缓存与库异步同步的组件",                    "Canal"},
            {"Redis 分布式锁原子获取命令",                "SET NX"},
            {"ZooKeeper 锁用的节点类型",                  "临时顺序节点"},
            {"ZooKeeper 的变更通知机制",                  "Watch"},
            {"消费者数量变化触发什么",                     "Rebalance"},
            {"Flink 按活跃间隔切的窗口",                  "会话窗口"},
            {"TCP 检测丢包后的恢复手段",                  "快速重传"},
            {"TCP 刚建立时拥塞窗口怎么增长",              "慢启动"},
            {"进程间通信最高效方式",                      "共享内存"},
            {"死锁避免的预判算法",                        "银行家算法"},
            {"类加载的委派模型名称",                      "双亲委派"},
            {"JDK8 方法区的新实现",                       "Metaspace"},
            {"CPU 缓存一致性协议",                        "MESI"},
            {"多线程改不同变量却互相拖累的现象",          "伪共享"},
            {"MySQL 索引下推优化",                        "索引下推"},
            {"覆盖索引避免什么操作",                      "回表"},
            {"联合索引失效的一个前提",                    "最左前缀"},
            {"MySQL 解决幻读的组合锁",                    "Next-Key Lock"},
            {"操作系统按需加载内存的机制",                "虚拟内存"},
            {"缓存淘汰的频率算法",                        "LFU"},
            {"并发控制的信号量机制",                      "信号量"},
            {"进程间数据传输的字节流方式",                "管道"},
            {"高并发系统的削峰手段",                      "消息队列"},
            {"消息不重复消费的解法",                      "幂等"},
            {"Linux 文件权限的命令",                      "chmod"},
            {"浏览器存储登录态的常用方式",                "localStorage"},
            {"服务注册中心的心跳超时保护",                "心跳"},
            {"Spring Cloud 网关组件",                     "Gateway"},
            {"微服务配置中心",                            "配置中心"},
            {"服务网格的控制面组件",                      "Istio"},
            {"Java 里怎么获取类的字节码对象",             "反射"},
            {"I/O 模型里异步非阻塞是哪种",                "AIO"},
            {"用户态和内核态切换的开销来源",              "内核态"},
            {"接口访问不存在的资源返回什么码",            "404"},
            {"RESTful 接口资源用复数表示",                "users"},
            {"JWT 的三个组成部分之一",                    "payload"},
            {"OAuth 拿 code 换什么",                      "access_token"},
            {"微服务每个服务独立什么",                    "数据库"},
            {"限流阈值超限的响应码",                      "429"},
            {"Sentinel 慢调用比例触发什么",               "熔断"},
            {"K8s 里管理应用实例数的资源",                "Deployment"},
            {"CI 中自动化构建的工具",                     "GitHub Actions"},
            {"Kafka 的高吞吐写入原理",                    "顺序写磁盘"},
            {"分布式事务的 TCC 是什么",                   "TCC"},
            {"服务降级的兜底手段",                        "stale data"},
            {"数据库读写分离引入什么新问题",              "主从延迟"},
            {"Redis 集群请求重定向错误",                  "MOVED"},
            {"Nginx 反向代理的指令",                      "proxy_pass"},
            {"Docker 与虚拟机的核心区别",                 "共享内核"},
            {"日志级别最低的是哪个",                      "DEBUG"},
            {"Arthas 查看方法调用耗时",                   "trace"},
            {"jmap 导出堆转储",                           "dump"},
            {"消息队列投递至少一次的语义",                "at-least-once"},
            {"Redis 缓存预热的目的",                      "提前加载"},
            {"数据库连接池最小空闲连接的目的",            "预热"},
            {"接口幂等键的常见载体",                      "幂等 key"},
            {"CDN 没有缓存时的行为",                      "回源"},
            {"前端路由优化首屏的手段",                      "代码分割"},
            {"在线笔试训练平台",                          "牛客网"},
            {"技术分享的常见平台",                        "掘金"},
            {"项目管理里的冲刺迭代",                      "Sprint"},
            {"单元测试框架",                              "JUnit"},
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
        int top5 = Math.min(5, docs.size());
        if (matchesAny(docs.subList(0, top5), expected)) hits[2]++;
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

    // ==================== 向量 + BM25 混合检索（Hybrid + RRF） ====================

    /**
     * 三路对比：Baseline（纯向量） / BM25-only（Lucene 稀疏） / Hybrid（向量+BM25 → RRF）。
     * <p>
     * 面试可讲的要点：
     * 1) 知识库 ≥200 段后启用混合检索（用户既定门槛）：向量负责语义召回，BM25 负责专有名词/数字精确命中。
     * 2) 测试前先 loadAndIndex 重新向量化，保证向量库与 BM25 索引同一知识源（公平对照）。
     * 3) RRF 融合：score(d) = Σ 1/(k + rank_i(d))，同一文档在两路都命中时分数累加（见 rrfFuseByText）。
     */
    @Test
    void compareHybrid() {
        // 1) 重新向量化最新知识库（TRUNCATE + 全量入库），保证与 BM25 索引同源
        log.info("重新向量化知识库（228 段），保证与 BM25 索引同源...");
        ragDocumentLoader.loadAndIndex(new ClassPathResource("rag/career-tips.txt"));

        Bm25Retriever bm25 = new Bm25Retriever(new ClassPathResource("rag/career-tips.txt"));
        int maxK = 5;

        // 预收集：每个 QA 的 baseline(top5)、向量 top10、BM25 top10（检索只做一次，融合参数遍历在内存完成）
        List<List<Document>> baseList = new ArrayList<>();
        List<List<Document>> vTopList = new ArrayList<>();
        List<List<Document>> bTopList = new ArrayList<>();
        // 互补性诊断：仅 BM25 top10 命中 / 仅向量 top5 命中 / 共同命中
        int bm25Rescues = 0, vectorOnly = 0, bothHit = 0;
        List<String> rescueQueries = new ArrayList<>();
        List<String> vectorOnlyQueries = new ArrayList<>();
        for (Object[] pair : QA_PAIRS) {
            String q = (String) pair[0];
            String exp = (String) pair[1];
            List<Document> base = vectorStore.similaritySearch(
                    SearchRequest.builder().query(q).topK(maxK).build());
            List<Document> vTop = vectorStore.similaritySearch(
                    SearchRequest.builder().query(q).topK(RRF_TOP_N).build());
            List<Document> bTop = bm25.search(q, RRF_TOP_N);
            baseList.add(base);
            vTopList.add(vTop);
            bTopList.add(bTop);
            boolean baseHit = matchesAny(base, exp);
            boolean bmHit = matchesAny(bTop, exp);
            if (!baseHit && bmHit) { bm25Rescues++; rescueQueries.add(q + " → " + exp); }
            else if (baseHit && !bmHit) { vectorOnly++; vectorOnlyQueries.add(q + " → " + exp); }
            else if (baseHit) bothHit++;
        }

        // 融合参数组：{k, 向量权重 w1, BM25 权重 w2}
        double[][] params = {
                {60, 1.0, 1.0}, // 标准 RRF（等权）
                {30, 1.0, 1.0}, // 更激进：强调两路靠前排名
                {60, 1.0, 0.5}, // 向量为主
                {60, 1.0, 0.3}, // 向量更强
        };
        String[] names = {"k60 w1:1 w2:1", "k30 w1:1 w2:1", "k60 w1:1 w2:.5", "k60 w1:1 w2:.3"};

        int total = QA_PAIRS.length;
        System.out.println("========== 向量+BM25 混合检索：融合参数调优（228 段知识库） ==========");
        System.out.printf("互补性：%d/%d 个 query 仅 BM25 在 top10 命中（向量 top5 漏检）%n", bm25Rescues, total);
        System.out.println("== 互补性诊断（top5 向量 vs top10 BM25）==");
        System.out.printf("  共同命中 %d | 仅向量命中 %d | 仅 BM25 命中 %d | 两路都未命中 %d%n",
                bothHit, vectorOnly, bm25Rescues, total - bothHit - vectorOnly - bm25Rescues);
        System.out.println("  仅 BM25 命中（BM25 独有价值的证据）:");
        for (String s : rescueQueries) System.out.println("    " + s);
        System.out.println("  仅向量命中（BM25 漏检）:");
        for (String s : vectorOnlyQueries) System.out.println("    " + s);

        // Baseline（纯向量）
        int[] bHits = {0, 0, 0};
        double bMRR = 0;
        for (int i = 0; i < QA_PAIRS.length; i++) {
            String exp = (String) QA_PAIRS[i][1];
            stats(bHits, baseList.get(i), exp);
            bMRR += mrrc(baseList.get(i), exp);
        }
        System.out.println("Baseline（纯向量）:");
        System.out.printf("  Recall@1: %.1f%%   Recall@3: %.1f%%   Recall@5: %.1f%%   MRR: %.4f%n",
                100.0 * bHits[0] / total, 100.0 * bHits[1] / total, 100.0 * bHits[2] / total, bMRR / total);

        // 各组融合参数
        for (int gi = 0; gi < params.length; gi++) {
            int k = (int) params[gi][0];
            double w1 = params[gi][1], w2 = params[gi][2];
            int[] hits = {0, 0, 0};
            double mrrSum = 0;
            for (int i = 0; i < QA_PAIRS.length; i++) {
                String exp = (String) QA_PAIRS[i][1];
                List<Document> fused = rrfFuseWeighted(vTopList.get(i), bTopList.get(i), k, w1, w2, maxK);
                stats(hits, fused, exp);
                mrrSum += mrrc(fused, exp);
            }
            System.out.printf("Hybrid %s:", names[gi]);
            System.out.printf("  Recall@1: %.1f%%   Recall@3: %.1f%%   Recall@5: %.1f%%   MRR: %.4f%n",
                    100.0 * hits[0] / total, 100.0 * hits[1] / total, 100.0 * hits[2] / total, mrrSum / total);
        }

        // 路 4：Hybrid + Rerank 精排（子样本前 SAMPLE 个 QA 调 qwen3-rerank，节省 API 调用时间）
        int sampleN = 60;
        int[] sBase = {0, 0, 0}, sHyb = {0, 0, 0}, sRank = {0, 0, 0};
        double sBM = 0, sHM = 0, sRM = 0;
        int sampleCount = Math.min(sampleN, QA_PAIRS.length);
        for (int i = 0; i < sampleCount; i++) {
            String q = (String) QA_PAIRS[i][0];
            String exp = (String) QA_PAIRS[i][1];
            // baseline（同口径）
            stats(sBase, baseList.get(i), exp); sBM += mrrc(baseList.get(i), exp);
            // hybrid（等权 RRF top5）
            List<Document> fused5 = rrfFuseWeighted(vTopList.get(i), bTopList.get(i), 60, 1.0, 1.0, maxK);
            stats(sHyb, fused5, exp); sHM += mrrc(fused5, exp);
            // hybrid + rerank（RRF 融合 top10 → qwen3-rerank → top5）
            List<Document> fused10 = rrfFuseWeighted(vTopList.get(i), bTopList.get(i), 60, 1.0, 1.0, RRF_TOP_N);
            List<Document> reranked = reranker.rerank(q, fused10, maxK);
            stats(sRank, reranked, exp); sRM += mrrc(reranked, exp);
        }
        System.out.println("========== 子样本三路对比（前 " + sampleCount + " QA，qwen3-rerank 精排） ==========");
        System.out.printf("%-18s %-18s %-18s %-18s%n", "指标", "Baseline", "Hybrid(等权)", "Hybrid+Rerank");
        System.out.printf("%-18s %-18s %-18s %-18s%n", "-----", "--------", "-----------", "-------------");
        System.out.printf("%-18s %-18.1f%% %-18.1f%% %-18.1f%%%n", "Recall@1",
                100.0 * sBase[0] / sampleCount, 100.0 * sHyb[0] / sampleCount, 100.0 * sRank[0] / sampleCount);
        System.out.printf("%-18s %-18.1f%% %-18.1f%% %-18.1f%%%n", "Recall@3",
                100.0 * sBase[1] / sampleCount, 100.0 * sHyb[1] / sampleCount, 100.0 * sRank[1] / sampleCount);
        System.out.printf("%-18s %-18.1f%% %-18.1f%% %-18.1f%%%n", "Recall@5",
                100.0 * sBase[2] / sampleCount, 100.0 * sHyb[2] / sampleCount, 100.0 * sRank[2] / sampleCount);
        System.out.printf("%-18s %-18.4f %-18.4f %-18.4f%n", "MRR",
                sBM / sampleCount, sHM / sampleCount, sRM / sampleCount);
    }

    /**
     * 加权 RRF：score(d) = w1/(k + rank1(d)) + w2/(k + rank2(d))，
     * 以文档文本为 key，同一文档两路都命中时分数累加；w1/w2 可调两路贡献。
     */
    static List<Document> rrfFuseWeighted(List<Document> list1, List<Document> list2, int k,
                                          double w1, double w2, int topK) {
        Map<String, Double> scores = new LinkedHashMap<>();
        Map<String, Document> seen = new LinkedHashMap<>();
        indexByTextWeighted(list1, scores, seen, k, w1);
        indexByTextWeighted(list2, scores, seen, k, w2);
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> seen.get(e.getKey()))
                .toList();
    }

    private static void indexByTextWeighted(List<Document> docs, Map<String, Double> scores,
                                            Map<String, Document> seen, int k, double weight) {
        for (int i = 0; i < docs.size(); i++) {
            String text = docs.get(i).getText();
            if (text == null || text.isBlank()) continue;
            scores.merge(text, weight / (k + i + 1), Double::sum);
            seen.putIfAbsent(text, docs.get(i));
        }
    }
}
