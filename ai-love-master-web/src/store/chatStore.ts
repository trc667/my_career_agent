import { defineStore } from 'pinia';
import { computed, ref, watch } from 'vue';

export type Theme = 'light' | 'dark';

export type AssistantStepType = 'thought' | 'tool_call' | 'tool_result';

export interface AssistantStep {
  type: AssistantStepType;
  content: string;
  toolName?: string;
  toolInput?: string;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  createdAt: number;
  steps?: AssistantStep[];
}

export interface Conversation {
  id: string; // 前端本地会话 id，同时作为后端 conversationId 使用
  title: string;
  messages: ChatMessage[];
  updatedAt: number;
}

function now() {
  return Date.now();
}

function uid(prefix = 'id') {
  return `${prefix}_${Math.random().toString(36).slice(2, 10)}_${Date.now().toString(36)}`;
}

const LS_KEY = 'love_master_conversations_v1';
const LS_THEME = 'love_master_theme_v1';
const LS_USER = 'love_master_user_id_v1';

export const useChatStore = defineStore('chat', () => {
  const userId = ref<string>(localStorage.getItem(LS_USER) || uid('user'));
  localStorage.setItem(LS_USER, userId.value);

  const theme = ref<Theme>((localStorage.getItem(LS_THEME) as Theme) || 'light');
  const loading = ref(false);
  const typing = ref(false);

  const conversations = ref<Conversation[]>([]);
  const currentConversationId = ref<string>('');

  // 初始化：从 localStorage 读取历史
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
    const idx = conversations.value.findIndex((c) => c.id === id);
    if (idx < 0) return;
    conversations.value.splice(idx, 1);
    if (currentConversationId.value === id) {
      currentConversationId.value = conversations.value[0]?.id || '';
    }
  }

  function clearCurrentConversation() {
    const conv = currentConversation.value;
    if (!conv) return;
    conv.messages = [];
    conv.updatedAt = now();
  }

  function ensureCurrentConversation() {
    if (!currentConversation.value) {
      return newConversation();
    }
    return currentConversation.value;
  }

  function addMessage(role: 'user' | 'assistant', content: string, steps?: AssistantStep[]) {
    const conv = ensureCurrentConversation();
    conv.messages.push({
      id: crypto?.randomUUID?.() ?? uid('msg'),
      role,
      content,
      createdAt: now(),
      steps: steps ? [...steps] : undefined,
    });
    conv.updatedAt = now();
    // 简单：用第一条用户消息作为标题
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
        msg.content = content;
        conv.updatedAt = now();
        return;
      }
    }
  }

  function setLastAssistantSteps(steps: AssistantStep[]) {
    const conv = ensureCurrentConversation();
    for (let i = conv.messages.length - 1; i >= 0; i--) {
      const msg = conv.messages[i];
      if (!msg) continue;
      if (msg.role === 'assistant') {
        msg.steps = [...steps];
        conv.updatedAt = now();
        return;
      }
    }
  }

  function appendLastAssistantStep(step: AssistantStep) {
    const conv = ensureCurrentConversation();
    for (let i = conv.messages.length - 1; i >= 0; i--) {
      const msg = conv.messages[i];
      if (!msg) continue;
      if (msg.role === 'assistant') {
        msg.steps = msg.steps ?? [];
        msg.steps.push(step);
        conv.updatedAt = now();
        return;
      }
    }
  }

  return {
    userId,
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
    clearCurrentConversation,
    addMessage,
    replaceLastAssistantMessage,
    setLastAssistantSteps,
    appendLastAssistantStep,
  };
});

