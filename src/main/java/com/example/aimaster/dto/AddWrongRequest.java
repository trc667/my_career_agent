package com.example.aimaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 加入错题本请求：前端随机抽题/列表点击「加入错题本」时提交。
 */
@Data
public class AddWrongRequest {

    /** 题目内容 hash（BaguEntry.id） */
    @NotBlank(message = "questionId 不能为空")
    @Size(max = 32, message = "questionId 长度不超过32")
    private String questionId;

    /** 题目分类（autoTag 结果，可选） */
    @Size(max = 32, message = "category 长度不超过32")
    private String category;

    /** 题目全文（冗余存储） */
    @NotBlank(message = "content 不能为空")
    @Size(max = 2000, message = "content 长度不超过2000")
    private String content;
}
