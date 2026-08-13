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

/** POST /api/resume/analyze 评分分析（1 分）：6 维度评分 + 亮点/不足，不含优化版 */
export function analyzeResume(payload: { resumeText: string; targetPosition?: string }) {
  return http.post<any, ResultWrapper<{ id: number | null; result: ResumeReviewResultDto }>>('/api/resume/analyze', payload, {
    timeout: 90000,
  });
}

/** POST /api/resume/optimize/{id} 生成优化版简历（2 分，需先完成分析） */
export function optimizeResume(id: number) {
  return http.post<any, ResultWrapper<ResumeReviewResultDto>>(`/api/resume/optimize/${id}`, undefined, {
    timeout: 120000,
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
