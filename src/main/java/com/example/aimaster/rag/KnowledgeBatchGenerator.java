package com.example.aimaster.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库批量生成器：用 LLM（DashScope qwen-plus）按主题批量撰写知识段，
 * 解决手工维护 career-tips.txt 效率低的问题。
 * <p>
 * 设计（面试可讲）：
 * 1) 自动化知识构建流水线——主题清单 → LLM 批量生成 → 结构化 JSON 解析 → 落盘待合并，
 *    一次运行可产出数百条知识段，替代逐条手写。
 * 2) 防重复机制——生成前读取现有知识库片段摘录喂给 LLM 参考，
 *    且同一主题分批生成时把"已生成片段"一并传入，避免批次间内容重叠。
 * 3) 输出兜底——LLM 偶发输出被 ```json 包裹或格式漂移时，先剥包裹再按 JSON 数组解析，
 *    解析失败退化为按行提取，保证流水线不中断。
 * <p>
 * 触发入口见 {@link RagBatchGenRunner}（仅 raggen profile 下执行）。
 */
@Slf4j
@Component
public class KnowledgeBatchGenerator {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 生成结果输出文件（target 目录，不入库，review 后手动合并进 career-tips.txt） */
    @Value("${app.rag-gen.output-file:target/generated-knowledge.txt}")
    private String outputFile;

    /** 是否直接合并进知识库文件（true 时生成完成后自动追加到 src/main/resources/rag/career-tips.txt） */
    @Value("${app.rag-gen.merge-to-kb:false}")
    private boolean mergeToKb;

    public KnowledgeBatchGenerator(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /** 系统提示：知识段撰写规范（与 career-tips.txt 现有段落风格保持一致） */
    private static final String GEN_SYSTEM_PROMPT = """
            你是「AI 计算机学生职规大师」知识库的资深编辑，负责为知识库批量撰写知识段。

            ## 知识段撰写规范
            1. 每段 100~200 字，一段一个完整知识点，可直接作为检索答案。
            2. 语言务实、具体、口语化，像有经验的学长分享经验，避免空泛鸡汤。
            3. 包含关键技术术语、要点、步骤或示例，具备可操作性。
            4. 面向计算机专业学生，围绕求职、学习、面试、技术八股等场景。
            5. 技术内容必须准确，不编造事实，不臆造 API、版本号或数据。
            6. 段落之间内容不要重复，也不要与"已有片段"重复。

            ## 输出格式（严格遵守）
            只输出一个 JSON 字符串数组，每个元素是一条知识段（纯文本，不加编号前缀）。
            不要输出 ```json 包裹、不要输出任何解释文字。
            """;

    /** 批量生成并写入输出文件 */
    public void generate(List<String> topics, int perTopicCount, int batchSize) {
        List<String> existing = loadExistingSnippets();
        List<String> all = new ArrayList<>();
        log.info("开始批量生成：{} 个主题，每主题 {} 段，预计 {} 段", topics.size(), perTopicCount, topics.size() * perTopicCount);
        for (String topic : topics) {
            List<String> generated = generateForTopic(topic, perTopicCount, batchSize, existing);
            log.info("主题「{}」生成完成：{} 段", topic, generated.size());
            all.addAll(generated);
        }
        writeOutput(all);
        log.info("全部完成：共 {} 段，已写入 {}", all.size(), outputFile);
        if (mergeToKb) {
            appendToKnowledgeBase(all);
        }
    }

    /**
     * 直接把新生成段落追加进知识库文件（供大规模扩容时避免手工合并）。
     * 路径相对运行目录（项目根），与测试/开发一致。
     */
    private void appendToKnowledgeBase(List<String> paragraphs) {
        if (paragraphs.isEmpty()) {
            log.warn("没有生成任何段落，跳过合并");
            return;
        }
        try {
            Path kb = Paths.get("src/main/resources/rag/career-tips.txt");
            StringBuilder sb = new StringBuilder();
            if (Files.exists(kb) && Files.size(kb) > 0
                    && !Files.readString(kb, StandardCharsets.UTF_8).endsWith("\n\n")) {
                sb.append("\n");
            }
            for (String p : paragraphs) sb.append(p).append("\n\n");
            Files.writeString(kb, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.info("已合并 {} 段到知识库 {}", paragraphs.size(), kb.toAbsolutePath());
        } catch (IOException e) {
            log.error("合并进知识库失败：{}", e.getMessage());
        }
    }

    /** 单个主题：分批调用 LLM，直到凑够目标数量（或连续空批提前停止） */
    private List<String> generateForTopic(String topic, int target, int batchSize, List<String> existing) {
        List<String> result = new ArrayList<>();
        int round = 0;
        while (result.size() < target) {
            int need = Math.min(batchSize, target - result.size());
            List<String> batch = callLlm(topic, need, existing, result);
            if (batch.isEmpty()) {
                log.warn("主题「{}」第 {} 批生成 0 段，提前停止该主题", topic, round + 1);
                break;
            }
            result.addAll(batch);
            round++;
        }
        return result;
    }

    /** 调用 qwen-plus 生成一批知识段，返回解析后的段落列表 */
    private List<String> callLlm(String topic, int count, List<String> existing, List<String> already) {
        StringBuilder user = new StringBuilder();
        user.append("## 主题\n").append(topic).append("\n\n");
        user.append("请围绕该主题生成 ").append(count).append(" 条知识段，每条 100~200 字。\n\n");
        if (!existing.isEmpty()) {
            user.append("## 知识库已有片段（开头摘录，请避免与它们重复，可从不同角度深化或补充）\n");
            for (String s : existing) user.append("- ").append(s).append('\n');
        }
        if (!already.isEmpty()) {
            user.append("## 本主题已生成片段（请继续生成不同内容，不要重复）\n");
            for (String s : already) user.append("- ").append(snippet(s)).append('\n');
        }
        user.append("\n请直接输出 JSON 数组。");

        ChatResponse resp = chatModel.call(new Prompt(List.of(
                new SystemMessage(GEN_SYSTEM_PROMPT),
                new UserMessage(user.toString()))));
        String text = resp != null && resp.getResult() != null ? resp.getResult().getOutput().getText() : null;
        if (text == null || text.isBlank()) {
            log.warn("LLM 返回为空（主题：{}）", topic);
            return List.of();
        }
        return parseJsonArray(text);
    }

    /** 解析 LLM 输出为段落列表：先剥代码块包裹，再按 JSON 数组解析，失败退化为按行提取 */
    private List<String> parseJsonArray(String text) {
        String cleaned = stripCodeFence(text.trim());
        try {
            List<String> list = objectMapper.readValue(cleaned, new TypeReference<List<String>>() {
            });
            if (list != null && !list.isEmpty()) {
                return list.stream()
                        .filter(s -> s != null && !s.isBlank())
                        .map(String::trim)
                        .toList();
            }
        } catch (Exception e) {
            log.warn("JSON 数组解析失败：{}，退化按行提取", e.getMessage());
        }
        return cleaned.lines()
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.startsWith("[") && !s.startsWith("]") && !s.contains("\""))
                .toList();
    }

    /** 去掉 ```json ... ``` 代码块包裹 */
    private String stripCodeFence(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```(json)?\\s*", "").replaceFirst("```\\s*$", "").trim();
        }
        return t;
    }

    /** 追加写入输出文件（已存在则继续追加，带分隔空行） */
    private void writeOutput(List<String> paragraphs) {
        if (paragraphs.isEmpty()) {
            log.warn("没有生成任何段落，跳过写入");
            return;
        }
        try {
            Path path = Paths.get(outputFile);
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            StringBuilder sb = new StringBuilder();
            if (!Files.exists(path)) {
                sb.append("# LLM 批量生成知识段（KnowledgeBatchGenerator 产物，review 后合并进 career-tips.txt）\n\n");
            }
            for (String p : paragraphs) sb.append(p).append("\n\n");
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("写入输出文件失败：{}", e.getMessage());
        }
    }

    /** 读取现有知识库每段开头摘录，供 LLM 参考防重复（最多 50 条） */
    private List<String> loadExistingSnippets() {
        try {
            String content = new ClassPathResource("rag/career-tips.txt").getContentAsString(StandardCharsets.UTF_8);
            List<String> snippets = new ArrayList<>();
            for (String part : content.split("\\n\\s*\\n")) {
                String s = part.trim();
                if (s.isEmpty() || s.startsWith("#")) continue;
                snippets.add(snippet(s));
                if (snippets.size() >= 50) break;
            }
            return snippets;
        } catch (IOException e) {
            log.warn("读取现有知识库失败：{}", e.getMessage());
            return List.of();
        }
    }

    /** 取段落开头 25 字作为防重复参考摘录 */
    private String snippet(String text) {
        String oneLine = text.replace('\n', ' ');
        return oneLine.length() > 25 ? oneLine.substring(0, 25) + "…" : oneLine;
    }
}
