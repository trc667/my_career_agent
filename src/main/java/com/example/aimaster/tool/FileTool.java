package com.example.aimaster.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 文件保存工具：将职业规划、技能清单等保存为本地文件。
 */
@Component
public class FileTool {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Value("${app.file-save.base-dir:./data/files}")
    private String baseDir;

    @Tool(description = "将职业规划、技能清单、学习计划等文本保存为本地文件，支持 txt、md 格式")
    public String saveToFile(
            @ToolParam(description = "要保存的文本内容", required = true) String content,
            @ToolParam(description = "文件名，如 career-plan.txt、skills.md，可选", required = false) String filename) {
        if (content == null || content.isBlank()) {
            return "内容不能为空，请提供要保存的文字。";
        }
        try {
            Path dir = Paths.get(baseDir).toAbsolutePath();
            Files.createDirectories(dir);
            String name = (filename != null && !filename.isBlank())
                    ? filename.trim()
                    : "plan_" + LocalDateTime.now().format(FORMAT) + ".txt";
            if (!name.endsWith(".txt") && !name.endsWith(".md")) {
                name += ".txt";
            }
            Path file = dir.resolve(sanitizeFilename(name));
            Files.writeString(file, content.trim(), StandardCharsets.UTF_8);
            return "已保存到: " + file.toString();
        } catch (IOException e) {
            return "保存失败: " + e.getMessage();
        }
    }

    private String sanitizeFilename(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
