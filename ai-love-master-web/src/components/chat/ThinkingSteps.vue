<template>
  <div v-if="steps?.length" class="thinking-steps">
    <div class="thinking-steps__header">
      <el-icon><View /></el-icon>
      <span>思考过程</span>
    </div>
    <div
      v-for="(s, idx) in steps"
      :key="idx"
      class="thinking-step"
      :class="`thinking-step--${s.type}`"
    >
      <div class="thinking-step__icon">
        <el-icon v-if="s.type === 'thought'"><ChatDotRound /></el-icon>
        <el-icon v-else-if="s.type === 'tool_call'"><Setting /></el-icon>
        <el-icon v-else><Select /></el-icon>
      </div>
      <div class="thinking-step__body">
        <template v-if="s.type === 'thought'">
          <span class="thinking-step__label">思考</span>
          <pre class="thinking-step__content">{{ s.content }}</pre>
        </template>
        <template v-else-if="s.type === 'tool_call'">
          <span class="thinking-step__label">调用工具</span>
          <code v-if="s.toolName" class="thinking-step__tool"> {{ s.toolName }}({{ s.toolInput ?? '' }})</code>
          <pre v-if="s.content" class="thinking-step__content">{{ s.content }}</pre>
        </template>
        <template v-else>
          <span class="thinking-step__label">工具返回</span>
          <pre class="thinking-step__content">{{ s.content }}</pre>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { View, ChatDotRound, Setting, Select } from '@element-plus/icons-vue';
import type { AssistantStep } from '../../store/chatStore';

defineProps<{
  steps?: AssistantStep[];
}>();
</script>

<style scoped>
.thinking-steps {
  margin-bottom: var(--app-space-md);
  padding: var(--app-space-sm) var(--app-space-md);
  border-radius: var(--app-radius-sm);
  background: rgba(0 0 0 / 0.04);
  border: 1px solid var(--app-border);
}

.theme-dark .thinking-steps {
  background: rgba(255 255 255 / 0.05);
}

.thinking-steps__header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

.thinking-step {
  display: flex;
  gap: 8px;
  padding: 6px 0;
  font-size: 13px;
  border-bottom: 1px solid rgba(0 0 0 / 0.06);
}

.thinking-step:last-child {
  border-bottom: none;
}

.thinking-step__icon {
  flex-shrink: 0;
  margin-top: 2px;
  color: var(--el-color-primary);
  font-size: 14px;
}

.thinking-step--tool_call .thinking-step__icon {
  color: var(--el-color-success);
}

.thinking-step--tool_result .thinking-step__icon {
  color: var(--el-color-info);
}

.thinking-step__body {
  flex: 1;
  min-width: 0;
}

.thinking-step__label {
  display: inline-block;
  margin-right: 6px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  text-transform: uppercase;
}

.thinking-step__content,
.thinking-step__tool {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--el-text-color-regular);
}

.thinking-step__tool {
  font-family: ui-monospace, monospace;
  background: rgba(0 0 0 / 0.06);
  padding: 2px 6px;
  border-radius: 4px;
}
</style>
