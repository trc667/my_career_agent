/**
 * 个人中心 & 意见反馈 API
 */
import http from './http';
import type { ResultWrapper } from './chat';

export interface UserInfo {
  id: number;
  username: string;
  createTime?: string;
  avatar?: string;
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

/** 上传/更换个人头像（multipart/form-data，字段名 file） */
export function uploadAvatar(file: File) {
  const fd = new FormData();
  fd.append('file', file);
  return http.post<any, ResultWrapper<{ avatar: string }>>('/api/user/avatar', fd);
}

/** 获取 AI 头像 URL（公开接口，无需登录） */
export function getAiAvatar() {
  return http.get<any, ResultWrapper<{ avatar: string }>>('/api/config/ai-avatar');
}

/* 积分/会员（商业化） */

export interface PointLogItem {
  id: number;
  userId: number;
  changePoints: number;
  reason: string;
  createTime?: string;
}

export interface PointProfile {
  points: number;
  level: string;
  vipExpireAt?: string;
  signedToday: boolean;
  streakDays: number;
  logs?: PointLogItem[];
}

/** 积分画像（余额/等级/签到状态/连续天数/流水） */
export function getPoints() {
  return http.get<any, ResultWrapper<PointProfile>>('/api/user/points');
}

/** 每日签到（幂等） */
export function signIn() {
  return http.post<any, ResultWrapper<{ points: number; streakDays: number; bonus: boolean }>>(
    '/api/user/sign-in',
  );
}

/** 邀请信息（分享裂变：邀请码/已成功邀请数/每单奖励） */
export function getInvite() {
  return http.get<any, ResultWrapper<{ inviteCode: number; invitedCount: number; rewardPoints: number }>>(
    '/api/user/invite',
  );
}

/** 成就条目 */
export interface Achievement {
  code: string;
  name: string;
  desc: string;
  icon: string;
  progress: number;
  target: number;
  unlocked: boolean;
}

/** 成就列表（签到/对话/邀请/积分，含进度） */
export function getAchievements() {
  return http.get<any, ResultWrapper<Achievement[]>>('/api/user/achievements');
}
