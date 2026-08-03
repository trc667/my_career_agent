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

export function getHealth() {
  return http.get<any, ResultWrapper<string>>('/api/health');
}

