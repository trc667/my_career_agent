/**
 * 八股知识库 API
 */
import http from './http';
import type { ResultWrapper } from './chat';

export interface BaguEntry {
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
