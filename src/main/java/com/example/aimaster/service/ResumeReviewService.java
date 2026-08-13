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

    private static final String SYSTEM_PROMPT_TEMPLATE =
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
            + "{\"totalScore\":78,\"summary\":\"总体评价\",\"dimensions\":[{\"name\":\"项目经历\",\"score\":82,\"comment\":\"现状评价\",\"suggestion\":\"改进建议\"}],\"highlights\":[\"亮点1\"],\"weaknesses\":[\"不足1\"],\"improvedResume\":\"基于原文优化后的完整简历全文，保留原信息结构，补充量化表述\"}\n"
            + "不要输出其他文字，不要用 ```json 包裹。";

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
    public ResumeReviewResult review(Long userId, String username, ResumeReviewRequest req) {
        String resumeText = filter(req.getResumeText() == null ? "" : req.getResumeText());
        if (resumeText == null || resumeText.isBlank()) {
            throw new BusinessException("简历内容不能为空");
        }
        String targetPosition = req.getTargetPosition() != null ? req.getTargetPosition().trim() : "";

        // 积分预检：余额 ≥1 分才放行（VIP/ADMIN 不检），防 0 分用户刷评分
        pointService.precheckFeature(username, "AI 简历评分");

        // 1. 检索知识库中的简历写作规范作为评分依据（失败降级跳过，不阻塞评分）
        String context = "";
        try {
            List<Document> docs = hybridRetriever.retrieve("简历撰写优化建议 项目经历量化 技能表达", 5);
            if (docs != null && !docs.isEmpty()) {
                context = docs.stream()
                        .map(Document::getText)
                        .filter(t -> t != null && !t.isBlank())
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("");
            }
        } catch (Exception e) {
            log.warn("简历评分检索知识库失败，降级为无上下文评分: {}", e.getMessage());
        }

        // 2. 调用模型生成结构化评分
        String systemPrompt = SYSTEM_PROMPT_TEMPLATE.replace("{context}", context.isBlank() ? "（无参考规范）" : context);
        String userText = (targetPosition.isBlank() ? "目标岗位：未指定" : "目标岗位：" + targetPosition)
                + "\n简历内容：\n" + resumeText;
        String raw;
        try {
            // maxTokens 8192：长简历要输出「优化版完整简历」，默认 2048 会被截断导致 JSON 不完整
            org.springframework.ai.chat.model.ChatResponse response =
                    chatModel.call(new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userText)),
                            ChatOptions.builder().model("qwen-plus").maxTokens(8192).build()));
            raw = extractReplyText(response);
            // 按实际 token 结算（usage 缺失按输出长度估算防白嫖），失败不影响主流程
            try {
                Integer usage = response != null && response.getMetadata() != null
                        && response.getMetadata().getUsage() != null
                        ? response.getMetadata().getUsage().getTotalTokens() : null;
                int tokens = (usage != null && usage > 0) ? usage : (raw == null ? 0 : raw.length() / 2);
                pointService.settleFeature(username, "AI 简历评分", "qwen-plus", tokens);
            } catch (Exception ex) {
                log.warn("简历评分结算失败: {}", ex.getMessage());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("简历评分调用模型失败", e);
            throw new BusinessException("评分服务暂时不可用，请稍后重试");
        }
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("评分失败，请重试");
        }

        // 3. 解析 JSON（兼容 ```json 包裹；超长输出被截断时截取到最后一个 } 保住评分维度）
        ResumeReviewResult result;
        try {
            String json = extractJson(raw);
            result = objectMapper.readValue(json, ResumeReviewResult.class);
        } catch (Exception e) {
            try {
                String json = extractJson(raw);
                int lastBrace = json.lastIndexOf('}');
                if (lastBrace > 0) {
                    result = objectMapper.readValue(json.substring(0, lastBrace + 1), ResumeReviewResult.class);
                } else {
                    throw e;
                }
            } catch (Exception e2) {
                log.warn("简历评分 JSON 解析失败: {}", raw);
                throw new BusinessException("评分结果解析失败，请重试");
            }
        }
        if (result.getTotalScore() == null) {
            result.setTotalScore(0);
        }
        result.setSummary(filter(result.getSummary()));
        result.setImprovedResume(filter(result.getImprovedResume()));
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

        // 4. 落库
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
        } catch (Exception e) {
            log.warn("简历评分记录保存失败: {}", e.getMessage());
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
