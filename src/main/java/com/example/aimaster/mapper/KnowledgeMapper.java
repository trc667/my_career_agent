package com.example.aimaster.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aimaster.entity.Knowledge;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库 Mapper，继承 BaseMapper 即拥有 insert/select 等 CRUD 方法。
 */
@Mapper
public interface KnowledgeMapper extends BaseMapper<Knowledge> {
}
