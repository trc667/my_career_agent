package com.example.aimaster.controller;

import com.example.aimaster.dto.Result;
import com.example.aimaster.service.BaguService;
import com.example.aimaster.service.BaguService.BaguEntry;
import com.example.aimaster.service.BaguService.BaguPage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 八股知识库接口：浏览/搜索/随机抽题（内容来自 RAG 知识库 career-tips.txt，629 段）。
 */
@RestController
@RequestMapping("/api/bagu")
public class BaguController {

    private final BaguService baguService;

    public BaguController(BaguService baguService) {
        this.baguService = baguService;
    }

    /** GET /api/bagu/list?category=后端&keyword=锁&page=0&size=10 分类+关键词+分页 */
    @GetMapping("/list")
    public Result<BaguPage> list(@RequestParam(required = false) String category,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        size = Math.min(Math.max(size, 1), 50);
        return Result.ok(baguService.list(category, keyword, Math.max(page, 0), size));
    }

    /** GET /api/bagu/categories 各分类条目统计 */
    @GetMapping("/categories")
    public Result<List<Map<String, Object>>> categories() {
        return Result.ok(baguService.categories());
    }

    /** GET /api/bagu/random?category=后端 随机抽一条（可限定分类） */
    @GetMapping("/random")
    public Result<BaguEntry> random(@RequestParam(required = false) String category) {
        BaguEntry entry = baguService.random(category);
        if (entry == null) {
            return Result.fail(400, "该分类下暂无内容");
        }
        return Result.ok(entry);
    }
}
