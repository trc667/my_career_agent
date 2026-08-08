<template>
  <div class="chat-list">
    <div
      v-for="m in messages"
      :key="m.id"
      class="chat-list__item"
      :class="`chat-list__item--${m.role}`"
    >
      <div class="chat-list__avatar">
        <el-avatar v-if="m.role === 'user'" :size="36">
          <img v-if="userAvatar" :src="userAvatar" class="chat-list__avatar-img" @error="() => (userAvatar = '')" />
          <template v-else>我</template>
        </el-avatar>
        <el-avatar v-else :size="36">
          <img v-if="aiAvatar" :src="aiAvatar" class="chat-list__avatar-img" @error="() => (aiAvatar = '')" />
          <template v-else>AI</template>
        </el-avatar>
      </div>
      <div class="chat-list__bubble">
        <ThinkingSteps v-if="m.role === 'assistant' && m.steps?.length" :steps="m.steps" />
        <div class="chat-list__content">
          <div
            class="chat-md"
            v-html="renderMarkdown(
              streamingContent != null && m === messages[messages.length - 1] && m.role === 'assistant'
                ? streamingContent
                : m.content,
            )"
          ></div>
          <span
            v-if="
              m.role === 'assistant' &&
              typing &&
              m === messages[messages.length - 1]
            "
            class="pixel-caret"
          />
        </div>
        <!-- AI 回复操作栏：反馈 / 复制 / 重新生成（生成中隐藏） -->
        <div v-if="m.role === 'assistant' && !typing" class="chat-list__actions">
          <button
            class="chat-list__action"
            :class="{ 'chat-list__action--active': m.feedback === 'up' }"
            title="回答有帮助"
            @click="emitFeedback(m.id, 'up')"
          >
            👍
          </button>
          <button
            class="chat-list__action"
            :class="{ 'chat-list__action--active': m.feedback === 'down' }"
            title="回答不满意"
            @click="emitFeedback(m.id, 'down')"
          >
            👎
          </button>
          <button class="chat-list__action" title="复制全文" @click="copyMessage(m)">复制</button>
          <button
            v-if="m === messages[messages.length - 1]"
            class="chat-list__action"
            title="重新生成回复"
            @click="$emit('regenerate')"
          >
            重新生成
          </button>
        </div>
      </div>
    </div>
    <div ref="scrollAnchor" class="chat-list__anchor" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import type { ChatMessage } from '../../store/chatStore';
import ThinkingSteps from './ThinkingSteps.vue';
import { renderMarkdown } from '../../utils/markdown';
import { getAiAvatar, getUserMe } from '../../api/user';

const props = defineProps<{
  messages: ChatMessage[];
  typing?: boolean;
  streamingContent?: string | null;
}>();

const emit = defineEmits<{
  (e: 'feedback', payload: { messageId: string; type: 'up' | 'down' }): void;
  (e: 'regenerate'): void;
}>();

const scrollAnchor = ref<HTMLElement | null>(null);
const aiAvatar = ref('');
const userAvatar = ref('');

onMounted(async () => {
  // 并行拉取 AI 头像（全局公开）与当前用户头像，失败则保持首字母兜底
  getAiAvatar().then((r) => { aiAvatar.value = r.data?.avatar ?? ''; }).catch(() => {});
  getUserMe().then((r) => { userAvatar.value = r.data?.avatar ?? ''; }).catch(() => {});
});

watch(
  () => [props.messages, props.typing, props.streamingContent],
  () => {
    nextTick(() => scrollAnchor.value?.scrollIntoView({ behavior: 'smooth' }));
  },
  { deep: true },
);

/** 反馈：交给父组件（调 store.setFeedback + 后端入库） */
function emitFeedback(messageId: string, type: 'up' | 'down') {
  emit('feedback', { messageId, type });
}

/** 复制回复全文 */
async function copyMessage(m: ChatMessage) {
  try {
    await navigator.clipboard.writeText(m.content);
    ElMessage.success('已复制');
  } catch {
    ElMessage.error('复制失败，请手动选择复制');
  }
}
</script>

<style scoped>
.chat-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 新消息弹入动效（商用风：轻微上浮 + 缩放，弹性收尾） */
.chat-list__item {
  animation: msg-in 0.32s cubic-bezier(0.2, 0.8, 0.3, 1) both;
}

@keyframes msg-in {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.chat-list__item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.chat-list__item--user {
  flex-direction: row-reverse;
}

/* 头像图片：占满圆形容器、等比例裁剪 */
.chat-list__avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
  display: block;
}

.chat-list__bubble {
  max-width: min(720px, 78%);
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid var(--app-border);
  background: #f5f7fa;
}

.chat-list__item--assistant .chat-list__bubble {
  background: #ecf5ff;
}

.chat-list__content {
  word-break: break-word;
  font-size: 14px;
  line-height: 1.55;
}

/* AI 回复的 Markdown 渲染样式 */
.chat-md {
  line-height: 1.7;
  word-break: break-word;
}

.chat-md :deep(p) {
  margin: 6px 0;
}

.chat-md :deep(p:first-child) {
  margin-top: 0;
}

.chat-md :deep(h1),
.chat-md :deep(h2),
.chat-md :deep(h3),
.chat-md :deep(h4) {
  margin: 12px 0 6px;
  font-size: 1.08em;
  font-weight: 700;
  line-height: 1.4;
}

.chat-md :deep(ul),
.chat-md :deep(ol) {
  margin: 6px 0;
  padding-left: 20px;
}

.chat-md :deep(li) {
  margin: 3px 0;
}

.chat-md :deep(code) {
  background: rgba(0, 0, 0, 0.06);
  border-radius: 4px;
  padding: 1px 5px;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 0.9em;
}

.chat-md :deep(pre) {
  background: rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  padding: 10px 12px;
  overflow-x: auto;
  margin: 8px 0;
}

.chat-md :deep(pre code) {
  background: none;
  padding: 0;
}

.chat-md :deep(blockquote) {
  margin: 6px 0;
  padding-left: 12px;
  border-left: 3px solid var(--app-border);
  color: var(--app-text-secondary);
}

.chat-md :deep(a) {
  color: var(--app-accent-blue);
  text-decoration: none;
}

.chat-md :deep(a:hover) {
  text-decoration: underline;
}

.chat-md :deep(strong) {
  font-weight: 700;
}

.chat-md :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
  width: 100%;
}

.chat-md :deep(th),
.chat-md :deep(td) {
  border: 1px solid var(--app-border);
  padding: 4px 8px;
  text-align: left;
}

/* 暗色主题下代码块/行内代码底色加深 */
.theme-dark .chat-md :deep(code),
.theme-dark .chat-md :deep(pre) {
  background: rgba(255, 255, 255, 0.08);
}

.chat-list__cursor {
  display: inline-block;
  margin-left: 1px;
  animation: chat-list-cursor-blink 0.8s step-end infinite;
  vertical-align: text-bottom;
}

/* AI 回复操作栏 */
.chat-list__actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.chat-list__action {
  border: 1px solid var(--app-border);
  background: transparent;
  color: var(--app-text-secondary);
  font-size: 12px;
  line-height: 1;
  padding: 4px 8px;
  border-radius: 3px;
  cursor: pointer;
  transition: color 0.15s ease, border-color 0.15s ease, background 0.15s ease;
}

.chat-list__action:hover {
  color: var(--app-accent-blue);
  border-color: var(--app-accent-blue);
}

.chat-list__action--active {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary);
  background: rgba(64, 158, 255, 0.1);
}

@keyframes chat-list-cursor-blink {
  50% {
    opacity: 0;
  }
}
</style>

