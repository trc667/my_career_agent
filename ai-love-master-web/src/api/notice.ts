/**
 * 公告 API（访客可访问，无需 token）
 */
import http from './http';
import type { ResultWrapper } from './chat';

export interface Notice {
  id: number;
  title: string;
  content: string;
  createTime?: string;
}

export function getLatestNotice() {
  return http.get<any, ResultWrapper<Notice | null>>('/api/announcement/latest');
}

export function getNoticeList() {
  return http.get<any, ResultWrapper<Notice[]>>('/api/announcement/list');
}
