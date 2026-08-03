package com.example.aimaster.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aimaster.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公告 Mapper，继承 BaseMapper 即拥有 insert/select 等 CRUD 方法。
 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}
