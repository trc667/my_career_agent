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

/** 面试记录条目（列表） */
export interface InterviewRecordItem {
  id: number;
  position: string;
  totalScore: number;
  dimensions: { name: string; score: number }[];
  createdAt: string;
}

/** 面试记录详情（含逐题明细） */
export interface InterviewRecordDetail extends InterviewRecordItem {
  items: { question: string; score: number; comment: string }[];
}

/** 我的面试记录列表 */
export function getInterviewRecords() {
  return http.get<any, ResultWrapper<InterviewRecordItem[]>>('/api/interview/records');
}

/** 单场面试详情 */
export function getInterviewRecordDetail(id: number) {
  return http.get<any, ResultWrapper<InterviewRecordDetail>>(`/api/interview/records/${id}`);
}

/** 将面试记录中某题加入错题本（幂等） */
export function addInterviewWrong(id: number, index: number) {
  return http.post<any, ResultWrapper<{ added: boolean; wrongCount: number }>>(
    `/api/interview/records/${id}/wrong`,
    null,
    { params: { index } },
  );
}
