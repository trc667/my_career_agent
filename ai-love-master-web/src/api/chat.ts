import http from './http';

export interface ResultWrapper<T> {
  code: number;
  message: string;
  data: T;
}

export interface ChatRequestDto {
  message: string;
  conversationId?: string;
  stream?: boolean;
  /** 可选模型名（如 deepseek-v3 / qwen-max），空则后端用默认 qwen-plus */
  model?: string;
}

/** 可选模型条目（模型切换 + 费率展示） */
export interface ChatModelOption {
  id: string;
  name: string;
  rate: number;
  desc: string;
  default?: boolean;
}

export interface AssistantStepDto {
  type: 'thought' | 'tool_call' | 'tool_result';
  content: string;
  toolName?: string;
  toolInput?: string;
}

export interface ChatResponseDto {
  reply: string;
  conversationId: string;
  usageTokens?: number;
  steps?: AssistantStepDto[];
}

export function postChatRag(body: ChatRequestDto) {
  return http.post<any, ResultWrapper<ChatResponseDto>>('/api/chat', body);
}

export function postChatReact(body: ChatRequestDto, maxSteps = 8) {
  return http.post<any, ResultWrapper<ChatResponseDto>>(
    `/api/chat/react?maxSteps=${maxSteps}`,
    body,
  );
}

export function getChatModels() {
  return http.get<any, ResultWrapper<ChatModelOption[]>>('/api/models');
}

export function getHealth() {
  return http.get<any, ResultWrapper<string>>('/api/health');
}

/** 问答反馈（点赞/点踩一条 AI 回复） */
export function postChatFeedback(payload: {
  conversationId: string;
  messageId: string;
  feedbackType: 'up' | 'down';
}) {
  return http.post<any, ResultWrapper<null>>('/api/chat/feedback', payload);
}

