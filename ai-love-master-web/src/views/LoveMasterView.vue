<template>
  <el-container class="chat-layout" :class="`theme-${theme}`">
    <!-- 移动端侧栏遮罩：点击关闭 -->
    <div v-if="isMobile && sidebarOpen" class="chat-layout__mask" @click="sidebarOpen = false"></div>
    <el-aside
      class="chat-layout__aside"
      width="280px"
      v-show="!isMobile || sidebarOpen"
    >
      <SidebarMenu
        :conversations="store.conversations"
        :current-conversation-id="store.currentConversationId ?? ''"
        :show-mcp="true"
        @new-conversation="onNewConversation"
        @open-interview-talk="showInterviewTalk = true"
        @open-career-path="showCareerPath = true"
        @open-skill-gap="showSkillGap = true"
        @open-exam-plan="showExamPlan = true"
        @switch-conversation="store.switchConversation"
        @delete-conversation="store.deleteConversation"
      />
    </el-aside>
    <el-container>
      <el-header class="chat-layout__header">
        <div class="chat-header__left">
          <el-button v-if="isMobile" link @click="sidebarOpen = !sidebarOpen">
            <el-icon><Menu /></el-icon>
          </el-button>
          <router-link to="/" class="chat-header__back">← 返回</router-link>
          <div class="chat-header__title">AI 职规大师</div>
        </div>
        <div class="chat-header__right">
          <el-avatar :size="32" :src="authStore.avatar || undefined">{{ authStore.avatar ? '' : (authStore.username || '我')[0] }}</el-avatar>
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
      </el-main>
      <el-footer class="chat-layout__footer">
        <ChatInputBar
          :disabled="loading || typing"
          @send="handleSend"
          @clear="handleClear"
        />
      </el-footer>
    </el-container>
    <InterviewTalkDialog v-model:visible="showInterviewTalk" @insert="handleInsertPrompt" />
    <CareerPathDialog v-model:visible="showCareerPath" @insert="handleInsertPrompt" />
    <SkillGapDialog
      v-model:visible="showSkillGap"
      @insert="handleInsertPrompt"
    />
    <ExamPlanDialog
      v-model:visible="showExamPlan"
      @insert="handleInsertPrompt"
    />
  </el-container>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { ElMessage } from 'element-plus';
import { Menu } from '@element-plus/icons-vue';

import ChatMessageList from '../components/chat/ChatMessageList.vue';
import ChatInputBar from '../components/chat/ChatInputBar.vue';
import SidebarMenu from '../components/chat/SidebarMenu.vue';
import InterviewTalkDialog from '../components/mcp/InterviewTalkDialog.vue';
import CareerPathDialog from '../components/mcp/CareerPathDialog.vue';
import SkillGapDialog from '../components/mcp/SkillGapDialog.vue';
import ExamPlanDialog from '../components/mcp/ExamPlanDialog.vue';

import { postChatStream } from '../api/chatStream';
import { useStreamTypewriter } from '../composables/useStreamTypewriter';
import { useLoveMasterStore } from '../store/loveMasterStore';
import { useAuthStore } from '../store/authStore';

const router = useRouter();
const store = useLoveMasterStore();
const authStore = useAuthStore();
const streamingContent = ref('');
const typewriter = useStreamTypewriter((content) => {
  streamingContent.value = content;
  store.replaceLastAssistantMessage(content);
});
const { theme, loading, typing, currentConversation } = storeToRefs(store);

const isMobile = ref(false);
const sidebarOpen = ref(true);
const showInterviewTalk = ref(false);
const showCareerPath = ref(false);
const showSkillGap = ref(false);
const showExamPlan = ref(false);

function updateResponsive() {
  isMobile.value = window.innerWidth < 768;
  if (isMobile.value) sidebarOpen.value = false;
  else sidebarOpen.value = true;
}

onMounted(() => {
  updateResponsive();
  store.ensureCurrentConversation();
  authStore.fetchAvatar();
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
  store.addMessage('assistant', '');

  loading.value = true;
  typing.value = true;
  streamingContent.value = '';
  typewriter.reset();

  try {
    await postChatStream(
      {
        message: msg,
        conversationId: currentConversation.value?.backendConversationId ?? '',
      },
      (chunk) => typewriter.feed(chunk),
      (id) => store.setBackendConversationId(id),
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

function handleInsertPrompt(prompt: string) {
  handleSend(prompt);
}
</script>

<style scoped>
/* 布局样式由 src/styles/chat-layout.css 提供 */
</style>
