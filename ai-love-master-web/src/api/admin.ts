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

export interface AdminErrorLog {
  id: number;
  level: string;
  source: string;
  message: string;
  stackTrace?: string;
  uri?: string;
  method?: string;
  username?: string;
  userAgent?: string;
  ip?: string;
  createTime?: string;
}

/* 运营看板 */
export interface AdminStats {
  users: { total: number; newWeek: number; vip: number };
  activeToday: number;
  conversations: { total: number; week: number };
  weekSignDays: number;
  weekCheckinDays: number;
  points: { earned: number; spent: number };
  redeems: { count: number; points: number };
  spendTop: { reason: string; points: number }[];
}

export function getAdminStats() {
  return http.get<any, ResultWrapper<AdminStats>>('/api/admin/stats');
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

/* 错误日志（自建监控面板） */
export function getAdminErrorLogs(params?: { source?: string; level?: string; limit?: number }) {
  return http.get<any, ResultWrapper<AdminErrorLog[]>>('/api/admin/error-logs', { params });
}

export function clearAdminErrorLogs() {
  return http.delete<any, ResultWrapper<null>>('/api/admin/error-logs');
}

/* 知识库管理（RAG 事实源在线增删改查） */
export interface Knowledge {
  id: number;
  category: string;
  content: string;
  enabled: number;
  createTime?: string;
  updateTime?: string;
}

export interface KnowledgePage {
  list: Knowledge[];
  total: number;
}

export interface KnowledgeCategoryStat {
  category: string;
  count: number;
}

export function getAdminKnowledge(params?: {
  category?: string;
  keyword?: string;
  enabled?: number;
  page?: number;
  size?: number;
}) {
  return http.get<any, ResultWrapper<KnowledgePage>>('/api/admin/knowledge', { params });
}

export function getKnowledgeCategories() {
  return http.get<any, ResultWrapper<KnowledgeCategoryStat[]>>('/api/admin/knowledge/categories');
}

export function createKnowledge(payload: { content: string; category?: string; enabled?: boolean }) {
  return http.post<any, ResultWrapper<Knowledge>>('/api/admin/knowledge', payload);
}

export function updateKnowledge(id: number, payload: { content: string; category?: string; enabled?: boolean }) {
  return http.put<any, ResultWrapper<Knowledge>>(`/api/admin/knowledge/${id}`, payload);
}

export function toggleKnowledge(id: number, enabled: boolean) {
  return http.put<any, ResultWrapper<null>>(`/api/admin/knowledge/${id}/enabled?enabled=${enabled}`);
}

export function deleteKnowledge(id: number) {
  return http.delete<any, ResultWrapper<null>>(`/api/admin/knowledge/${id}`);
}

export function rebuildKnowledge() {
  return http.post<any, ResultWrapper<null>>('/api/admin/knowledge/rebuild');
}

export function getKnowledgeRebuildStatus() {
  return http.get<any, ResultWrapper<{ rebuilding: boolean; status: string; info: string }>>(
    '/api/admin/knowledge/rebuild-status',
  );
}
