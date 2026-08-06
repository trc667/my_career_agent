package com.example.aimaster.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aimaster.entity.ConversationMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话消息 Mapper：聊天历史完整落库 CRUD。
 */
@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessage> {
}
