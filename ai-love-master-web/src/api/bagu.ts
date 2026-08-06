/**
 * 八股知识库 + 学习记录 API
 */
import http from './http';
import type { ResultWrapper } from './chat';

export interface BaguEntry {
  id: string;
  content: string;
  category: string;
  summary?: string;
}

export interface BaguPage {
  list: BaguEntry[];
  total: number;
}

export interface BaguCategory {
  category: string;
  count: number;
}

export interface BaguWrong {
  id: number;
  userId: number;
  questionId: string;
  questionContent: string;
  category?: string;
  wrongCount: number;
  lastWrongAt?: string;
  mastered: number;
  createTime?: string;
}

export interface CheckinStatus {
  todayChecked: boolean;
  streak: number;
  totalDays: number;
}

export interface PracticeStats {
  totalWrong: number;
  masteredCount: number;
  activeWrong: number;
  streak: number;
  totalDays: number;
}

/* ===== 八股知识库 ===== */
export function getBaguList(params: { category?: string; keyword?: string; page?: number; size?: number }) {
  return http.get<any, ResultWrapper<BaguPage>>('/api/bagu/list', { params });
}

export function getBaguCategories() {
  return http.get<any, ResultWrapper<BaguCategory[]>>('/api/bagu/categories');
}

export function getBaguRandom(category?: string) {
  return http.get<any, ResultWrapper<BaguEntry>>('/api/bagu/random', {
    params: category ? { category } : {},
  });
}

/* ===== 学习记录（错题本 + 打卡，需登录） ===== */

/** 加入/更新错题 */
export function addBaguWrong(payload: { questionId: string; category?: string; content: string }) {
  return http.post<any, ResultWrapper<BaguWrong>>('/api/bagu/practice/wrong', payload);
}

/** 错题列表（未掌握） */
export function getBaguWrong() {
  return http.get<any, ResultWrapper<BaguWrong[]>>('/api/bagu/practice/wrong');
}

/** 标记掌握 */
export function markBaguMastered(id: number) {
  return http.put<any, ResultWrapper<null>>(`/api/bagu/practice/wrong/${id}/mastered`);
}

/** 删除错题 */
export function deleteBaguWrong(id: number) {
  return http.delete<any, ResultWrapper<null>>(`/api/bagu/practice/wrong/${id}`);
}

/** 今日打卡（幂等） */
export function checkinBagu() {
  return http.post<any, ResultWrapper<CheckinStatus>>('/api/bagu/practice/checkin');
}

/** 打卡状态 */
export function getBaguCheckinStatus() {
  return http.get<any, ResultWrapper<CheckinStatus>>('/api/bagu/practice/checkin/status');
}

/** 学习统计 */
export function getBaguStats() {
  return http.get<any, ResultWrapper<PracticeStats>>('/api/bagu/practice/stats');
}
