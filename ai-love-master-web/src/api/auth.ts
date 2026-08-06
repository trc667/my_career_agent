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
