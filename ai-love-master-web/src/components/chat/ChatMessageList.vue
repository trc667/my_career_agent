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
      </div>
    </div>
    <div ref="scrollAnchor" class="chat-list__anchor" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue';
import type { ChatMessage } from '../../store/chatStore';
import ThinkingSteps from './ThinkingSteps.vue';
import { renderMarkdown } from '../../utils/markdown';
import { getAiAvatar, getUserMe } from '../../api/user';

const props = defineProps<{
  messages: ChatMessage[];
  typing?: boolean;
  streamingContent?: string | null;
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
</script>

<style scoped>
.chat-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
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

@keyframes chat-list-cursor-blink {
  50% {
    opacity: 0;
  }
}
</style>

