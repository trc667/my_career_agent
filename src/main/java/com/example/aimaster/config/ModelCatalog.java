package com.example.aimaster.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * 可选模型目录（模型切换 + 差异化计费核心配置）。
 * <p>
 * 设计（面试可讲）：
 * 1) 白名单机制：前端传模型名，后端只放行目录内模型，杜绝任意模型名透传（防刷/防错配）；
 * 2) 费率 = 积分/千 token，参考百炼公开定价折算并留积分倍数（签到 5 分/天，一次普通对话约 1-5 分）；
 * 3) 模型名透传 DashScope（百炼聚合 qwen 系 + DeepSeek），切换模型零新增 API Key；
 * 4) 后续模型费率要运营可调时，把 MODELS 迁移到 DB 表即可，调用方无感。
 */
@Component
public class ModelCatalog {

    /** 模型元信息 */
    public record ModelInfo(String id, String name, int rate, String desc, boolean defaultModel) {
    }

    /** 默认模型 */
    private static final String DEFAULT_MODEL = "qwen-plus";

    /** 模型 → 费率（积分/千 token）。仅开放已验证稳定的模型。 */
    private static final Map<String, ModelInfo> MODELS = new LinkedHashMap<>(Map.of(
            "qwen-turbo", new ModelInfo("qwen-turbo", "通义千问 Turbo", 1, "轻量快速 · 适合日常问答", false),
            "qwen-plus", new ModelInfo("qwen-plus", "通义千问 Plus", 2, "均衡旗舰 · 默认推荐", true),
            "deepseek-v3", new ModelInfo("deepseek-v3", "DeepSeek V3", 3, "开源大模型 · 性价比高", false),
            "qwen-max", new ModelInfo("qwen-max", "通义千问 Max", 5, "最强通义 · 深度分析", false),
            "deepseek-r1", new ModelInfo("deepseek-r1", "DeepSeek R1", 6, "深度推理 · 复杂问题", false)));

    /** 对前端开放的模型列表（含费率展示） */
    public List<Map<String, Object>> list() {
        List<Map<String, Object>> result = new ArrayList<>();
        MODELS.forEach((id, m) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.id());
            item.put("name", m.name());
            item.put("rate", m.rate());
            item.put("desc", m.desc());
            item.put("default", m.defaultModel());
            result.add(item);
        });
        return result;
    }

    /** 白名单校验：非法/空模型名回落默认模型 */
    public String resolve(String model) {
        if (model != null && MODELS.containsKey(model.trim())) {
            return model.trim();
        }
        return DEFAULT_MODEL;
    }

    /** 模型费率（积分/千 token），未知模型按默认模型费率 */
    public int rateOf(String model) {
        ModelInfo info = MODELS.get(resolve(model));
        return info != null ? info.rate() : MODELS.get(DEFAULT_MODEL).rate();
    }

    /** 模型展示名 */
    public String nameOf(String model) {
        ModelInfo info = MODELS.get(resolve(model));
        return info != null ? info.name() : MODELS.get(DEFAULT_MODEL).name();
    }
}
