package com.example.aimaster.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 手写工具：保存学习笔记、求职提醒等。模型可根据用户需求调用。
 */
@Component
public class NoteTool {

    private final ConcurrentHashMap<String, String> notes = new ConcurrentHashMap<>();

    @Tool(description = "将用户指定的学习笔记、求职提醒、面试要点等保存起来，参数 content 为要保存的文本内容")
    public String saveNote(String content) {
        if (content == null || content.isBlank()) {
            return "笔记内容不能为空，请提供要保存的文字。";
        }
        String key = "note_" + System.currentTimeMillis();
        notes.put(key, content.trim());
        return "已保存笔记：" + (content.length() > 50 ? content.substring(0, 50) + "..." : content);
    }
}
