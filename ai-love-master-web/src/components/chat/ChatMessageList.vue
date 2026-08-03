<template>
  <div class="chat-list">
    <div
      v-for="m in messages"
      :key="m.id"
      class="chat-list__item"
      :class="`chat-list__item--${m.role}`"
    >
      <div class="chat-list__avatar">
        <el-avatar v-if="m.role === 'user'" size="small">我</el-avatar>
        <el-avatar v-else size="small">AI</el-avatar>
      </div>
      <div class="chat-list__bubble">
        <ThinkingSteps v-if="m.role === 'assistant' && m.steps?.length" :steps="m.steps" />
        <div class="chat-list__content">
          {{ streamingContent != null && m === messages[messages.length - 1] && m.role === 'assistant' ? streamingContent : m.content }}<span
            v-if="
              m.role === 'assistant' &&
              typing &&
              m === messages[messages.length - 1]
            "
            class="chat-list__cursor"
          >|</span>
        </div>
      </div>
    </div>
    <div ref="scrollAnchor" class="chat-list__anchor" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue';
import type { ChatMessage } from '../../store/chatStore';
import ThinkingSteps from './ThinkingSteps.vue';

const props = defineProps<{
  messages: ChatMessage[];
  typing?: boolean;
  streamingContent?: string | null;
}>();

const scrollAnchor = ref<HTMLElement | null>(null);

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
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.55;
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

