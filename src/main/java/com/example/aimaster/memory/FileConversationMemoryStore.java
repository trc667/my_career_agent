package com.example.aimaster.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.MessageType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 对话记忆的文件持久化实现。
 * <p>
 * 约定：
 * <ul>
 *   <li>每个会话一个 JSON 文件，文件名由 conversationId 转成安全文件名（建议只保留字母数字和短横线，避免路径注入）。</li>
 *   <li>文件内容为 {@link StoredMessageDto} 的列表，可用 ObjectMapper 序列化/反序列化。</li>
 *   <li>单会话条数超过 maxMessagesPerConversation 时，丢弃最早的消息再写回。</li>
 *   <li>getMessages 在文件不存在或读失败时返回空列表，不要抛异常。</li>
 * </ul>
 */
public class FileConversationMemoryStore implements ConversationMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(FileConversationMemoryStore.class);
    /** 反序列化时用：TypeReference&lt;List&lt;StoredMessageDto&gt;&gt; */
    protected static final TypeReference<List<StoredMessageDto>> LIST_TYPE = new TypeReference<>() {};

    /** 每个会话最多保留的消息条数（由 MemoryConfig 传入） */
    private final int maxMessagesPerConversation;
    /** 存储目录，例如 ./data/chat-memory */
    private final Path baseDir;
    private final ObjectMapper objectMapper;

    public FileConversationMemoryStore(Path baseDir, ObjectMapper objectMapper, int maxMessagesPerConversation) {
        this.baseDir = baseDir;
        this.objectMapper = objectMapper;
        this.maxMessagesPerConversation = maxMessagesPerConversation;
    }

    /**
     * 将会话 ID 转为安全文件名（建议只保留 [a-zA-Z0-9-]，其余替换为 _，最后加 .json）。
     * 由你实现。
     */
    protected String toSafeFileName(String conversationId) {
        // 空则返回默认文件名；否则把非字母数字非横线的字符替换成 _，最后加 .json 返回
        if (conversationId == null || conversationId.isBlank()) return "default.json";
        return conversationId.replaceAll("[^a-zA-Z0-9\\-]", "_") + ".json";
    }

    /**
     * 从 Message 取出纯文本（UserMessage/AssistantMessage 可用 getText() 或按类型强转取内容）。
     * 由你实现。
     */
    protected static String getMessageText(Message message) {
        // 先尝试 message.getText()；没有则按 UserMessage/AssistantMessage 强转取 getText()
        if (message == null) return "";
        try {
            String t = message.getText();
            return t != null ? t : "";
        } catch (Exception e) {
            if (message instanceof UserMessage u) return u.getText() != null ? u.getText() : "";
            if (message instanceof AssistantMessage a) return a.getText() != null ? a.getText() : "";
            return "";
        }
    }

    @Override
    public List<Message> getMessages(String conversationId) {
        List<StoredMessageDto> dtos = loadDtos(conversationId);
        List<Message> result = new ArrayList<>();
        for (StoredMessageDto dto : dtos) {
            Message msg = toMessage(dto);
            if (msg != null) result.add(msg);
        }
        return result;
    }

    @Override
    public void add(String conversationId, Message message) {
        List<StoredMessageDto> list = new ArrayList<>(loadDtos(conversationId));
        list.add(toDto(message));
        if (list.size() > maxMessagesPerConversation) {
            list = new ArrayList<>(list.subList(list.size() - maxMessagesPerConversation, list.size()));
        }
        saveDtos(conversationId, list);
    }

    @Override
    public void clear(String conversationId) {
        Path file = baseDir.resolve(toSafeFileName(conversationId));
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("清除会话文件失败: {}", file, e);
        }
    }

    protected List<StoredMessageDto> loadDtos(String conversationId) {
        Path file = baseDir.resolve(toSafeFileName(conversationId));
        if (!Files.isRegularFile(file)) return Collections.emptyList();
        try {
            String json = Files.readString(file);
            List<StoredMessageDto> list = objectMapper.readValue(json, LIST_TYPE);
            return list != null ? list : Collections.emptyList();
        } catch (IOException e) {
            log.warn("读取会话文件失败: {}", file, e);
            return Collections.emptyList();
        }
    }

    protected void saveDtos(String conversationId, List<StoredMessageDto> list) {
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            log.warn("创建存储目录失败: {}", baseDir, e);
            return;
        }
        Path file = baseDir.resolve(toSafeFileName(conversationId));
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(list);
            Files.writeString(file, json);
        } catch (IOException e) {
            log.warn("写入会话文件失败: {}", file, e);
        }
    }

    /**
     * Message → StoredMessageDto（role 可用 message.getMessageType() 判断 USER/ASSISTANT，content 用 getMessageText）。
     * 由你实现。
     */
    protected static StoredMessageDto toDto(Message message) {
        // 根据 message.getMessageType() 设 role（ASSISTANT→"assistant"，否则 "user"），content 用 getMessageText(message)
        if (message == null) return new StoredMessageDto("user", "");
        String role = message.getMessageType() == MessageType.ASSISTANT ? "assistant" : "user";
        return new StoredMessageDto(role, getMessageText(message));
    }

    /**
     * StoredMessageDto → Message（仅处理 user/assistant，content 用 dto.getContent()，其它可返回 null）。
     * 由你实现。
     */
    protected static Message toMessage(StoredMessageDto dto) {
        // role 为 "user" 返回 new UserMessage(content)，"assistant" 返回 new AssistantMessage(content)，其它返回 null
        if (dto == null || dto.getRole() == null) return null;
        String content = dto.getContent() != null ? dto.getContent() : "";
        return switch (dto.getRole().toLowerCase()) {
            case "user" -> new UserMessage(content);
            case "assistant" -> new AssistantMessage(content);
            default -> null;
        };
    }
}
