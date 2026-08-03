package com.example.aimaster.memory;

import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多轮对话记忆的简单内存实现。
 * 按会话 ID 存储消息列表，并限制单会话最大条数，超出时丢弃最早的消息。
 * 由 {@link com.example.aimaster.config.MemoryConfig} 根据配置装配，不单独 @Component。
 */
public class InMemoryConversationMemoryStore implements ConversationMemoryStore {

    /** 每个会话最多保留的消息条数（user+assistant 合计） */
    private final int maxMessagesPerConversation;
    private final Map<String, List<Message>> store = new ConcurrentHashMap<>();

    public InMemoryConversationMemoryStore(int maxMessagesPerConversation) {
        this.maxMessagesPerConversation = maxMessagesPerConversation;
    }

    @Override
    public List<Message> getMessages(String conversationId) {
        List<Message>list=store.get(conversationId);
       if (list==null||list.isEmpty()) {
        return List.of();
        
       }
        return List.copyOf(list);
    }

    @Override
    public void add(String conversationId, Message message) {
        // 先取出该会话的消息列表，没有就新建一个并放进 store
       List<Message>list= store.get(conversationId);
       if (list==null) {
        list=new ArrayList<>();
        store.put(conversationId, list);
       }
       list.add(message);

        // 如果条数超过上限，从最前面删掉多余的，只保留最近 maxMessagesPerConversation 条
        if (list.size() > maxMessagesPerConversation) {
            int removeCount = list.size() - maxMessagesPerConversation;
            list.subList(0, removeCount).clear();
        }
    }

    @Override
    public void clear(String conversationId) {
        store.remove(conversationId);
    }
}
