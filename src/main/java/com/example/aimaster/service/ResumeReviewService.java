package com.example.aimaster.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aimaster.dto.ResumeDimension;
import com.example.aimaster.dto.ResumeReviewRequest;
import com.example.aimaster.dto.ResumeReviewResult;
import com.example.aimaster.entity.ResumeReview;
import com.example.aimaster.exception.BusinessException;
import com.example.aimaster.filter.SensitiveWordFilter;
import com.example.aimaster.mapper.ResumeReviewMapper;
import com.example.aimaster.rag.HybridRetriever;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历评分服务：结合 RAG 知识库检索的简历写作规范，由 qwen-plus 分维度评分，
 * 输出总分/维度明细/亮点不足/优化版简历，并落库支持历史回看。
 */
@Service
public class ResumeReviewService {

    private static final Logger log = LoggerFactory.getLogger(ResumeReviewService.class);

    /** 评分分析 prompt（只输出评分与点评，不含优化版简历，输出小响应快） */
    private static final String ANALYZE_PROMPT =
            "你是一位资深技术面试官与简历优化专家，为计算机专业学生/求职者的简历评分。\n"
            + "参考以下简历写作规范（知识库检索结果，仅作参考，不要照抄）：\n{context}\n"
            + "评分维度固定为：\n"
            + "1. 项目经历：技术深度、真实复杂度、职责与成果清晰度\n"
            + "2. 技术栈表达：关键词使用、熟练程度描述\n"
            + "3. 量化成果：数据支撑、可衡量性\n"
            + "4. 结构表达：条理性、STAR 法则、排版\n"
            + "5. 岗位匹配度：与目标岗位的相关性\n"
            + "6. 综合素养：实习/竞赛/开源/证书等加分项\n"
            + "只输出一个合法 JSON，格式如下：\n"
            + "{\"totalScore\":78,\"summary\":\"总体评价\",\"dimensions\":[{\"name\":\"项目经历\",\"score\":82,\"comment\":\"现状评价\",\"suggestion\":\"改进建议\"}],\"highlights\":[\"亮点1\"],\"weaknesses\":[\"不足1\"]}\n"
            + "不要输出其他文字，不要用 ```json 包裹。";

    /** 优化版简历 prompt（根据原文+评分结果，输出优化后的完整简历全文） */
    private static final String OPTIMIZE_PROMPT =
            "你是一位资深简历优化专家。请根据以下简历原文和已有评分意见，输出一份优化后的完整简历全文。\n"
            + "要求：保留原信息结构（基本信息/教育背景/实习/项目/技能/荣誉），补充量化表述、修正表达问题、突出与目标岗位匹配的亮点。\n"
            + "直接输出简历正文（纯文本，用换行分隔），不要输出任何解释、标题前缀或 Markdown 代码块。";

    private final ChatModel chatModel;
    private final HybridRetriever hybridRetriever;
    private final ResumeReviewMapper resumeReviewMapper;
    private final ObjectMapper objectMapper;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final PointService pointService;

    public ResumeReviewService(ChatModel chatModel,
                               HybridRetriever hybridRetriever,
                               ResumeReviewMapper resumeReviewMapper,
                               ObjectMapper objectMapper,
                               @Autowired(required = false) SensitiveWordFilter sensitiveWordFilter,
                               PointService pointService) {
        this.chatModel = chatModel;
        this.hybridRetriever = hybridRetriever;
        this.resumeReviewMapper = resumeReviewMapper;
        this.objectMapper = objectMapper;
        this.sensitiveWordFilter = sensitiveWordFilter;
        this.pointService = pointService;
    }

    /**
     * 评分并保存记录（调用前余额预检、结束后按 token 结算，防爆刷 LLM）。
     *
     * @param userId   当前用户 ID（由 Controller 从 JWT 解析）
     * @param username 当前用户名（积分预检/结算用，VIP/ADMIN 免扣）
     * @param req      简历内容 + 目标岗位
     * @return 评分结果
     */
    public Map<String, Object> analyze(Long userId, String username, ResumeReviewRequest req) {
        String resumeText = filter(req.getResumeText() == null ? "" : req.getResumeText());
        if (resumeText == null || resumeText.isBlank()) {
            throw new BusinessException("简历内容不能为空");
        }
        String targetPosition = req.getTargetPosition() != null ? req.getTargetPosition().trim() : "";

        // 计费预扣：分析 1 分（余额不足拦截，后续失败退回）
        pointService.consumeForChat(username, 1, "AI 简历分析");

        String context = retrieveContext();

        // 2. 调用模型生成结构化评分（不含优化版，输出小响应快）
        String systemPrompt = ANALYZE_PROMPT.replace("{context}", context.isBlank() ? "（无参考规范）" : context);
        String userText = (targetPosition.isBlank() ? "目标岗位：未指定" : "目标岗位：" + targetPosition)
                + "\n简历内容：\n" + resumeText;
        String raw;
        try {
            org.springframework.ai.chat.model.ChatResponse response =
                    chatModel.call(new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userText))));
            raw = extractReplyText(response);
        } catch (Exception e) {
            pointService.addPoints(userId, 1, "简历分析失败退回");
            log.error("简历评分调用模型失败", e);
            throw new BusinessException("评分服务暂时不可用，请稍后重试");
        }
        if (raw == null || raw.isBlank()) {
            pointService.addPoints(userId, 1, "简历分析失败退回");
            throw new BusinessException("评分失败，请重试");
        }

        // 3. 解析 + 敏感词过滤
        ResumeReviewResult result = sanitize(parseResult(raw));
        if (result.getTotalScore() == null) {
            result.setTotalScore(0);
        }

        // 4. 落库（优化版简历由 optimize 阶段再生成）
        Long recordId;
        try {
            ResumeReview record = ResumeReview.builder()
                    .userId(userId)
                    .targetPosition(targetPosition)
                    .resumeText(resumeText)
                    .totalScore(result.getTotalScore())
                    .detailJson(objectMapper.writeValueAsString(result))
                    .createTime(LocalDateTime.now())
                    .build();
            resumeReviewMapper.insert(record);
            recordId = record.getId();
        } catch (Exception e) {
            log.warn("简历评分记录保存失败: {}", e.getMessage());
            recordId = null;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", recordId);
        data.put("result", result);
        return data;
    }

    /**
     * 生成优化版简历（第二步，2 分）：根据已保存的评分记录 + 简历原文，LLM 输出优化后的完整简历。
     * 已生成过直接返回（幂等，不重复扣分）。
     */
    public ResumeReviewResult optimize(Long userId, String username, Long recordId) {
        ResumeReview record = findOwned(userId, recordId);
        if (record == null) throw new BusinessException(404, "记录不存在");
        ResumeReviewResult result;
        try {
            result = objectMapper.readValue(record.getDetailJson(), ResumeReviewResult.class);
        } catch (Exception e) {
            throw new BusinessException("记录数据异常");
        }
        // 幂等：已生成优化版，直接返回不重复扣分
        if (result.getImprovedResume() != null && !result.getImprovedResume().isBlank()) {
            return result;
        }
        // 计费预扣：优化 2 分（失败退回）
        pointService.consumeForChat(username, 2, "AI 简历优化");

        String userText = "目标岗位：" + (record.getTargetPosition() == null || record.getTargetPosition().isBlank() ? "未指定" : record.getTargetPosition())
                + "\n简历原文：\n" + record.getResumeText()
                + "\n已有评分意见：" + (result.getSummary() == null ? "" : result.getSummary())
                + (result.getWeaknesses() != null && !result.getWeaknesses().isEmpty() ? "；主要不足：" + String.join("；", result.getWeaknesses()) : "");
        String raw;
        try {
            org.springframework.ai.chat.model.ChatResponse response =
                    chatModel.call(new Prompt(List.of(new SystemMessage(OPTIMIZE_PROMPT), new UserMessage(userText)),
                            ChatOptions.builder().model("qwen-plus").maxTokens(8192).build()));
            raw = extractReplyText(response);
        } catch (Exception e) {
            pointService.addPoints(userId, 2, "简历优化失败退回");
            log.error("简历优化调用模型失败", e);
            throw new BusinessException("优化服务暂时不可用，请稍后重试");
        }
        if (raw == null || raw.isBlank()) {
            pointService.addPoints(userId, 2, "简历优化失败退回");
            throw new BusinessException("优化失败，请重试");
        }
        result.setImprovedResume(filter(raw));
        try {
            ResumeReview update = new ResumeReview();
            update.setId(recordId);
            update.setDetailJson(objectMapper.writeValueAsString(result));
            resumeReviewMapper.updateById(update);
        } catch (Exception e) {
            log.warn("简历优化结果保存失败: {}", e.getMessage());
        }
        return result;
    }

    /** RAG 检索简历写作规范（失败降级返回空，不阻塞评分） */
    private String retrieveContext() {
        try {
            List<Document> docs = hybridRetriever.retrieve("简历撰写优化建议 项目经历量化 技能表达", 5);
            if (docs != null && !docs.isEmpty()) {
                return docs.stream()
                        .map(Document::getText)
                        .filter(t -> t != null && !t.isBlank())
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("");
            }
        } catch (Exception e) {
            log.warn("简历评分检索知识库失败，降级为无上下文评分: {}", e.getMessage());
        }
        return "";
    }

    /** JSON 解析（兼容 ```json 包裹；截断时截取到最后一个 } 保住评分维度） */
    private ResumeReviewResult parseResult(String raw) {
        String json = extractJson(raw);
        try {
            return objectMapper.readValue(json, ResumeReviewResult.class);
        } catch (Exception e) {
            try {
                int lastBrace = json.lastIndexOf('}');
                if (lastBrace > 0) {
                    return objectMapper.readValue(json.substring(0, lastBrace + 1), ResumeReviewResult.class);
                }
            } catch (Exception ignored) {
            }
            log.warn("简历评分 JSON 解析失败: {}", raw);
            throw new BusinessException("评分结果解析失败，请重试");
        }
    }

    /** 结果字段敏感词过滤 */
    private ResumeReviewResult sanitize(ResumeReviewResult result) {
        result.setSummary(filter(result.getSummary()));
        if (result.getDimensions() != null) {
            for (ResumeDimension d : result.getDimensions()) {
                d.setName(filter(d.getName()));
                d.setComment(filter(d.getComment()));
                d.setSuggestion(filter(d.getSuggestion()));
            }
        }
        if (result.getHighlights() != null) {
            result.setHighlights(result.getHighlights().stream().map(this::filter).toList());
        }
        if (result.getWeaknesses() != null) {
            result.setWeaknesses(result.getWeaknesses().stream().map(this::filter).toList());
        }
        return result;
    }

    /**
     * 历史评分概要列表（不含简历原文与详情 JSON）。
     */
    public List<Map<String, Object>> list(Long userId) {
        List<ResumeReview> rows = resumeReviewMapper.selectList(
                new LambdaQueryWrapper<ResumeReview>()
                        .eq(ResumeReview::getUserId, userId)
                        .orderByDesc(ResumeReview::getCreateTime));
        List<Map<String, Object>> data = new ArrayList<>(rows.size());
        for (ResumeReview r : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("targetPosition", r.getTargetPosition());
            item.put("totalScore", r.getTotalScore());
            item.put("createdAt", r.getCreateTime());
            data.add(item);
        }
        return data;
    }

    /**
     * 评分详情（含简历原文与完整明细 JSON），归属校验。
     */
    public Map<String, Object> detail(Long userId, Long id) {
        ResumeReview record = findOwned(userId, id);
        if (record == null) throw new BusinessException(404, "记录不存在");
        ResumeReviewResult result;
        try {
            result = objectMapper.readValue(record.getDetailJson(), ResumeReviewResult.class);
        } catch (Exception e) {
            log.warn("简历评分详情解析失败 id={}", id);
            throw new BusinessException("记录数据异常");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", record.getId());
        data.put("targetPosition", record.getTargetPosition());
        data.put("resumeText", record.getResumeText());
        data.put("createdAt", record.getCreateTime());
        data.put("result", result);
        return data;
    }

    /**
     * 删除评分记录（归属校验）。
     */
    public void delete(Long userId, Long id) {
        ResumeReview record = findOwned(userId, id);
        if (record == null) throw new BusinessException(404, "记录不存在");
        resumeReviewMapper.deleteById(record.getId());
    }

    private ResumeReview findOwned(Long userId, Long id) {
        if (userId == null || id == null) return null;
        return resumeReviewMapper.selectOne(
                new LambdaQueryWrapper<ResumeReview>()
                        .eq(ResumeReview::getUserId, userId)
                        .eq(ResumeReview::getId, id));
    }

    private String filter(String text) {
        if (text == null || sensitiveWordFilter == null || !sensitiveWordFilter.isLoaded()) return text;
        return sensitiveWordFilter.filter(text);
    }

    /** 从模型输出中提取文本（参照职规服务实现） */
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

    /** 从 LLM 输出中提取 JSON（去掉 ```json 包裹） */
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
