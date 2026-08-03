package com.example.aimaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 公告新增/更新请求体（管理后台用）。
 */
@Data
public class AnnouncementRequest {

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 128, message = "标题不超过128字")
    private String title;

    @NotBlank(message = "公告内容不能为空")
    @Size(max = 4000, message = "内容不超过4000字")
    private String content;
}
