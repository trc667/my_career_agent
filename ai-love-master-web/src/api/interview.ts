/**
 * 面试模拟 API
 */
import http from './http';
import type { ResultWrapper } from './chat';

export interface InterviewReview {
  totalScore: number;
  dimensions: { name: string; score: number; comment: string }[];
  comment: string;
  reference: string;
}

export interface InterviewStartData {
  sessionId: string;
  position: string;
  question: string;
  index: number;
  total: number;
  vip: boolean;
  quotaLeft: number;
}

export interface InterviewAnswerData {
  index: number;
  total: number;
  finished: boolean;
  review: InterviewReview;
  nextQuestion?: string;
}

export interface InterviewReport {
  position: string;
  totalScore: number;
  dimensions: { name: string; score: number }[];
  items: { question: string; score: number; comment: string }[];
}

export interface InterviewQuota {
  vip: boolean;
  dailyLimit: number;
  quotaLeft: number;
}

/** 开始面试（选岗位，返回第 1 题） */
export function startInterview(position: string) {
  return http.post<any, ResultWrapper<InterviewStartData>>('/api/interview/start', { position });
}

/** 作答当前题（返回 AI 点评 + 下一题/结束） */
export function answerInterview(sessionId: string, answer: string) {
  return http.post<any, ResultWrapper<InterviewAnswerData>>('/api/interview/answer', { sessionId, answer });
}

/** 面试总结报告 */
export function getInterviewReport(sessionId: string) {
  return http.get<any, ResultWrapper<InterviewReport>>('/api/interview/report', { params: { sessionId } });
}

/** 今日剩余面试次数（VIP 返回 -1 不限） */
export function getInterviewQuota() {
  return http.get<any, ResultWrapper<InterviewQuota>>('/api/interview/quota');
}
