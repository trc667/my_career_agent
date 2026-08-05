/**
 * 管理后台 API（需 ADMIN 角色）
 */
import http from './http';
import type { ResultWrapper } from './chat';
import type { Notice } from './notice';

export interface AdminFeedback {
  id: number;
  username: string;
  contact?: string;
  content: string;
  createTime?: string;
}

export interface AdminUser {
  id: number;
  username: string;
  email?: string;
  role: string;
  createTime?: string;
}

export interface AnnouncementPayload {
  title: string;
  content: string;
}

/* 公告管理 */
export function getAdminAnnouncements() {
  return http.get<any, ResultWrapper<Notice[]>>('/api/admin/announcements');
}

export function createAnnouncement(payload: AnnouncementPayload) {
  return http.post<any, ResultWrapper<null>>('/api/admin/announcements', payload);
}

export function updateAnnouncement(id: number, payload: AnnouncementPayload) {
  return http.put<any, ResultWrapper<null>>(`/api/admin/announcements/${id}`, payload);
}

export function deleteAnnouncement(id: number) {
  return http.delete<any, ResultWrapper<null>>(`/api/admin/announcements/${id}`);
}

/* 意见反馈 */
export function getAdminFeedbacks() {
  return http.get<any, ResultWrapper<AdminFeedback[]>>('/api/admin/feedbacks');
}

export function deleteFeedback(id: number) {
  return http.delete<any, ResultWrapper<null>>(`/api/admin/feedbacks/${id}`);
}

/* 用户列表 */
export function getAdminUsers() {
  return http.get<any, ResultWrapper<AdminUser[]>>('/api/admin/users');
}

/* AI 头像（全局，所有人可见） */
export function uploadAiAvatar(file: File) {
  const fd = new FormData();
  fd.append('file', file);
  return http.post<any, ResultWrapper<{ avatar: string }>>('/api/admin/ai-avatar', fd);
}
