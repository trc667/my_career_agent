/**
 * 聊天历史管理 API（会话跨设备同步）
 */
import http from './http';
import type { ResultWrapper } from './chat';

export interface ConversationSummary {
  conversationId: string;
  title: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ConversationMessageDto {
  role: 'user' | 'assistant';
  content: string;
  createdAt?: string;
}

/** GET /api/conversations 当前用户的会话列表（按更新时间倒序） */
export function listConversations() {
  return http.get<any, ResultWrapper<ConversationSummary[]>>('/api/conversations');
}

/** POST /api/conversations 创建会话（后端生成 conversationId） */
export function createConversation(title?: string) {
  return http.post<any, ResultWrapper<ConversationSummary>>('/api/conversations', {
    title: title ?? '',
  });
}

/** PUT /api/conversations/{id}/rename 重命名会话 */
export function renameConversation(conversationId: string, title: string) {
  return http.put<any, ResultWrapper<null>>(
    `/api/conversations/${conversationId}/rename`,
    { title },
  );
}

/** DELETE /api/conversations/{id} 删除会话（元数据 + 消息） */
export function deleteConversation(conversationId: string) {
  return http.delete<any, ResultWrapper<null>>(`/api/conversations/${conversationId}`);
}

/** GET /api/conversations/{id}/messages 拉取完整消息（跨设备回看） */
export function getConversationMessages(conversationId: string) {
  return http.get<any, ResultWrapper<ConversationMessageDto[]>>(
    `/api/conversations/${conversationId}/messages`,
  );
}
