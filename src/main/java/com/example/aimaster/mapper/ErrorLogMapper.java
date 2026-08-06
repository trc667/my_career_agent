package com.example.aimaster.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aimaster.entity.ErrorLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 错误日志 Mapper（自建监控面板数据源）。
 */
@Mapper
public interface ErrorLogMapper extends BaseMapper<ErrorLog> {
}
