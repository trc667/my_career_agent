/**
 * 个人中心 & 意见反馈 API
 */
import http from './http';
import type { ResultWrapper } from './chat';

export interface UserInfo {
  id: number;
  username: string;
  createTime?: string;
  avatar?: string;
}

export interface FeedbackPayload {
  contact?: string;
  content: string;
}

export function getUserMe() {
  return http.get<any, ResultWrapper<UserInfo>>('/api/user/me');
}

export function changePassword(oldPassword: string, newPassword: string) {
  return http.post<any, ResultWrapper<null>>('/api/user/change-password', {
    oldPassword,
    newPassword,
  });
}

export function postFeedback(payload: FeedbackPayload) {
  return http.post<any, ResultWrapper<null>>('/api/user/feedback', payload);
}

/** 上传/更换个人头像（multipart/form-data，字段名 file） */
export function uploadAvatar(file: File) {
  const fd = new FormData();
  fd.append('file', file);
  return http.post<any, ResultWrapper<{ avatar: string }>>('/api/user/avatar', fd);
}

/** 获取 AI 头像 URL（公开接口，无需登录） */
export function getAiAvatar() {
  return http.get<any, ResultWrapper<{ avatar: string }>>('/api/config/ai-avatar');
}
