package com.example.aimaster.service.impl;

import com.example.aimaster.dto.AuthResponse;
import com.example.aimaster.dto.LoginRequest;
import com.example.aimaster.dto.RegisterRequest;
import com.example.aimaster.entity.User;
import com.example.aimaster.exception.BusinessException;
import com.example.aimaster.mapper.UserMapper;
import com.example.aimaster.security.JwtUtil;
import com.example.aimaster.service.AuthService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 认证服务实现：登录、注册、JWT 签发。
 * 【由你实现】login() 和 register() 的逻辑。
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserMapper userMapper,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 登录：根据 username 查用户 → 校验密码 → 生成 JWT → 返回 AuthResponse。
     * 用户不存在或密码错误时抛出 BusinessException("用户名或密码错误")。
     */
    @Override
    public AuthResponse login(LoginRequest req) {
        // TODO: 1. 用 userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername())) 查用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        boolean matches = passwordEncoder.matches(req.getPassword(), user.getPasswordHash());
        if (!matches) {
            throw new BusinessException("用户名或密码错误");
        }
        // TODO: 4. 用 jwtUtil.generateToken(user.getUsername()) 生成 token
        String token = jwtUtil.generateToken(user.getUsername());
        // TODO: 5. 返回 AuthResponse.builder().token(token).username(user.getUsername()).build()
        return AuthResponse.builder().token(token).username(user.getUsername()).build();
    }

    /**
     * 注册：先查用户名是否已存在 → 不存在则加密密码、入库。不返回 token。
     * 用户名已存在时抛出 BusinessException("用户名已存在")。
     */
    @Override
    public void register(RegisterRequest req) {
        User existingUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (existingUser != null) {
            throw new BusinessException("用户名已存在");
        }
        String encode = passwordEncoder.encode(req.getPassword());
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPasswordHash(encode);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
    }

    @Override
    public User getUserInfo(String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    @Override
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = getUserInfo(username);
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException("旧密码不正确");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }
}
