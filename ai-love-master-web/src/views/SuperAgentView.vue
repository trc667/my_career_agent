<template>
  <el-container class="chat-layout" :class="`theme-${theme}`">
    <el-aside
      class="chat-layout__aside"
      width="280px"
      v-show="!isMobile || sidebarOpen"
    >
      <SidebarMenu
        :conversations="conversations"
        :current-conversation-id="store.currentConversationId"
        :show-mcp="false"
        @new-conversation="onNewConversation"
        @switch-conversation="(id) => store.switchConversation(id)"
        @delete-conversation="(id) => store.deleteConversation(id)"
      />
    </el-aside>
    <el-container>
      <el-header class="chat-layout__header">
        <div class="chat-header__left">
          <el-button v-if="isMobile" link @click="sidebarOpen = !sidebarOpen">
            <el-icon><Menu /></el-icon>
          </el-button>
          <router-link to="/" class="chat-header__back">← 返回</router-link>
          <div class="chat-header__title">AI 超级智能体</div>
        </div>
        <div class="chat-header__right">
          <el-avatar size="small">{{ (authStore.username || '我')[0] }}</el-avatar>
          <span class="chat-header__nickname">{{ authStore.username || '用户' }}</span>
          <el-button link size="small" @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="chat-layout__main">
        <ChatMessageList
          :messages="currentConversation?.messages || []"
          :typing="typing"
          :streaming-content="typing ? streamingContent : null"
        />
        <Transition name="hint-fade">
          <div v-if="loading || typing" class="super-agent__hint">
            <el-icon class="super-agent__hint-icon"><Loading /></el-icon>
            <span>智能体正在深度思考，可能在调用工具，请耐心等待...</span>
          </div>
        </Transition>
      </el-main>
      <el-footer class="chat-layout__footer">
        <ChatInputBar
          :disabled="loading || typing"
          placeholder="输入任务或问题…（Enter 发送，Ctrl/⌘+Enter 换行）"
          @send="handleSend"
          @clear="handleClear"
        />
      </el-footer>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { ElMessage } from 'element-plus';
import { Loading, Menu } from '@element-plus/icons-vue';

import ChatMessageList from '../components/chat/ChatMessageList.vue';
import ChatInputBar from '../components/chat/ChatInputBar.vue';
import SidebarMenu from '../components/chat/SidebarMenu.vue';

import {
  postChatReactStream,
  type ReactStreamEvent,
} from '../api/chatStream';
import { useStreamTypewriter } from '../composables/useStreamTypewriter';
import { useSuperAgentStore } from '../store/superAgentStore';
import { useAuthStore } from '../store/authStore';

const router = useRouter();
const store = useSuperAgentStore();
const authStore = useAuthStore();
const streamingContent = ref('');
const typewriter = useStreamTypewriter((content) => {
  streamingContent.value = content;
  store.replaceLastAssistantMessage(content);
});
const {
  theme,
  loading,
  typing,
  currentConversation,
  conversations,
} = storeToRefs(store);

const isMobile = ref(false);
const sidebarOpen = ref(true);

function updateResponsive() {
  isMobile.value = window.innerWidth < 768;
  if (isMobile.value) sidebarOpen.value = false;
  else sidebarOpen.value = true;
}

onMounted(() => {
  updateResponsive();
  store.ensureCurrentConversation();
  window.addEventListener('resize', updateResponsive);
});

onUnmounted(() => window.removeEventListener('resize', updateResponsive));

function handleLogout() {
  authStore.logout();
  router.push('/');
}

function onNewConversation() {
  store.newConversation();
  if (isMobile.value) sidebarOpen.value = false;
}

async function handleSend(text: string) {
  const msg = text.trim();
  if (!msg) return;

  store.ensureCurrentConversation();
  store.addMessage('user', msg);
  store.addMessage('assistant', '', []);

  loading.value = true;
  typing.value = true;
  streamingContent.value = '';
  typewriter.reset();

  try {
    await postChatReactStream(
      {
        message: msg,
        conversationId: currentConversation.value?.backendConversationId ?? '',
      },
      8,
      (ev: ReactStreamEvent) => {
        switch (ev.type) {
          case 'conv':
            if (ev.conversationId)
              store.setBackendConversationId(ev.conversationId);
            break;
          case 'thought':
            store.appendLastAssistantStep({
              type: 'thought',
              content: ev.content ?? '',
            });
            break;
          case 'tool_call': {
            store.appendLastAssistantStep({
              type: 'tool_call',
              content: ev.content ?? '',
              toolName: ev.toolName,
              toolInput: ev.toolInput,
            });
            // 在主内容区输出工具名，便于用户看到调用了哪些工具
            if (ev.toolName) {
              typewriter.feed(`🔧 调用工具: ${String(ev.toolName)}\n`);
            }
            break;
          }
          case 'tool_result':
            store.appendLastAssistantStep({
              type: 'tool_result',
              content: ev.content ?? '',
            });
            break;
          case 'reply': {
            // 后端会多次发送 {"type":"reply","content":"你"}、{"type":"reply","content":"好"} 等片段
            // 必须逐块追加（typewriter.feed 内部 receivedContent += chunk），不能替换
            const text = ev.content ?? ev.data ?? ev.text ?? ev.delta ?? '';
            if (text != null && text !== '') typewriter.feed(String(text));
            break;
          }
          case 'error':
            ElMessage.error(ev.content ?? '请求出错');
            break;
        }
      },
      (err) => ElMessage.error(err),
    );
  } catch {
    ElMessage.error('请求失败');
  } finally {
    typewriter.stop();
    const final = streamingContent.value;
    if (final) store.replaceLastAssistantMessage(final);
    streamingContent.value = '';
    loading.value = false;
    typing.value = false;
  }
}

function handleClear() {
  store.clearCurrentConversation();
}
</script>

<style scoped>
/* 布局样式由 src/styles/chat-layout.css 提供 */
.super-agent__hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  margin-top: 12px;
  border-radius: 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  background: rgba(64, 158, 255, 0.08);
  border: 1px solid rgba(64, 158, 255, 0.2);
}
.super-agent__hint-icon {
  flex-shrink: 0;
  font-size: 16px;
  color: var(--el-color-primary);
}
.hint-fade-enter-active,
.hint-fade-leave-active {
  transition: opacity 0.25s ease;
}
.hint-fade-enter-from,
.hint-fade-leave-to {
  opacity: 0;
}
</style>
