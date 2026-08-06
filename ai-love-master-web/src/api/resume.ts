/**
 * 简历评分 API
 */
import http from './http';
import type { ResultWrapper } from './chat';

export interface ResumeDimensionDto {
  name: string;
  score: number;
  comment: string;
  suggestion: string;
}

export interface ResumeReviewResultDto {
  totalScore: number;
  summary: string;
  dimensions: ResumeDimensionDto[];
  highlights: string[];
  weaknesses: string[];
  improvedResume: string;
}

export interface ResumeReviewSummaryDto {
  id: number;
  targetPosition?: string;
  totalScore: number;
  createdAt?: string;
}

export interface ResumeReviewDetailDto {
  id: number;
  targetPosition?: string;
  resumeText: string;
  createdAt?: string;
  result: ResumeReviewResultDto;
}

/** POST /api/resume/review 评分并保存（AI 评审耗时长，单独放宽超时到 90s，覆盖全局 20s 默认值） */
export function reviewResume(payload: { resumeText: string; targetPosition?: string }) {
  return http.post<any, ResultWrapper<ResumeReviewResultDto>>('/api/resume/review', payload, {
    timeout: 90000,
  });
}

/** GET /api/resume/reviews 历史概要列表 */
export function listResumeReviews() {
  return http.get<any, ResultWrapper<ResumeReviewSummaryDto[]>>('/api/resume/reviews');
}

/** GET /api/resume/reviews/{id} 评分详情 */
export function getResumeReview(id: number) {
  return http.get<any, ResultWrapper<ResumeReviewDetailDto>>(`/api/resume/reviews/${id}`);
}

/** DELETE /api/resume/reviews/{id} 删除评分记录 */
export function deleteResumeReview(id: number) {
  return http.delete<any, ResultWrapper<null>>(`/api/resume/reviews/${id}`);
}
