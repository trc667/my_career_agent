package com.example.aimaster.rag;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

/**
 * RAG 文档切割实现类，由你按 {@link #splitText(String)} 内逻辑注释实现具体代码。
 * <p>
 * 约定：按「块大小 + 重叠」把一段原文切分成多块字符串；Spring AI 的 TextSplitter 会对每个 Document 调
 * {@code splitText(doc.getText())}，再把每块转成 Document。实现后由 {@link com.example.aimaster.config.RagConfig}
 * 注入为 TextSplitter Bean，在 {@link RagDocumentLoader#loadAndIndex} 中会对「按双换行得到的段落」再做一次切分。
 * </p>
 */
public class RagTextSplitterImpl {

    /** 每块目标大小（字符数），超过则再切。 */
    private final int chunkSize;
    /** 相邻块之间的重叠字符数，避免语义在边界被截断。 */
    private final int chunkOverlap;

    public RagTextSplitterImpl(int chunkSize, int chunkOverlap) {
        this.chunkSize = Math.max(100, chunkSize);
        this.chunkOverlap = Math.max(0, Math.min(chunkOverlap, chunkSize - 1));
    }

    /**
     * 将一段原文按块大小与重叠切分成多块字符串（由你写代码）。
     * <p>
     * 实现逻辑：
     * </p>
     * <ol>
     *   <li>若 {@code text} 为空或空白，返回 {@link Collections#emptyList()}。</li>
     *   <li>新建 {@code List<String> result = new ArrayList<>()}。</li>
     *   <li>从 {@code start = 0} 开始循环：</li>
     *   <li>取结束下标 {@code end = Math.min(start + chunkSize, text.length())}，子串 {@code block = text.substring(start, end)} 加入 result。</li>
     *   <li>若 {@code end >= text.length()} 结束循环；否则 {@code start += (chunkSize - chunkOverlap)}，若 {@code start >= text.length()} 则结束，否则继续。</li>
     *   <li>返回 result。</li>
     * </ol>
     *
     * @param text 一段原文（可能很长）
     * @return 切分后的字符串列表，每块长度约 chunkSize，块间重叠 chunkOverlap
     */
    public List<String> splitText(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        int start = 0;
        int step = Math.max(1, chunkSize - chunkOverlap);
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            result.add(text.substring(start, end));
            if (end >= text.length()) {
                break;
            }
            start += step;
        }
        return result;
    }
}
