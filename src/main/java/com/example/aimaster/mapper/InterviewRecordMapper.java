package com.example.aimaster.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aimaster.entity.InterviewRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 面试记录 Mapper。
 */
@Mapper
public interface InterviewRecordMapper extends BaseMapper<InterviewRecord> {
}
