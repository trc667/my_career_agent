<template>
  <!-- 本周学习概览（复用周报数据，填充页面底部空白） -->
  <section class="home__weekly">
    <div class="home__weekly-head">
      <h3 class="home__weekly-title">📊 本周学习概览</h3>
      <router-link to="/weekly-report" class="home__weekly-more">查看完整周报 →</router-link>
    </div>
    <div class="home__weekly-grid">
      <div class="home__weekly-item">
        <span class="home__weekly-icon">💬</span>
        <div class="home__weekly-body">
          <span class="home__weekly-num app-num">{{ weekly.conversation?.count ?? 0 }}</span>
          <span class="home__weekly-label">本周对话</span>
        </div>
      </div>
      <div class="home__weekly-item">
        <span class="home__weekly-icon">📅</span>
        <div class="home__weekly-body">
          <span class="home__weekly-num app-num">{{ weekly.learning?.signDays ?? 0 }}</span>
          <span class="home__weekly-label">签到天数</span>
        </div>
      </div>
      <div class="home__weekly-item">
        <span class="home__weekly-icon">📕</span>
        <div class="home__weekly-body">
          <span class="home__weekly-num app-num">+{{ weekly.learning?.newWrong ?? 0 }}</span>
          <span class="home__weekly-label">新增错题</span>
        </div>
      </div>
      <div class="home__weekly-item">
        <span class="home__weekly-icon">🎯</span>
        <div class="home__weekly-body">
          <span class="home__weekly-num app-num">{{ weekly.output?.interviews ?? 0 }}</span>
          <span class="home__weekly-label">面试场次</span>
        </div>
      </div>
      <div class="home__weekly-item">
        <span class="home__weekly-icon">🪙</span>
        <div class="home__weekly-body">
          <span class="home__weekly-num app-num" :class="weekly.points?.net > 0 ? 'is-plus' : weekly.points?.net < 0 ? 'is-minus' : ''">
            {{ (weekly.points?.net ?? 0) > 0 ? '+' : '' }}{{ weekly.points?.net ?? 0 }}
          </span>
          <span class="home__weekly-label">积分净变</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { WeeklyReport } from '../../api/user';

defineProps<{
  weekly: WeeklyReport;
}>();
</script>

<style scoped>
.home__weekly {
  position: relative;
  z-index: 2;
  max-width: var(--app-content-max);
  width: 100%;
  margin: 0 auto 40px;
  padding: 24px var(--app-space-xl);
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  box-shadow: var(--app-shadow-sm);
}

.home__weekly-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.home__weekly-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--app-text);
}

.home__weekly-more {
  font-size: 13px;
  color: var(--app-accent-blue);
  text-decoration: none;
}

.home__weekly-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: var(--app-space-md);
}

.home__weekly-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: var(--app-bg);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-md);
}

.home__weekly-item:hover {
  box-shadow: var(--app-shadow-sm);
}

.home__weekly-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.home__weekly-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.home__weekly-num {
  font-size: 18px;
  font-weight: 800;
  color: var(--app-text);
  line-height: 1.1;
}

.home__weekly-num.is-plus {
  color: #16a34a;
}

.home__weekly-num.is-minus {
  color: #ef4444;
}

.home__weekly-label {
  font-size: 11px;
  color: var(--app-text-secondary);
}
</style>
