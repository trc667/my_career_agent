package com.example.aimaster.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rerank 精排器：调用 DashScope gte-rerank 交叉编码器，对多路召回融合后的候选文档重新打分排序。
 * <p>
 * 设计（面试可讲）：
 * 1) 为什么需要 Rerank：向量检索（双编码器）和 BM25 各自独立打分，RRF 融合只是"按排名粗排"；
 *    精排用交叉编码器（query 与 document 拼接后一起过模型）能捕捉细粒度语义交互，
 *    提升 top 排序质量（Recall@1 / MRR），这是 2025 年 RAG 架构"多路召回 → RRF 粗排 → 精排"的标配。
 * 2) API 调用：DashScope text-rerank 服务（gte-rerank 模型），HTTP POST + Bearer Token。
 * 3) 容错降级：API 超时/限流/报错时直接返回原顺序（不阻断主链路）。
 */
@Slf4j
@Component
public class Reranker {

    /** DashScope 文本重排服务端点 */
    private static final String RERANK_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank";

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 精排调用间隔（毫秒）：qwen3-rerank 有 QPS 限制，串行评估时加限速防 429 */
    private final long callIntervalMs;

    public Reranker(@Value("${spring.ai.dashscope.api-key:}") String apiKey,
                    @Value("${app.rag.rerank-model:qwen3-rerank}") String model,
                    @Value("${app.rag.rerank-interval-ms:300}") long callIntervalMs) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.callIntervalMs = Math.max(0, callIntervalMs);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 精排：按 rerank 分数降序返回 topN 条候选文档。
     *
     * @param query      原始用户问题
     * @param candidates 多路召回融合后的候选（最多取前 N 条送入精排）
     * @param topN       返回条数
     * @return 精排后的文档列表（API 失败时降级返回原顺序）
     */
    public List<Document> rerank(String query, List<Document> candidates, int topN) {
        if (query == null || query.isBlank() || candidates == null || candidates.isEmpty()) {
            return candidates == null ? List.of() : candidates;
        }
        if (apiKey.isEmpty()) {
            log.warn("Rerank 未配置 api-key，降级返回原顺序");
            return candidates;
        }
        try {
            // 限速：两次调用间隔，避免触发 QPS 限制（429）
            if (callIntervalMs > 0) {
                Thread.sleep(callIntervalMs);
            }
            // 构造请求体：{model, input:{query, documents}, parameters:{top_n}}
            Map<String, Object> body = new LinkedHashMap<>();
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("query", query);
            input.put("documents", candidates.stream().map(Document::getText).toList());
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("top_n", Math.min(topN, candidates.size()));
            body.put("model", model);
            body.put("input", input);
            body.put("parameters", params);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RERANK_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Rerank API 返回 {}：{}，降级返回原顺序", response.statusCode(), response.body());
                return candidates;
            }

            // 解析 output.results[]: {"index": i, "relevance_score": x}
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode results = root.path("output").path("results");
            List<Document> reranked = new ArrayList<>();
            for (JsonNode r : results) {
                int idx = r.path("index").asInt(-1);
                if (idx >= 0 && idx < candidates.size()) {
                    reranked.add(candidates.get(idx));
                }
            }
            return reranked.isEmpty() ? candidates : reranked;
        } catch (Exception e) {
            log.warn("Rerank 调用失败（降级返回原顺序）：{}", e.getMessage());
            return candidates;
        }
    }
}
