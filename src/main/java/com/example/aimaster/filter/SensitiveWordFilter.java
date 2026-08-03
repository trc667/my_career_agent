package com.example.aimaster.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 敏感词过滤器（DFA 算法）。
 * 从词库文件加载敏感词，构建多叉树，一次扫描即可检测并替换所有敏感词。
 *
 * <p>使用方式：
 * <pre>
 * SensitiveWordFilter filter = new SensitiveWordFilter();
 * filter.loadFromClasspath("/sensitive-words.txt");
 * filter.setReplacement("***");
 * String filtered = filter.filter("原文包含敏感词");
 * </pre>
 */
public class SensitiveWordFilter {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordFilter.class);

    /** DFA 树节点：子节点为字符 -> 下一层 Node */
    private static class Node {
        final Map<Character, Node> children = new HashMap<>();
        boolean isEnd;
    }

    private final Node root = new Node();
    private String replacement = "***";
    private boolean loaded;

    /**
     * 设置替换字符串，默认 "***"
     */
    public void setReplacement(String replacement) {
        this.replacement = replacement != null ? replacement : "***";
    }

    /**
     * 从 classpath 资源加载词库（每行一词，UTF-8）
     */
    public void loadFromClasspath(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("敏感词库不存在: {}", resourcePath);
                return;
            }
            loadFromStream(is);
        } catch (IOException e) {
            log.warn("敏感词库加载失败: {}", e.getMessage());
        }
    }

    /**
     * 从文件路径加载词库
     */
    public void loadFromFile(Path path) {
        if (path == null || !Files.exists(path)) {
            log.warn("敏感词库文件不存在: {}", path);
            return;
        }
        try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
            lines.map(String::trim).filter(s -> !s.isEmpty()).forEach(this::addWord);
            loaded = true;
        } catch (IOException e) {
            log.warn("敏感词库加载失败: {}", e.getMessage());
        }
    }

    /**
     * 从输入流加载词库
     */
    public void loadFromStream(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.trim();
                if (!word.isEmpty() && !word.startsWith("#")) {
                    addWord(word);
                }
            }
            loaded = true;
        }
    }

    /**
     * 添加一个敏感词到 DFA 树
     */
    public void addWord(String word) {
        if (word == null || word.isEmpty()) return;
        Node cur = root;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            cur = cur.children.computeIfAbsent(c, k -> new Node());
        }
        cur.isEnd = true;
    }

    /**
     * 过滤文本：将命中敏感词替换为 replacement
     *
     * @param text 原始文本
     * @return 过滤后的文本；空词库或 text 为空时返回原文本
     */
    public String filter(String text) {
        if (text == null || text.isEmpty()) return text;
        if (!loaded || root.children.isEmpty()) return text;

        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int matchLen = findLongestMatch(text, i);
            if (matchLen > 0) {
                sb.append(replacement);
                i += matchLen;
            } else {
                sb.append(text.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * 从 start 位置开始查找最长匹配的敏感词长度
     */
    private int findLongestMatch(String text, int start) {
        Node node = root;
        int lastMatch = 0;
        for (int i = start; i < text.length(); i++) {
            Node next = node.children.get(text.charAt(i));
            if (next == null) break;
            if (next.isEnd) lastMatch = i - start + 1;
            node = next;
        }
        return lastMatch;
    }

    /**
     * 是否已加载词库
     */
    public boolean isLoaded() {
        return loaded && !root.children.isEmpty();
    }

    /**
     * 判断过滤后的文本是否「仅剩替换符」，即原内容被完全过滤。
     * 用于在用户输入全是敏感词时，直接返回友好提示而非调用模型。
     */
    public boolean isFullyFiltered(String filtered) {
        if (filtered == null || filtered.isEmpty()) {
            return true;
        }
        String r = replacement != null ? replacement : "***";
        String trimmed = filtered.trim();
        if (trimmed.isEmpty()) {
            return true;
        }
        return trimmed.replace(r, "").trim().isEmpty();
    }
}
