package com.example.aimaster.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.example.aimaster.dto.InterviewAnswerRequest;
import com.example.aimaster.dto.InterviewStartRequest;
import com.example.aimaster.entity.InterviewRecord;
import com.example.aimaster.entity.User;
import com.example.aimaster.exception.BusinessException;
import com.example.aimaster.mapper.InterviewRecordMapper;
import com.example.aimaster.mapper.UserMapper;
import com.example.aimaster.rag.HybridRetriever;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

/**
 * AI 面试模拟：按岗位从知识库抽题 → 用户作答 → AI 点评打分 → 出总结报告。
 * <p>
 * 设计（面试可讲）：
 * 1) 题库复用 629 段 RAG 知识库（按 autoTag 分类抽题），题目自带权威参考要点，零新增数据成本；
 * 2) 会话状态用 Caffeine 本地缓存（TTL 30 分钟），与项目缓存规范一致，重启丢失可接受；
 * 3) 点评结合 HybridRetriever 检索题目相关段落，回答对照知识要点评分，防止 AI 主观打分；
 * 4) 商业化咬合：FREE 每日 2 次、VIP 不限次——给 VIP 付费理由（引流功能不扣积分避免劝退）。
 */
@Service
public class InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewService.class);

    /** 一次面试的题目数量 */
    public static final int QUESTION_COUNT = 5;
    /** FREE 用户每日可面试次数 */
    public static final int FREE_DAILY_LIMIT = 2;
    /** 面试会话 TTL（分钟） */
    private static final long SESSION_TTL_MINUTES = 30;

    /** 岗位 → 知识库分类映射（空则随机混合出题） */
    private static final Map<String, String> POSITION_CATEGORY = Map.of(
            "后端", "后端", "前端", "前端", "算法", "算法", "测试", "测试", "运维", "运维");

    /** FREE 标准点评：qwen-plus + 3 维度（成本优先） */
    private static final String STANDARD_REVIEW_PROMPT =
            "你是一位资深技术面试官，正在对求职者的面试回答进行点评。\n"
            + "以下是从知识库检索到的本题参考要点（仅作评分依据，不要照抄）：\n{context}\n"
            + "评分维度固定为：\n"
            + "1. 专业度：知识点准确、术语规范\n"
            + "2. 表达结构：条理清晰、有逻辑层次\n"
            + "3. 完整性：覆盖问题核心要点\n"
            + "只输出一个合法 JSON，格式如下：\n"
            + "{\"totalScore\":78,\"dimensions\":[{\"name\":\"专业度\",\"score\":82,\"comment\":\"...\"},{\"name\":\"表达结构\",\"score\":74,\"comment\":\"...\"},{\"name\":\"完整性\",\"score\":80,\"comment\":\"...\"}],\"comment\":\"整体评价\",\"reference\":\"参考要点\"}\n"
            + "不要输出其他文字，不要用 ```json 包裹。";

    /** VIP 深度点评：qwen-max + 4 维度 + 详尽分析（付费体验差异） */
    private static final String DEEP_REVIEW_PROMPT =
            "你是一位资深技术面试官，正在为 VIP 用户做深度点评，要求比标准点评更细致、更有指导性。\n"
            + "以下是从知识库检索到的本题参考要点（仅作评分依据，不要照抄）：\n{context}\n"
            + "评分维度固定为：\n"
            + "1. 专业度：知识点准确、术语规范\n"
            + "2. 表达结构：条理清晰、有逻辑层次\n"
            + "3. 完整性：覆盖问题核心要点\n"
            + "4. 深度与见解：有个人理解、追问延展、面试官视角的加分点\n"
            + "只输出一个合法 JSON，格式如下：\n"
            + "{\"totalScore\":78,\"dimensions\":[{\"name\":\"专业度\",\"score\":82,\"comment\":\"...\"},{\"name\":\"表达结构\",\"score\":74,\"comment\":\"...\"},{\"name\":\"完整性\",\"score\":80,\"comment\":\"...\"},{\"name\":\"深度与见解\",\"score\":70,\"comment\":\"...\"}],\"comment\":\"整体评价（含具体改进方向）\",\"reference\":\"参考要点\"}\n"
            + "要求：comment 必须给出 2-3 条可执行的改进建议，reference 列出知识库中的关键要点。\n"
            + "不要输出其他文字，不要用 ```json 包裹。";

    /** VIP 深度点评使用的模型 */
    private static final String DEEP_MODEL = "qwen-max";

    /** 抽题后 LLM 改写 Prompt：把知识库陈述句知识点转成可直接作答的面试问句 */
    private static final String QUESTION_REWRITE_PROMPT =
            "你是一位资深技术面试官，正在为求职者出面试题。\n"
            + "下面是从技术知识库抽取的 {n} 条知识点，请把每条改写成一道面试问句：\n"
            + "1) 以问号结尾，口语化、能直接作答，引导求职者解释原理/原因/区别；\n"
            + "2) 不要包含答案内容，顺序与输入一一对应；\n"
            + "3) 若原文已是问句则润色后保留。\n"
            + "知识点：\n{knowledge}\n"
            + "只输出一个合法 JSON 数组，例如 [\"问题1\",\"问题2\"]，不要输出其他文字，不要用 ```json 包裹。";

    /** 单题点评结果 */
    public record Review(int totalScore, List<Dimension> dimensions, String comment, String reference) {
        public record Dimension(String name, int score, String comment) {
        }
    }

    /** 面试会话（Caffeine 缓存） */
    private record Session(Long userId, String position, List<String> questions,
                           List<Answer> answers, int current, boolean vip) {
    }

    private record Answer(String question, String answer, Review review) {
    }

    private final ChatModel chatModel;
    private final HybridRetriever hybridRetriever;
    private final BaguService baguService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final InterviewRecordMapper interviewRecordMapper;

    /** sessionId → 面试会话 */
    private final Cache<String, Session> sessions = Caffeine.newBuilder()
            .expireAfterWrite(SESSION_TTL_MINUTES, TimeUnit.MINUTES)
            .maximumSize(500)
            .build();
    /** key=userId:yyyyMMdd → 已面试次数（FREE 每日 2 次，TTL 24h） */
    private final Cache<String, Integer> dailyCount = Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(2000)
            .build();

    public InterviewService(ChatModel chatModel, HybridRetriever hybridRetriever,
                            BaguService baguService, UserMapper userMapper, ObjectMapper objectMapper,
                            InterviewRecordMapper interviewRecordMapper) {
        this.chatModel = chatModel;
        this.hybridRetriever = hybridRetriever;
        this.baguService = baguService;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
        this.interviewRecordMapper = interviewRecordMapper;
    }

    /** 开始面试：次数校验 → 抽 5 题 → 建会话 → 返回第 1 题 */
    public Map<String, Object> start(Long userId, InterviewStartRequest req) {
        User user = ensureUser(userId);
        boolean vip = isVip(user);
        if (!vip && usedToday(userId) >= FREE_DAILY_LIMIT) {
            throw new BusinessException("免费用户每日可进行 " + FREE_DAILY_LIMIT + " 次面试模拟，开通 VIP 不限次数");
        }

        String category = POSITION_CATEGORY.getOrDefault(req.getPosition().trim(), null);
        List<String> knowledge = drawQuestions(category, QUESTION_COUNT);
        if (knowledge.size() < QUESTION_COUNT) {
            throw new BusinessException("该岗位知识不足，请换个岗位试试");
        }
        // 知识点（陈述句）→ 面试问句（一次 LLM 调用，失败降级用原文保证可用）
        List<String> questions = toInterviewQuestions(knowledge);
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, new Session(userId, req.getPosition().trim(), questions, new ArrayList<>(), 0, vip));
        if (!vip) {
            dailyCount.put(dailyKey(userId), usedToday(userId) + 1);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", sessionId);
        data.put("position", req.getPosition().trim());
        data.put("question", questions.get(0));
        data.put("index", 1);
        data.put("total", QUESTION_COUNT);
        data.put("vip", vip);
        data.put("quotaLeft", quotaLeft(userId, vip));
        return data;
    }

    /** 作答当前题：RAG 检索参考要点 → LLM 点评 → 存答案 → 返回点评与下一题 */
    public Map<String, Object> answer(Long userId, InterviewAnswerRequest req) {
        Session session = sessions.getIfPresent(req.getSessionId());
        if (session == null) throw new BusinessException("面试会话已过期，请重新开始");
        if (!session.userId().equals(userId)) throw new BusinessException("无权操作该会话");
        if (session.current() >= session.questions().size()) throw new BusinessException("面试已结束");

        String question = session.questions().get(session.current());
        Review review = reviewAnswer(question, req.getAnswer(), session.vip());
        List<Answer> answers = new ArrayList<>(session.answers());
        answers.add(new Answer(question, req.getAnswer(), review));
        int next = session.current() + 1;
        sessions.put(req.getSessionId(), new Session(userId, session.position(), session.questions(), answers, next, session.vip()));

        Map<String, Object> data = new HashMap<>();
        data.put("index", next);
        data.put("total", QUESTION_COUNT);
        data.put("finished", next >= QUESTION_COUNT);
        data.put("review", review);
        if (next < QUESTION_COUNT) {
            data.put("nextQuestion", session.questions().get(next));
        }
        return data;
    }

    /** 总结报告：聚合各题评分（总分/分维度均值/题目明细），不额外调 LLM 省钱 */
    public Map<String, Object> report(Long userId, String sessionId) {
        Session session = sessions.getIfPresent(sessionId);
        if (session == null) throw new BusinessException("面试会话已过期，请重新开始");
        if (!session.userId().equals(userId)) throw new BusinessException("无权查看该会话");
        if (session.answers().size() < QUESTION_COUNT) throw new BusinessException("面试尚未完成");

        int totalSum = 0;
        Map<String, Integer> dimSum = new LinkedHashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        for (Answer a : session.answers()) {
            totalSum += a.review().totalScore();
            for (Review.Dimension d : a.review().dimensions()) {
                dimSum.merge(d.name(), d.score(), Integer::sum);
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("question", a.question());
            item.put("score", a.review().totalScore());
            item.put("comment", a.review().comment());
            items.add(item);
        }
        List<Map<String, Object>> dimensions = new ArrayList<>();
        dimSum.forEach((name, sum) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", name);
            m.put("score", sum / QUESTION_COUNT);
            dimensions.add(m);
        });

        Map<String, Object> data = new HashMap<>();
        data.put("position", session.position());
        data.put("totalScore", totalSum / QUESTION_COUNT);
        data.put("dimensions", dimensions);
        data.put("items", items);

        // 完成即落库：供个人中心历史回看 / 周报 / 运营看板统计（失败不影响本次返回）
        try {
            interviewRecordMapper.insert(InterviewRecord.builder()
                    .userId(userId)
                    .position(session.position())
                    .totalScore(totalSum / QUESTION_COUNT)
                    .dimensionsJson(objectMapper.writeValueAsString(dimensions))
                    .itemsJson(objectMapper.writeValueAsString(items))
                    .createTime(LocalDateTime.now())
                    .build());
            log.info("面试记录已保存: userId={} position={} score={}", userId, session.position(), totalSum / QUESTION_COUNT);
        } catch (Exception e) {
            log.warn("面试记录落库失败: {}", e.getMessage());
        }
        return data;
    }

    /** 我的面试记录列表（倒序，不含逐题明细以减负载） */
    public List<Map<String, Object>> records(Long userId) {
        return interviewRecordMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InterviewRecord>()
                        .eq(InterviewRecord::getUserId, userId)
                        .orderByDesc(InterviewRecord::getId)
                        .last("LIMIT 50"))
                .stream().map(r -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", r.getId());
                    m.put("position", r.getPosition());
                    m.put("totalScore", r.getTotalScore());
                    m.put("dimensions", parseJson(r.getDimensionsJson()));
                    m.put("createdAt", r.getCreateTime());
                    return m;
                }).toList();
    }

    /** 单场面试记录详情（含逐题明细），仅本人可看 */
    public Map<String, Object> recordDetail(Long userId, Long recordId) {
        InterviewRecord r = interviewRecordMapper.selectById(recordId);
        if (r == null || !r.getUserId().equals(userId)) throw new BusinessException("记录不存在");
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("position", r.getPosition());
        m.put("totalScore", r.getTotalScore());
        m.put("dimensions", parseJson(r.getDimensionsJson()));
        m.put("items", parseJson(r.getItemsJson()));
        m.put("createdAt", r.getCreateTime());
        return m;
    }

    @SuppressWarnings("unchecked")
    private List<Object> parseJson(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            log.warn("面试记录 JSON 解析失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /** 剩余面试次数（VIP 返回 -1 表示不限） */
    public Map<String, Object> quota(Long userId) {
        User user = ensureUser(userId);
        boolean vip = isVip(user);
        Map<String, Object> data = new HashMap<>();
        data.put("vip", vip);
        data.put("dailyLimit", FREE_DAILY_LIMIT);
        data.put("quotaLeft", quotaLeft(userId, vip));
        return data;
    }

    // ==================== 内部实现 ====================

    /** 抽题后统一 LLM 改写为面试问句；数量不匹配/调用失败时降级返回原文 */
    private List<String> toInterviewQuestions(List<String> knowledge) {
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < knowledge.size(); i++) {
                sb.append(i + 1).append(". ").append(knowledge.get(i)).append("\n");
            }
            String prompt = QUESTION_REWRITE_PROMPT
                    .replace("{n}", String.valueOf(knowledge.size()))
                    .replace("{knowledge}", sb.toString().trim());
            org.springframework.ai.chat.model.ChatResponse response = chatModel.call(
                    new Prompt(List.of(new SystemMessage(prompt))));
            String raw = extractReplyText(response);
            List<String> rewritten = objectMapper.readValue(extractJson(raw),
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            if (rewritten != null && rewritten.size() == knowledge.size()
                    && rewritten.stream().noneMatch(t -> t == null || t.isBlank())) {
                log.info("面试题改写成功: {} 题", rewritten.size());
                return rewritten;
            }
            log.warn("面试题改写数量不匹配({}->{})，降级用原文",
                    knowledge.size(), rewritten == null ? 0 : rewritten.size());
        } catch (Exception e) {
            log.warn("面试题改写失败，降级用原文: {}", e.getMessage());
        }
        return knowledge;
    }

    /** 从知识库按分类抽 N 道不重复的题；分类无独立知识（如测试/运维）时全库随机兜底 */
    private List<String> drawQuestions(String category, int n) {
        List<String> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        drawInto(result, seen, category, n);
        if (result.size() < n) {
            // 兜底：分类内不足（如知识库无“测试/运维”分类）时，剩余从全库随机补足
            drawInto(result, seen, null, n);
        }
        return result;
    }

    private void drawInto(List<String> result, java.util.Set<String> seen, String category, int n) {
        for (int i = 0; i < n * 30 && result.size() < n; i++) {
            BaguService.BaguEntry entry = baguService.random(category);
            if (entry == null) break;
            if (seen.add(entry.content())) {
                result.add(entry.content());
            }
        }
    }

    /** 单题点评：RAG 检索题目相关段落 → LLM 结构化点评 → 解析 JSON。VIP 走 qwen-max 深度点评 */
    private Review reviewAnswer(String question, String answer, boolean vip) {
        String context = "";
        try {
            List<Document> docs = hybridRetriever.retrieve(question, vip ? 5 : 3);
            if (docs != null && !docs.isEmpty()) {
                context = docs.stream().map(Document::getText)
                        .filter(t -> t != null && !t.isBlank())
                        .reduce((a, b) -> a + "\n" + b).orElse("");
            }
        } catch (Exception e) {
            log.warn("面试点评检索知识库失败，降级无参考点评: {}", e.getMessage());
        }
        String systemPrompt = (vip ? DEEP_REVIEW_PROMPT : STANDARD_REVIEW_PROMPT)
                .replace("{context}", context.isBlank() ? "（无参考）" : context);
        String userText = "面试题：\n" + question + "\n\n求职者回答：\n" + answer;
        String raw;
        try {
            Prompt prompt = vip
                    ? new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userText)),
                            org.springframework.ai.chat.prompt.ChatOptions.builder().model(DEEP_MODEL).build())
                    : new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userText)));
            org.springframework.ai.chat.model.ChatResponse response = chatModel.call(prompt);
            raw = extractReplyText(response);
        } catch (Exception e) {
            log.error("面试点评调用模型失败", e);
            throw new BusinessException("点评服务暂时不可用，请稍后重试");
        }
        try {
            String json = extractJson(raw);
            Review review = objectMapper.readValue(json, Review.class);
            if (review.dimensions() == null || review.dimensions().isEmpty()) {
                throw new BusinessException("点评结构异常");
            }
            return review;
        } catch (Exception e) {
            log.warn("面试点评 JSON 解析失败: {}", raw);
            throw new BusinessException("点评解析失败，请重试");
        }
    }

    private boolean isVip(User user) {
        return "VIP".equals(user.getLevel());
    }

    private int usedToday(Long userId) {
        Integer c = dailyCount.getIfPresent(dailyKey(userId));
        return c == null ? 0 : c;
    }

    private int quotaLeft(Long userId, boolean vip) {
        return vip ? -1 : Math.max(0, FREE_DAILY_LIMIT - usedToday(userId));
    }

    private String dailyKey(Long userId) {
        return userId + ":" + LocalDate.now();
    }

    private User ensureUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        return user;
    }

    private String extractReplyText(org.springframework.ai.chat.model.ChatResponse response) {
        if (response == null) return "";
        org.springframework.ai.chat.model.Generation gen = response.getResult();
        if (gen == null && response.getResults() != null && !response.getResults().isEmpty()) {
            gen = response.getResults().get(0);
        }
        if (gen == null || gen.getOutput() == null) return "";
        String text = gen.getOutput().getText();
        return text == null ? "" : text;
    }

    private String extractJson(String raw) {
        String json = raw;
        if (raw.contains("```")) {
            int s = raw.indexOf("```");
            int e = raw.indexOf("```", s + 3);
            if (e > s) {
                json = raw.substring(s + 3, e).replace("json", "").trim();
            }
        }
        return json;
    }
}
