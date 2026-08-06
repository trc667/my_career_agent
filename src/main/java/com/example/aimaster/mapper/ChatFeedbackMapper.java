package com.example.aimaster.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aimaster.entity.ChatFeedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天问答反馈 Mapper。
 */
@Mapper
public interface ChatFeedbackMapper extends BaseMapper<ChatFeedback> {
}
