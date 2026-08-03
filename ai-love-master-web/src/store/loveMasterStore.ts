import { defineStore } from 'pinia';
import { computed, ref, watch } from 'vue';

export type Theme = 'light' | 'dark';

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  createdAt: number;
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
    clearCurrentConversation,
    ensureCurrentConversation,
    addMessage,
    replaceLastAssistantMessage,
    setBackendConversationId,
  };
});
