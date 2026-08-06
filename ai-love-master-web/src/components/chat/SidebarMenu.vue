<template>
  <div class="sidebar">
    <div class="sidebar__top">
      <div class="sidebar__brand">功能</div>
      <el-button size="small" type="primary" plain class="pixel-btn" @click="$emit('new-conversation')">
        新对话
      </el-button>
    </div>

    <div v-if="showMcp" class="sidebar__section">
      <div class="sidebar__section-title">快捷入口</div>
      <el-menu class="sidebar__menu" :default-active="'talk'">
        <el-menu-item index="talk" @click="$emit('open-interview-talk')">
          <PixelIcon name="message" :size="16" /> 面试话术
        </el-menu-item>
        <el-menu-item index="path" @click="$emit('open-career-path')">
          <PixelIcon name="note" :size="16" /> 学习路线
        </el-menu-item>
        <el-menu-item index="gap" @click="$emit('open-skill-gap')">
          <PixelIcon name="chart" :size="16" /> 技能差距
        </el-menu-item>
        <el-menu-item index="plan" @click="$emit('open-exam-plan')">
          <PixelIcon name="clock" :size="16" /> 备考计划
        </el-menu-item>
      </el-menu>
    </div>

    <div class="sidebar__section">
      <div class="sidebar__section-title">对话历史</div>
      <div class="sidebar__history">
        <div
          v-for="c in conversations"
          :key="c.id"
          class="sidebar__history-item pixel-hover"
          :class="{ 'sidebar__history-item--active': c.id === currentConversationId }"
          @click="$emit('switch-conversation', c.id)"
        >
          <div class="sidebar__history-title">{{ c.title || c.id }}</div>
          <el-button
            class="sidebar__history-delete"
            link
            type="danger"
            @click.stop="$emit('delete-conversation', c.id)"
          >
            删除
          </el-button>
        </div>
        <div v-if="conversations.length === 0" class="sidebar__history-empty">
          暂无历史
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import PixelIcon from '../PixelIcon.vue';

export interface SidebarConversation {
  id: string;
  title: string;
}

defineProps<{
  isMobile?: boolean;
  conversations: SidebarConversation[];
  currentConversationId: string;
  showMcp?: boolean;
}>();

defineEmits<{
  (e: 'new-conversation'): void;
  (e: 'open-interview-talk'): void;
  (e: 'open-career-path'): void;
  (e: 'open-skill-gap'): void;
  (e: 'open-exam-plan'): void;
  (e: 'switch-conversation', id: string): void;
  (e: 'delete-conversation', id: string): void;
}>();
</script>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: var(--app-space-md);
  gap: var(--app-space-md);
}

.sidebar__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--app-space-md);
}

.sidebar__brand {
  font-weight: 700;
  font-size: 15px;
}

.sidebar__section-title {
  font-size: 12px;
  opacity: 0.75;
  margin-bottom: var(--app-space-sm);
}

.sidebar__menu {
  border-right: 0;
}

.sidebar__menu :deep(.el-menu-item) {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.sidebar__history {
  display: flex;
  flex-direction: column;
  gap: var(--app-space-sm);
  max-height: 42vh;
  overflow: auto;
  padding-right: var(--app-space-xs);
}

.sidebar__history-item {
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-md);
  padding: var(--app-space-md);
  background: var(--app-card);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--app-space-md);
  transition: border-color 0.2s;
}

.sidebar__history-item:hover {
  border-color: var(--el-color-primary-light-5);
}

.sidebar__history-item--active {
  outline: 2px solid rgba(64, 158, 255, 0.35);
  border-color: var(--el-color-primary);
}

.sidebar__history-title {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.sidebar__history-empty {
  font-size: 12px;
  opacity: 0.6;
  padding: var(--app-space-sm);
}

@media (max-width: 767px) {
  .sidebar__history {
    max-height: 50vh;
  }
}
</style>
