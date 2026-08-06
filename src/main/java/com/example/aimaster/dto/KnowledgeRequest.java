package com.example.aimaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识库条目新增/更新请求体。
 */
@Data
public class KnowledgeRequest {

    /** 知识段正文（必填） */
    @NotBlank(message = "知识内容不能为空")
    @Size(max = 4000, message = "单条知识不超过4000字")
    private String content;

    /** 分类（可选，空则后端自动 autoTag） */
    @Size(max = 32, message = "分类名不超过32字符")
    private String category;

    /** 是否启用（新增时默认启用；更新时可一并调整） */
    private Boolean enabled;
}
