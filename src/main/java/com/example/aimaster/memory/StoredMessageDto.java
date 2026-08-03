package com.example.aimaster.memory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 持久化时单条消息的 DTO，用于 JSON 序列化/反序列化。
 * 只保留角色和文本内容，与 Spring AI 的 Message 互转由存储实现类完成。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoredMessageDto {

    /** 角色：user / assistant（与 MessageType 对应，系统消息一般不落库） */
    private String role;
    /** 消息正文 */
    private String content;
}
