/**
 * 个人中心 & 意见反馈 API
 */
import http from './http';
import type { ResultWrapper } from './chat';

export interface UserInfo {
  id: number;
  username: string;
  createTime?: string;
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
