package com.example.aimaster.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aimaster.dto.CreateConversationRequest;
import com.example.aimaster.dto.RenameConversationRequest;
import com.example.aimaster.dto.Result;
import com.example.aimaster.entity.Conversation;
import com.example.aimaster.entity.ConversationMessage;
import com.example.aimaster.entity.User;
import com.example.aimaster.mapper.ConversationMapper;
import com.example.aimaster.mapper.ConversationMessageMapper;
import com.example.aimaster.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 聊天历史管理接口（均需登录，数据按当前用户隔离）。
 * <p>
 * - conversation_id 为后端生成的 UUID，也是对话记忆（conversation_message）的会话主键；
 * - 创建/重命名/删除/拉取消息均校验会话归属当前用户。
 */
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final AuthService authService;
    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper messageMapper;

    public ConversationController(AuthService authService,
                                  ConversationMapper conversationMapper,
                                  ConversationMessageMapper messageMapper) {
        this.authService = authService;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    /** 从 JWT 上下文解析当前用户名 */
    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "";
    }

    /** 当前用户 ID（未登录返回 null） */
    private Long currentUserId() {
        User user = authService.getUserInfo(currentUsername());
        return user != null ? user.getId() : null;
    }

    /** GET /api/conversations 当前用户的会话列表（不含消息，按更新时间倒序） */
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        List<Conversation> list = conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUserId, userId)
                        .orderByDesc(Conversation::getUpdateTime));
        List<Map<String, Object>> data = new ArrayList<>(list.size());
        for (Conversation c : list) {
            data.add(toSummary(c));
        }
        return Result.ok(data);
    }

    /** POST /api/conversations 创建会话（后端生成 conversation_id） */
    @PostMapping
    public Result<Map<String, Object>> create(@Valid @RequestBody(required = false) CreateConversationRequest req) {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        String title = (req != null && req.getTitle() != null && !req.getTitle().isBlank())
                ? req.getTitle().trim() : "新的职规咨询";
        Conversation conv = Conversation.builder()
                .userId(userId)
                .conversationId(UUID.randomUUID().toString())
                .title(title)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        conversationMapper.insert(conv);
        return Result.ok(toSummary(conv));
    }

    /** PUT /api/conversations/{conversationId}/rename 重命名会话 */
    @PutMapping("/{conversationId}/rename")
    public Result<Void> rename(@PathVariable String conversationId,
                               @Valid @RequestBody RenameConversationRequest req) {
        Conversation conv = findOwned(conversationId);
        if (conv == null) return Result.fail(404, "会话不存在");
        conv.setTitle(req.getTitle().trim());
        conversationMapper.updateById(conv);
        return Result.ok("重命名成功", null);
    }

    /** DELETE /api/conversations/{conversationId} 删除会话（元数据 + 全部消息） */
    @DeleteMapping("/{conversationId}")
    public Result<Void> delete(@PathVariable String conversationId) {
        Conversation conv = findOwned(conversationId);
        if (conv == null) return Result.fail(404, "会话不存在");
        conversationMapper.deleteById(conv.getId());
        messageMapper.delete(new LambdaQueryWrapper<ConversationMessage>()
                .eq(ConversationMessage::getConversationId, conversationId));
        return Result.ok("删除成功", null);
    }

    /** GET /api/conversations/{conversationId}/messages 拉取完整消息（跨设备回看） */
    @GetMapping("/{conversationId}/messages")
    public Result<List<Map<String, Object>>> messages(@PathVariable String conversationId) {
        Conversation conv = findOwned(conversationId);
        if (conv == null) return Result.fail(404, "会话不存在");
        List<ConversationMessage> rows = messageMapper.selectList(
                new LambdaQueryWrapper<ConversationMessage>()
                        .eq(ConversationMessage::getConversationId, conversationId)
                        .orderByAsc(ConversationMessage::getCreateTime));
        List<Map<String, Object>> data = new ArrayList<>(rows.size());
        for (ConversationMessage m : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("role", m.getRole());
            item.put("content", m.getContent());
            item.put("createdAt", m.getCreateTime());
            data.add(item);
        }
        return Result.ok(data);
    }

    /** 按当前用户归属查询会话（不存在或不属于当前用户返回 null） */
    private Conversation findOwned(String conversationId) {
        Long userId = currentUserId();
        if (userId == null || conversationId == null || conversationId.isBlank()) return null;
        return conversationMapper.selectOne(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUserId, userId)
                        .eq(Conversation::getConversationId, conversationId));
    }

    private Map<String, Object> toSummary(Conversation c) {
        Map<String, Object> map = new HashMap<>();
        map.put("conversationId", c.getConversationId());
        map.put("title", c.getTitle());
        map.put("createdAt", c.getCreateTime());
        map.put("updatedAt", c.getUpdateTime());
        return map;
    }
}
