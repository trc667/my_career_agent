<template>
  <div class="input-bar">
    <el-input
      v-model="text"
      type="textarea"
      :rows="2"
      :placeholder="placeholder"
      @keydown.enter.prevent="onEnter"
    />
    <div v-if="models && models.length" class="input-bar__model">
      <el-select v-model="selectedModel" size="small" class="input-bar__model-select" aria-label="选择对话模型">
        <el-option
          v-for="m in models"
          :key="m.id"
          :value="m.id"
          :label="`${m.name} · ${m.rate}积分/千token`"
        >
          <div class="input-bar__model-opt">
            <span class="input-bar__model-name">{{ m.name }}</span>
            <span class="input-bar__model-rate">{{ m.rate }} 积分/千token</span>
          </div>
          <span class="input-bar__model-desc">{{ m.desc }}</span>
        </el-option>
      </el-select>
      <span v-if="currentModelDesc" class="input-bar__model-hint">{{ currentModelDesc }}</span>
    </div>
    <div class="input-bar__actions">
      <el-button @click="$emit('clear')" :disabled="disabled || typing">清空对话</el-button>
      <el-button v-if="!typing" type="primary" class="pixel-btn" :disabled="disabled || !text.trim()" @click="send">
        发送
      </el-button>
      <el-button v-else type="danger" class="pixel-btn" @click="$emit('stop')">
        停止生成
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import type { ChatModelOption } from '../../api/chat';

const props = withDefaults(
  defineProps<{
    disabled?: boolean;
    placeholder?: string;
    typing?: boolean;
    /** 可选模型列表；不传/为空则隐藏模型选择器 */
    models?: ChatModelOption[];
    modelValue?: string;
  }>(),
  { modelValue: '' },
);

const emit = defineEmits<{
  (e: 'send', text: string): void;
  (e: 'clear'): void;
  (e: 'stop'): void;
  (e: 'update:modelValue', model: string): void;
}>();

const text = ref('');

/** 当前选中模型（本地同步 + 双向绑定给父组件） */
const selectedModel = computed({
  get: () => props.modelValue,
  set: (v: string) => emit('update:modelValue', v),
});

/** 当前模型的描述文案 */
const currentModelDesc = computed(() => {
  const m = props.models?.find((x) => x.id === props.modelValue);
  return m ? m.desc : '';
});

function send() {
  const msg = text.value.trim();
  if (!msg) return;
  emit('send', msg);
  text.value = '';
}

function onEnter(e: Event) {
  const ke = e as KeyboardEvent;
  if (ke.ctrlKey || ke.metaKey) {
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

.input-bar__model {
  display: flex;
  align-items: center;
  gap: var(--app-space-sm, 8px);
  flex-wrap: wrap;
}

.input-bar__model-select {
  width: 210px;
}

.input-bar__model-hint {
  font-size: 12px;
  color: var(--app-text-secondary, #8a94a6);
}

.input-bar__model-opt {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.input-bar__model-name {
  font-weight: 600;
}

.input-bar__model-rate {
  font-size: 12px;
  color: #2f6bff;
}

.input-bar__model-desc {
  display: block;
  font-size: 12px;
  color: #8a94a6;
  line-height: 1.4;
  margin-top: 2px;
}

@media (max-width: 767px) {
  .input-bar :deep(.el-textarea__inner) {
    min-height: 72px;
    font-size: 16px;
  }

  /* 模型选择器在窄屏占满整行 */
  .input-bar__model-select {
    flex: 1;
    width: auto;
    min-width: 0;
  }

  /* 操作按钮均分整行（清空 / 发送·停止），拇指更易点按 */
  .input-bar__actions :deep(.el-button) {
    flex: 1;
    margin-left: 0;
  }
}
</style>

