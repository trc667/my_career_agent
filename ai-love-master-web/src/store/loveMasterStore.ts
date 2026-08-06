import { defineStore } from 'pinia';
import { computed, ref, watch } from 'vue';
import {
  listConversations,
  createConversation,
  renameConversation as apiRenameConversation,
  deleteConversation as apiDeleteConversation,
  getConversationMessages,
} from '../api/conversation';

export type Theme = 'light' | 'dark';

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  createdAt: number;
  feedback?: 'up' | 'down';
}

export interface Conversation {
  id: string;
  title: string;
  messages: ChatMessage[];
  updatedAt: number;
  backendConversationId?: string;
}

function now() {
  return Date.now();
}

function uid(prefix = 'id') {
  return `${prefix}_${Math.random().toString(36).slice(2, 10)}_${Date.now().toString(36)}`;
}

const LS_KEY = 'career_master_conversations_v1';
const LS_THEME = 'love_master_theme_v1';

export const useLoveMasterStore = defineStore('loveMaster', () => {
  const theme = ref<Theme>((localStorage.getItem(LS_THEME) as Theme) || 'light');
  const loading = ref(false);
  const typing = ref(false);
  const conversations = ref<Conversation[]>([]);
  const currentConversationId = ref<string>('');

  try {
    const raw = localStorage.getItem(LS_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as Conversation[];
      if (Array.isArray(parsed)) {
        conversations.value = parsed;
        currentConversationId.value = parsed[0]?.id || '';
      }
    }
  } catch {
    // ignore
  }

  watch(theme, (t) => localStorage.setItem(LS_THEME, t), { immediate: true });
  watch(
    conversations,
    (list) => localStorage.setItem(LS_KEY, JSON.stringify(list)),
    { deep: true },
  );

  const currentConversation = computed(() => {
    return conversations.value.find((c) => c.id === currentConversationId.value) || null;
  });

  function setTheme(next: Theme) {
    theme.value = next;
  }

  function newConversation(title = '新的职规咨询') {
    const id = crypto?.randomUUID?.() ?? uid('conv');
    const conv: Conversation = { id, title, messages: [], updatedAt: now() };
    conversations.value.unshift(conv);
    currentConversationId.value = id;
    return conv;
  }

  function switchConversation(id: string) {
    if (conversations.value.some((c) => c.id === id)) {
      currentConversationId.value = id;
    }
  }

  function deleteConversation(id: string) {
    const conv = conversations.value.find((c) => c.id === id);
    // 同步删除云端会话（后端绑定存在时），失败不阻塞本地移除
    if (conv?.backendConversationId) {
      apiDeleteConversation(conv.backendConversationId).catch(() => {});
    }
    const idx = conversations.value.findIndex((c) => c.id === id);
    if (idx < 0) return;
    conversations.value.splice(idx, 1);
    if (currentConversationId.value === id) {
      currentConversationId.value = conversations.value[0]?.id || '';
    }
  }

  /** 重命名会话（本地即时更新 + 云端同步） */
  function renameConversation(id: string, title: string) {
    const conv = conversations.value.find((c) => c.id === id);
    if (!conv) return;
    const trimmed = title.trim().slice(0, 32) || conv.title;
    conv.title = trimmed;
    conv.updatedAt = now();
    if (conv.backendConversationId) {
      apiRenameConversation(conv.backendConversationId, trimmed).catch(() => {});
    }
  }

  /** 登录后从云端拉取会话列表并与本地合并（云端优先，本地旧会话保留） */
  async function initFromServer() {
    try {
      const res = await listConversations();
      const remote = (res.data ?? []) as Array<{
        conversationId: string;
        title: string;
        updatedAt?: string;
      }>;
      if (!remote.length) return;
      const merged: Conversation[] = [];
      for (const r of remote) {
        const local = conversations.value.find(
          (l) => l.backendConversationId === r.conversationId,
        );
        merged.push(
          local ?? {
            id: r.conversationId,
            title: r.title || '新的职规咨询',
            messages: [],
            updatedAt: Date.parse(r.updatedAt ?? '') || now(),
            backendConversationId: r.conversationId,
          },
        );
      }
      // 本地未绑定后端的旧会话保留（避免丢失既有本地数据）
      for (const l of conversations.value) {
        if (!l.backendConversationId && !merged.some((m) => m.id === l.id)) {
          merged.push(l);
        }
      }
      merged.sort((a, b) => b.updatedAt - a.updatedAt);
      conversations.value = merged;
      if (!merged.some((c) => c.id === currentConversationId.value)) {
        currentConversationId.value = merged[0]?.id ?? '';
      }
    } catch {
      // 静默：未登录 / 网络错误，保持本地会话
    }
  }

  /** 发送首条消息前确保会话已绑定云端（懒创建） */
  async function ensureBackendConversation(): Promise<string | undefined> {
    const conv = currentConversation.value;
    if (!conv) return undefined;
    if (conv.backendConversationId) return conv.backendConversationId;
    try {
      const res = await createConversation();
      const id = res.data?.conversationId;
      if (!id) return undefined;
      conv.backendConversationId = id;
      return id;
    } catch {
      return undefined;
    }
  }

  /** 拉取云端消息填充会话（跨设备回看；本地已有消息则跳过） */
  async function loadMessages(convId: string) {
    const conv = conversations.value.find((c) => c.id === convId);
    if (!conv?.backendConversationId || conv.messages.length > 0) return;
    try {
      const res = await getConversationMessages(conv.backendConversationId);
      const rows = (res.data ?? []) as Array<{
        role: 'user' | 'assistant';
        content: string;
        createdAt?: string;
      }>;
      conv.messages = rows.map((r, i) => ({
        id: `remote_${conv.backendConversationId}_${i}`,
        role: r.role,
        content: r.content,
        createdAt: Date.parse(r.createdAt ?? '') || now(),
      }));
      if (conv.messages.length) conv.updatedAt = now();
    } catch {
      // 静默
    }
  }

  /** 清空全部会话（登出时调用，防止换账号串数据） */
  function reset() {
    conversations.value = [];
    currentConversationId.value = '';
  }

  function clearCurrentConversation() {
    const conv = currentConversation.value;
    if (!conv) return;
    conv.messages = [];
    conv.backendConversationId = undefined;
    conv.updatedAt = now();
  }

  function ensureCurrentConversation() {
    if (!currentConversation.value) {
      return newConversation();
    }
    return currentConversation.value;
  }

  function addMessage(role: 'user' | 'assistant', content: string) {
    const conv = ensureCurrentConversation();
    conv.messages.push({
      id: crypto?.randomUUID?.() ?? uid('msg'),
      role,
      content,
      createdAt: now(),
    });
    conv.updatedAt = now();
    if (conv.title === '新的职规咨询' && role === 'user' && content.trim()) {
      conv.title = content.trim().slice(0, 16);
    }
  }

  function replaceLastAssistantMessage(content: string) {
    const conv = ensureCurrentConversation();
    for (let i = conv.messages.length - 1; i >= 0; i--) {
      const msg = conv.messages[i];
      if (!msg) continue;
      if (msg.role === 'assistant') {
        conv.messages[i] = { ...msg, content };
        conv.updatedAt = now();
        return;
      }
    }
  }

  function setBackendConversationId(id: string) {
    const conv = currentConversation.value;
    if (conv) conv.backendConversationId = id;
  }

  /** 本地设置/切换消息反馈（up/down） */
  function setFeedback(msgId: string, type: 'up' | 'down') {
    const conv = currentConversation.value;
    if (!conv) return;
    const msg = conv.messages.find((m) => m.id === msgId);
    if (msg) {
      msg.feedback = msg.feedback === type ? undefined : type;
    }
  }

  /** 删除当前会话最后一条 AI 消息（重新生成用） */
  function removeLastAssistant() {
    const conv = currentConversation.value;
    if (!conv) return;
    for (let i = conv.messages.length - 1; i >= 0; i--) {
      const msg = conv.messages[i];
      if (msg && msg.role === 'assistant') {
        conv.messages.splice(i, 1);
        conv.updatedAt = now();
        return;
      }
    }
  }

  return {
    theme,
    loading,
    typing,
    conversations,
    currentConversationId,
    currentConversation,
    setTheme,
    newConversation,
    switchConversation,
    deleteConversation,
    renameConversation,
    clearCurrentConversation,
    ensureCurrentConversation,
    addMessage,
    replaceLastAssistantMessage,
    setBackendConversationId,
    setFeedback,
    removeLastAssistant,
    initFromServer,
    ensureBackendConversation,
    loadMessages,
    reset,
  };
});
