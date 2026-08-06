package com.example.aimaster.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aimaster.entity.ResumeReview;
import org.apache.ibatis.annotations.Mapper;

/**
 * 简历评分记录 Mapper（按 user_id 隔离，配合 LambdaQueryWrapper 使用）。
 */
@Mapper
public interface ResumeReviewMapper extends BaseMapper<ResumeReview> {
}
