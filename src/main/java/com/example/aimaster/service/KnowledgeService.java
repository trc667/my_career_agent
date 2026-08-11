package com.example.aimaster.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aimaster.dto.KnowledgeRequest;
import com.example.aimaster.entity.Knowledge;
import com.example.aimaster.exception.BusinessException;
import com.example.aimaster.mapper.KnowledgeMapper;
import com.example.aimaster.rag.HybridRetriever;
import com.example.aimaster.rag.RagDocumentLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * 知识库管理服务：以 DB（knowledge 表）为唯一事实源，管理后台在线增删改查。
 * <p>
 * 设计（面试可讲）：
 * 1) 事实源升级：原来从 career-tips.txt 文件读取（改文件要重启），现在首次启动把文件
 *    一次性导入 DB，之后以 DB 为准，管理后台在线编辑立即生效；
 * 2) 索引重建：增删改后异步触发三处检索源全量重建——pgvector 向量库、Lucene BM25、
 *    八股内存缓存，保证 RAG 检索 / 八股练习场数据一致；
 * 3) 并发安全：重建用 AtomicBoolean 防重入 + volatile 状态供前端轮询；
 *    单条 rebuild 失败不影响其余（各索引独立降级）。
 */
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    /** 分页结果 */
    public record KnowledgePage(List<Knowledge> list, long total) {
    }

    private final KnowledgeMapper knowledgeMapper;
    private final RagDocumentLoader ragDocumentLoader;
    private final HybridRetriever hybridRetriever;
    private final BaguService baguService;

    /** 重建线程池（单线程，避免并发重建） */
    private final ExecutorService rebuildExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean rebuilding = new AtomicBoolean(false);
    private volatile String rebuildStatus = "idle";      // idle/running/success/failed
    private volatile String rebuildInfo = "";            // 最近一次结果描述（时间/条数/错误）

    public KnowledgeService(KnowledgeMapper knowledgeMapper,
                            RagDocumentLoader ragDocumentLoader,
                            HybridRetriever hybridRetriever,
                            BaguService baguService) {
        this.knowledgeMapper = knowledgeMapper;
        this.ragDocumentLoader = ragDocumentLoader;
        this.hybridRetriever = hybridRetriever;
        this.baguService = baguService;
    }

    /** 启动初始化：表空则从 career-tips.txt 导入，再重建索引（RagInitRunner 调用，同步）。
     * 向量库持久化在 pgvector：非空则跳过 embedding 复用旧向量（启动从 30-40s 降到秒级、省 API 费），
     * 为空（首次/被清空）才全量向量化；知识变更仍由管理接口触发全量重建保证一致性。 */
    public void ensureInitialized() {
        long count = knowledgeMapper.selectCount(null);
        if (count == 0) {
            importFromFile();
        } else {
            log.info("知识库已存在 {} 条，跳过文件导入", count);
        }
        rebuildIndexesSync(true);
    }

    /** 从 career-tips.txt 一次性导入 DB（仅首次，之后以 DB 为唯一事实源） */
    private void importFromFile() {
        try {
            String content = new ClassPathResource("rag/career-tips.txt")
                    .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            List<String> paragraphs = RagDocumentLoader.splitParagraphs(content);
            LocalDateTime now = LocalDateTime.now();
            int inserted = 0;
            for (String p : paragraphs) {
                knowledgeMapper.insert(Knowledge.builder()
                        .category(RagDocumentLoader.autoTag(p))
                        .content(p)
                        .enabled(1)
                        .createTime(now)
                        .updateTime(now)
                        .build());
                inserted++;
            }
            log.info("知识库从文件导入完成：{} 条", inserted);
        } catch (Exception e) {
            log.error("知识库文件导入失败：{}", e.getMessage());
        }
    }

    /** 分页查询：分类 + 关键词 + 启用状态过滤 */
    public KnowledgePage list(String category, String keyword, Integer enabled, int page, int size) {
        LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<Knowledge>()
                .eq(category != null && !category.isBlank(), Knowledge::getCategory, category)
                .like(keyword != null && !keyword.isBlank(), Knowledge::getContent, keyword)
                .eq(enabled != null, Knowledge::getEnabled, enabled)
                .orderByDesc(Knowledge::getId);
        Long total = knowledgeMapper.selectCount(wrapper);
        List<Knowledge> list = knowledgeMapper.selectList(
                wrapper.last("LIMIT " + Math.max(0, page * size) + ", " + Math.max(1, size)));
        return new KnowledgePage(list, total);
    }

    /** 分类统计（前端筛选下拉） */
    public List<Map<String, Object>> categories() {
        Map<String, Long> countMap = new LinkedHashMap<>();
        for (Knowledge k : knowledgeMapper.selectList(null)) {
            countMap.merge(k.getCategory() == null ? "综合" : k.getCategory(), 1L, Long::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        countMap.forEach((cat, cnt) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("category", cat);
            m.put("count", cnt);
            result.add(m);
        });
        return result;
    }

    /** 新增知识段：分类为空则自动 autoTag，默认启用，随后异步重建索引 */
    public Knowledge create(KnowledgeRequest req) {
        String content = req.getContent().trim();
        String category = req.getCategory() != null && !req.getCategory().isBlank()
                ? req.getCategory().trim() : RagDocumentLoader.autoTag(content);
        LocalDateTime now = LocalDateTime.now();
        Knowledge k = Knowledge.builder()
                .category(category)
                .content(content)
                .enabled(req.getEnabled() == null || req.getEnabled() ? 1 : 0)
                .createTime(now)
                .updateTime(now)
                .build();
        knowledgeMapper.insert(k);
        log.info("知识库新增：id={} category={} len={}", k.getId(), category, content.length());
        rebuildAsync();
        return k;
    }

    /** 更新知识段（内容/分类/启用状态），随后异步重建索引 */
    public Knowledge update(Long id, KnowledgeRequest req) {
        Knowledge k = knowledgeMapper.selectById(id);
        if (k == null) throw new BusinessException("知识条目不存在");
        String content = req.getContent().trim();
        k.setContent(content);
        k.setCategory(req.getCategory() != null && !req.getCategory().isBlank()
                ? req.getCategory().trim() : RagDocumentLoader.autoTag(content));
        if (req.getEnabled() != null) k.setEnabled(req.getEnabled() ? 1 : 0);
        k.setUpdateTime(LocalDateTime.now());
        knowledgeMapper.updateById(k);
        log.info("知识库更新：id={}", id);
        rebuildAsync();
        return k;
    }

    /** 启停切换（停用段不进索引、八股列表隐藏） */
    public void setEnabled(Long id, boolean enabled) {
        Knowledge k = knowledgeMapper.selectById(id);
        if (k == null) throw new BusinessException("知识条目不存在");
        k.setEnabled(enabled ? 1 : 0);
        k.setUpdateTime(LocalDateTime.now());
        knowledgeMapper.updateById(k);
        log.info("知识库启停：id={} enabled={}", id, enabled);
        rebuildAsync();
    }

    /** 删除知识段，随后异步重建索引 */
    public void delete(Long id) {
        Knowledge k = knowledgeMapper.selectById(id);
        if (k == null) throw new BusinessException("知识条目不存在");
        knowledgeMapper.deleteById(id);
        log.info("知识库删除：id={}", id);
        rebuildAsync();
    }

    /** 触发异步全量重建（管理接口调用，立即返回；知识变更必须全量重建保证三处索引一致） */
    public void rebuildAsync() {
        if (rebuilding.compareAndSet(false, true)) {
            rebuildStatus = "running";
            rebuildInfo = "重建中，耗时约 1-2 分钟...";
            CompletableFuture.runAsync(() -> rebuildIndexesSync(false), rebuildExecutor);
        } else {
            log.info("知识库重建已在进行中，忽略本次触发");
        }
    }

    /** 同步重建三处检索源（启动 / 异步任务共用，需先通过 rebuilding 防重入）。
     * @param skipVectorIfExists 启动场景传 true：向量库非空则跳过 embedding（持久化复用）；管理接口传 false 强制全量。 */
    private void rebuildIndexesSync(boolean skipVectorIfExists) {
        long start = System.currentTimeMillis();
        boolean vectorSkipped = false;
        try {
            List<Knowledge> enabledList = knowledgeMapper.selectList(
                    new LambdaQueryWrapper<Knowledge>()
                            .eq(Knowledge::getEnabled, 1)
                            .orderByAsc(Knowledge::getId));
            List<String> paragraphs = enabledList.stream()
                    .map(Knowledge::getContent)
                    .filter(s -> s != null && !s.isBlank())
                    .toList();
            if (paragraphs.isEmpty()) {
                rebuildStatus = "failed";
                rebuildInfo = "没有启用的知识段，跳过重建";
                log.warn("知识库重建跳过：无启用知识段");
                return;
            }
            // 1) 向量库（pgvector，embedding 逐批入库，最耗时）：启动时非空则复用持久化向量
            if (skipVectorIfExists && !ragDocumentLoader.isVectorStoreEmpty()) {
                vectorSkipped = true;
                log.info("向量库非空，跳过 embedding（启动复用持久化向量）");
            } else {
                ragDocumentLoader.reloadFromParagraphs(paragraphs);
            }
            // 2) BM25 稀疏索引（Lucene 内存索引，毫秒级）
            hybridRetriever.rebuildBm25(paragraphs);
            // 3) 八股练习场内存缓存
            baguService.reload(paragraphs);
            rebuildStatus = "success";
            rebuildInfo = String.format("重建成功：%d 段，耗时 %.1fs%s", paragraphs.size(),
                    (System.currentTimeMillis() - start) / 1000.0,
                    vectorSkipped ? "（向量库已存在跳过 embedding）" : "");
            log.info("知识库全量重建完成：{} 段，耗时 {}ms{}", paragraphs.size(),
                    System.currentTimeMillis() - start,
                    vectorSkipped ? "（向量库已存在跳过 embedding）" : "");
        } catch (Exception e) {
            rebuildStatus = "failed";
            rebuildInfo = "重建失败：" + e.getMessage();
            log.error("知识库全量重建失败", e);
        } finally {
            rebuilding.set(false);
        }
    }

    /** 重建状态（供前端轮询/展示） */
    public Map<String, Object> rebuildStatus() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rebuilding", rebuilding.get());
        m.put("status", rebuildStatus);
        m.put("info", rebuildInfo);
        return m;
    }
}
