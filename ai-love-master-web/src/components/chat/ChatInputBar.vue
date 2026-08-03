<template>
  <div class="input-bar">
    <el-input
      v-model="text"
      type="textarea"
      :rows="2"
      :placeholder="placeholder"
      @keydown.enter.prevent="onEnter"
    />
    <div class="input-bar__actions">
      <el-button @click="$emit('clear')" :disabled="disabled">清空对话</el-button>
      <el-button type="primary" :disabled="disabled || !text.trim()" @click="send">
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

defineProps<{
  disabled?: boolean;
  placeholder?: string;
}>();

const emit = defineEmits<{
  (e: 'send', text: string): void;
  (e: 'clear'): void;
}>();

const text = ref('');

function send() {
  const msg = text.value.trim();
  if (!msg) return;
  emit('send', msg);
  text.value = '';
}

function onEnter(e: KeyboardEvent) {
  if (e.ctrlKey || e.metaKey) {
    text.value += '\n';
    return;
  }
  send();
}
</script>

<style scoped>
.input-bar {
  display: flex;
  flex-direction: column;
  gap: var(--app-space-md);
}

.input-bar__actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--app-space-md);
  flex-wrap: wrap;
}

@media (max-width: 767px) {
  .input-bar :deep(.el-textarea__inner) {
    min-height: 72px;
    font-size: 16px;
  }
}
</style>

