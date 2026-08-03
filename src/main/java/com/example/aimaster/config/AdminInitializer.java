package com.example.aimaster.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aimaster.entity.User;
import com.example.aimaster.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 管理员初始化：启动时把 app.admin.username 指定的用户提升为 ADMIN 角色。
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserMapper userMapper;

    @Value("${app.admin.username:}")
    private String adminUsername;

    public AdminInitializer(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminUsername == null || adminUsername.isBlank()) {
            log.info("未配置 app.admin.username，跳过管理员初始化");
            return;
        }
        String name = adminUsername.trim();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, name));
        if (user == null) {
            log.warn("管理员用户 {} 不存在，请先注册该账号再重启以提升为管理员", name);
            return;
        }
        if (!"ADMIN".equals(user.getRole())) {
            user.setRole("ADMIN");
            userMapper.updateById(user);
            log.info("已将 {} 提升为管理员", name);
        } else {
            log.info("{} 已是管理员", name);
        }
    }
}
