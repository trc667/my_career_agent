package com.example.aimaster.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aimaster.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper，继承 BaseMapper 即拥有 insert/selectOne/update 等 CRUD 方法。
 * 登录/注册用 LambdaQueryWrapper 即可，无需写 XML。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
