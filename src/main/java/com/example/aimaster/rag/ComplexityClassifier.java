package com.example.aimaster.rag;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 查询复杂度分类器（规则版，零 LLM 成本）：判断问题是否需要走 HyDE 增强检索。
 * <p>
 * 设计（面试可讲）：
 * 1) 为什么规则版不用 LLM 分类：HyDE 本身已多花一次 LLM 调用，若再用 LLM 判断复杂度
 *    就双重开销；规则版（长度/结构词/主题词计数）毫秒级、可解释、阈值可配。
 * 2) 判复杂的依据：长 query（多条件信息量大）、多主题词（跨领域查询）、结构词
 *    （对比/规划/步骤），这类问题与知识库文档的语义鸿沟大，值得用 HyDE 假设文档弥合；
 *    短而直白的问题直接检索召回已足够，HyDE 反而不稳定。
 */
public class ComplexityClassifier {

    /** 结构词：出现即倾向复杂（对比/规划/多步骤等） */
    private static final Pattern STRUCTURE_PATTERN = Pattern.compile(
            "对比|区别|差异|计划|规划|安排|如何|如果|同时|分别|并且|步骤|流程|应该先|有哪些|怎么选择|时间表|路线");

    /** 主题词：跨领域多主题查询更复杂 */
    private static final Pattern TOPIC_PATTERN = Pattern.compile(
            "简历|算法|项目|实习|秋招|春招|面试|技术栈|数据库|分布式|框架|语言|offer|笔试|八股|复盘|时间|英语|考研|竞赛|开源");

    /** 默认长度阈值（字）：超过即视为复杂 */
    public static final int DEFAULT_LENGTH_THRESHOLD = 40;

    private ComplexityClassifier() {
    }

    /**
     * 判断 query 是否复杂（需要 HyDE）：
     * 1) 长度超过阈值；
     * 2) 命中结构词（对比/规划/步骤等）；
     * 3) 命中 ≥2 个不同主题词。
     * 任一满足即复杂。
     *
     * @param query             用户问题（多轮融合后的完整 query）
     * @param lengthThreshold   长度阈值（字），≤0 用默认 40
     */
    public static boolean isComplex(String query, int lengthThreshold) {
        if (query == null || query.isBlank()) return false;
        String q = query.trim();
        int threshold = lengthThreshold > 0 ? lengthThreshold : DEFAULT_LENGTH_THRESHOLD;
        if (q.length() > threshold) return true;
        if (STRUCTURE_PATTERN.matcher(q).find()) return true;
        return countTopics(q) >= 2;
    }

    /** 统计命中的不同主题词数量（去重） */
    static int countTopics(String q) {
        Matcher m = TOPIC_PATTERN.matcher(q);
        java.util.Set<String> found = new java.util.HashSet<>();
        while (m.find()) {
            found.add(m.group());
        }
        return found.size();
    }
}
