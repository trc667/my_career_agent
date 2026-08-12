/**
 * 认证 API：登录、注册，对接后端 /api/auth
 */
import http from './http';
import type { ResultWrapper } from './chat';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
  code: string;
  agreed: boolean;
  /** 分享裂变：邀请码（可选，填写则绑定邀请人） */
  inviteCode?: string;
}

export interface AuthResponse {
  token: string;
  username?: string;
  expiresIn?: number;
}

export function login(data: LoginRequest) {
  return http.post<any, ResultWrapper<AuthResponse>>('/api/auth/login', data);
}

export function register(data: RegisterRequest) {
  return http.post<any, ResultWrapper<null>>('/api/auth/register', data);
}

/** 发送注册邮箱验证码 */
export function sendEmailCode(email: string) {
  return http.post<any, ResultWrapper<null>>('/api/auth/send-code', { email });
}

/** 忘记密码第一步：发送找回验证码（按账号注册渠道分发邮箱/短信） */
export function forgotSendCode(account: string) {
  return http.post<any, ResultWrapper<null>>('/api/auth/forgot/send-code', { account });
}

/** 忘记密码第二步：校验验证码并重置密码 */
export function forgotReset(account: string, code: string, newPassword: string) {
  return http.post<any, ResultWrapper<null>>('/api/auth/forgot/reset', { account, code, newPassword });
}

/** 忘记密码第一步：发送找回验证码（按账号注册渠道分发邮箱/短信） */
export function forgotSendCode(account: string) {
  return http.post<any, ResultWrapper<null>>('/api/auth/forgot/send-code', { account });
}

/** 忘记密码第二步：校验验证码并重置密码 */
export function forgotReset(account: string, code: string, newPassword: string) {
  return http.post<any, ResultWrapper<null>>('/api/auth/forgot/reset', { account, code, newPassword });
}
