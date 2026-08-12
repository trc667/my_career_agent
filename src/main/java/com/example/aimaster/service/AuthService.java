package com.example.aimaster.service;

import com.example.aimaster.dto.AuthResponse;
import com.example.aimaster.dto.LoginRequest;
import com.example.aimaster.dto.RegisterRequest;
import com.example.aimaster.entity.User;

/**
 * 认证服务接口：登录、注册、个人中心。
 */
public interface AuthService {

    AuthResponse login(LoginRequest req);

    /** 注册：仅入库，不返回 token，需再调 login 获取。 */
    void register(RegisterRequest req);

    /** 根据用户名查询用户信息（个人中心用），用户不存在时抛业务异常。 */
    User getUserInfo(String username);

    /** 修改密码：校验旧密码后更新为新密码的 bcrypt 哈希。 */
    void changePassword(String username, String oldPassword, String newPassword);

    /**
     * 找回密码第一步：按账号（用户名或邮箱）查用户，根据注册渠道发送验证码（邮箱/短信）。
     * 账号不存在抛业务异常；手机号渠道预留未开通时抛业务异常。
     */
    void forgotSendCode(String account, String ip);

    /** 找回密码第二步：校验验证码后重置密码为新密码的 bcrypt 哈希。 */
    void forgotReset(String account, String code, String newPassword);
}
