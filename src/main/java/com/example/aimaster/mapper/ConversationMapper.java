package com.example.aimaster.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aimaster.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话 Mapper：会话元数据 CRUD（按 user_id 隔离，配合 LambdaQueryWrapper 使用）。
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}
