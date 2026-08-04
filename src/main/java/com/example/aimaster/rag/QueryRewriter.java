package com.example.aimaster.rag;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Query 改写器（规则版）—— 对用户问题做预处理，提升语义检索召回率。
 * <p>
 * 策略（面试可讲的三层）：
 * 1) 口语化清洗：去掉疑问语气词，减少噪声干扰 embedding
 * 2) 领域词映射：把口语表达映射为标准技术关键词（如"怎么写简历"→"简历"）
 * 3) 关键词拼接：把清洗后的词用空格拼接，短关键词串比长问句更适合 embedding 匹配
 */
public class QueryRewriter {

    // ==================== 1. 口语化清洗 ====================
    // 用 | 连接所有常见疑问语气词/虚词，编译为单一正则
    // 面试可讲：Pattern.compile 把正则表达式编译为有限状态机，
    // matcher.replaceAll(" ") 把每个匹配替换为空格，
    // 后续 replaceAll("\\s+", " ") 压缩连续空格。
    private static final Pattern SPOKEN_PATTERN =
            Pattern.compile("吗|呢|啊|么|怎么|怎样|啥|什么|该如何|怎么办|应该|可以|是|的|哪些|哪");

    // ==================== 2. 领域词映射表 ====================
    private static final Map<String, String> DOMAIN_MAP = new LinkedHashMap<>();
    static {
        DOMAIN_MAP.put("选什么语言","编程语言");
        DOMAIN_MAP.put("怎么写简历","简历");
        DOMAIN_MAP.put("刷题",      "LeetCode");
        DOMAIN_MAP.put("投简历",    "投递");
        DOMAIN_MAP.put("选offer",   "offer");
        DOMAIN_MAP.put("读研",      "考研");
        DOMAIN_MAP.put("群面",      "群面");
        DOMAIN_MAP.put("HR面",      "HR");
        DOMAIN_MAP.put("无领导小组","群面");
        DOMAIN_MAP.put("转正",      "转正");
        DOMAIN_MAP.put("内推",      "内推");
        DOMAIN_MAP.put("加班",      "加班");
        DOMAIN_MAP.put("大厂",      "大厂");
        DOMAIN_MAP.put("创业公司",  "创业");
        DOMAIN_MAP.put("独角兽",    "创业");
        DOMAIN_MAP.put("前端",      "前端");
        DOMAIN_MAP.put("后端",      "后端");
        DOMAIN_MAP.put("数据库",    "数据库");
        DOMAIN_MAP.put("分布式",    "分布式");
        DOMAIN_MAP.put("算法岗",    "算法");
        DOMAIN_MAP.put("测试岗",    "测试");
        DOMAIN_MAP.put("运维",      "运维");
        DOMAIN_MAP.put("安全",      "安全");
        DOMAIN_MAP.put("产品",      "产品");
        DOMAIN_MAP.put("设计模式",  "设计模式");
        DOMAIN_MAP.put("英语",      "英语");
        DOMAIN_MAP.put("六级",      "英语");
        DOMAIN_MAP.put("证书",      "证书");
        DOMAIN_MAP.put("技术博客",  "博客");
        DOMAIN_MAP.put("开源",      "开源");
        DOMAIN_MAP.put("学习资源",  "学习资源");
        DOMAIN_MAP.put("职业发展",  "职业规划");
        DOMAIN_MAP.put("缓存",      "Redis");
        DOMAIN_MAP.put("微服务",    "微服务");
        DOMAIN_MAP.put("性能优化",  "性能优化");
        DOMAIN_MAP.put("AI",        "RAG");
        DOMAIN_MAP.put("大模型",    "AIGC");
        DOMAIN_MAP.put("机器学习",  "机器学习");
        DOMAIN_MAP.put("计算机网络","计算机网络");
        DOMAIN_MAP.put("操作系统",  "操作系统");
        DOMAIN_MAP.put("编程竞赛",  "ACM");
        DOMAIN_MAP.put("数据结构",  "数据结构");
        DOMAIN_MAP.put("软技能",    "职场沟通");
        DOMAIN_MAP.put("秋招",      "秋招");
        DOMAIN_MAP.put("春招",      "春招");
        DOMAIN_MAP.put("校招",      "秋招");
        DOMAIN_MAP.put("笔试",      "笔试");
        DOMAIN_MAP.put("面试",      "面试");
        DOMAIN_MAP.put("实习",      "实习");
        DOMAIN_MAP.put("考研",      "考研");
        DOMAIN_MAP.put("八股",      "八股");
        DOMAIN_MAP.put("简历",      "简历");
        DOMAIN_MAP.put("项目",      "项目");
        DOMAIN_MAP.put("GUI",       "Git");
        DOMAIN_MAP.put("Python",    "Python");
        DOMAIN_MAP.put("Go语言",    "Go");
        DOMAIN_MAP.put("C\\+\\+",   "C++");
        DOMAIN_MAP.put("Java",      "Java");
    }

    // ==================== 3. 改写入口 ====================
    /**
     * @param rawQuestion 用户原始问题（可能含口语/模糊表述）
     * @return 改写后的检索词串（空格分隔的干净关键词）
     *
     * 示例：
     *   "怎么写简历才能被大厂捞起来呢" → "简历 大厂"
     */
    public static String rewrite(String rawQuestion) {
        if (rawQuestion == null || rawQuestion.isBlank()) return rawQuestion;

        // 第 1 步：口语化清洗 — 去掉所有疑问语气词
        String cleaned = SPOKEN_PATTERN.matcher(rawQuestion).replaceAll(" ");

        // 第 2 步：领域词映射 — 口语表达 → 标准关键词
        String mapped = cleaned;
        for (Map.Entry<String, String> e : DOMAIN_MAP.entrySet()) {
            mapped = mapped.replace(e.getKey(), e.getValue());
        }

        // 第 3 步：去重空格 + trim
        return mapped.replaceAll("\\s+", " ").trim();
    }

    // ==================== 4. HyDE 策略 ====================
    /**
     * HyDE（Hypothetical Document Embeddings）：用 LLM 先生成假设性答案，
     * 再用这个答案做向量检索。因为假设答案的语言风格更接近知识库文档，
     * 语义匹配效果通常优于直接用问题检索。
     *
     * 面试可讲：HyDE 把检索从"query-document 匹配"变成了
     * "document-document 匹配"，利用 LLM 的生成能力弥补查询与知识的语义鸿沟。
     *
     * @param question  原始用户问题
     * @param chatModel 大模型聊天接口（DashScope qwen-plus，由调用方注入）
     * @return 假设性回答文本（作为检索 query 使用）
     */
    public static String hydeRewrite(String question, ChatModel chatModel) {
        if (question == null || question.isBlank() || chatModel == null) return question;

        // HyDE prompt：让 LLM 以"计算机求职顾问"身份生成一段具体回答
        String promptText = String.format(
                "您是一名计算机专业学生求职顾问。请根据以下问题，" +
                "生成一段100-200字的假设性回答，要求内容具体、有可操作建议。" +
                "只输出回答本身，不加任何前缀或说明。\n\n问题：%s", question);

        ChatResponse response = chatModel.call(new Prompt(new UserMessage(promptText)));
        String answer = response.getResult().getOutput().getText();
        // 控制假设文档长度，减少后续 embedding token 消耗
        return answer != null && answer.length() > 300
                ? answer.substring(0, 300)
                : (answer != null ? answer : question);
    }
}
