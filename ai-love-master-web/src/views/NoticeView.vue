<template>
  <div class="notice-page">
    <div class="notice-page__bar">
      <router-link to="/" class="notice-page__back">← 返回首页</router-link>
      <h1 class="notice-page__title">公告中心</h1>
    </div>

    <main class="notice-page__body">
      <div v-if="loading" class="notice-page__empty">加载中…</div>
      <div v-else-if="notices.length === 0" class="notice-page__empty">暂无公告</div>
      <div
        v-for="n in notices"
        :key="n.id"
        class="notice-item"
        :class="{ 'notice-item--open': openId === n.id }"
        @click="openId = openId === n.id ? null : n.id"
      >
        <div class="notice-item__head">
          <span class="notice-item__badge">公告</span>
          <span class="notice-item__title">{{ n.title }}</span>
          <span class="notice-item__time">{{ formatTime(n.createTime) }}</span>
          <span class="notice-item__toggle">{{ openId === n.id ? '收起 ▲' : '展开 ▼' }}</span>
        </div>
        <div v-if="openId === n.id" class="notice-item__content">{{ n.content }}</div>
      </div>
    </main>

    <!-- 背景装饰 -->
    <div class="notice-page__bg" aria-hidden="true">
      <span class="app-orb app-orb--blue notice-orb notice-orb--1" />
      <span class="app-orb app-orb--orange notice-orb notice-orb--2" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { getNoticeList, type Notice } from '../api/notice';

const notices = ref<Notice[]>([]);
const openId = ref<number | null>(null);
const loading = ref(true);

function formatTime(t?: string) {
  if (!t) return '';
  return String(t).slice(0, 19).replace('T', ' ');
}

onMounted(async () => {
  try {
    const res = await getNoticeList();
    notices.value = res.data ?? [];
  } catch {
    // 忽略，显示空
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.notice-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: linear-gradient(165deg, #f6f8fb 0%, #eef2f7 50%, #e6ebf2 100%);
  color: var(--app-text);
  padding: 0 var(--app-space-xl) 60px;
}

.theme-dark .notice-page {
  background: linear-gradient(165deg, #14171c 0%, #101318 50%, #0d1014 100%);
}

.notice-page__bar {
  position: relative;
  z-index: 2;
  max-width: var(--app-content-max);
  width: 100%;
  margin: 0 auto;
  padding: var(--app-space-lg) 0;
  display: flex;
  align-items: center;
  gap: var(--app-space-lg);
}

.notice-page__back {
  font-size: 14px;
  color: var(--app-accent-blue);
  text-decoration: none;
}

.notice-page__back:hover {
  text-decoration: underline;
}

.notice-page__title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 1px;
}

.notice-page__body {
  position: relative;
  z-index: 2;
  max-width: var(--app-content-max);
  width: 100%;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: var(--app-space-md);
}

.notice-page__empty {
  text-align: center;
  padding: 80px 0;
  color: var(--app-text-secondary);
  font-size: 14px;
}

.notice-item {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-md);
  padding: 16px 20px;
  cursor: pointer;
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
  animation: app-fade-up 0.4s ease both;
}

.notice-item:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.07);
  border-color: rgba(64, 158, 255, 0.4);
}

.notice-item--open {
  border-color: rgba(64, 158, 255, 0.5);
}

.notice-item__head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.notice-item__badge {
  flex-shrink: 0;
  font-size: 11px;
  color: #fff;
  background: var(--app-accent-blue);
  padding: 2px 8px;
  border-radius: 4px;
}

.notice-item__title {
  flex: 1;
  min-width: 0;
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-item__time {
  font-size: 12px;
  color: var(--app-text-secondary);
  flex-shrink: 0;
}

.notice-item__toggle {
  font-size: 12px;
  color: var(--app-accent-blue);
  flex-shrink: 0;
}

.notice-item__content {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--app-border);
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
  color: var(--app-text);
}

.notice-page__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.notice-orb--1 {
  width: 380px;
  height: 380px;
  top: -120px;
  right: -100px;
}

.notice-orb--2 {
  width: 320px;
  height: 320px;
  bottom: -80px;
  left: -100px;
  animation-delay: 2s;
}

@media (max-width: 767px) {
  .notice-page {
    padding: 0 var(--app-space-md) 40px;
  }

  .notice-item__time {
    display: none;
  }
}
</style>
