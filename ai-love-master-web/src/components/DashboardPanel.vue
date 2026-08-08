<template>
  <div class="dash">
    <div class="dash__head">
      <div class="dash__title-wrap">
        <h3 class="dash__title">📊 数据仪表盘</h3>
        <p class="dash__sub">你的学习与成长，一目了然</p>
      </div>
      <el-tag :type="level === 'VIP' ? 'warning' : 'info'" effect="dark" size="small" class="dash__level">
        {{ level === 'VIP' ? '👑 VIP' : 'FREE' }}
      </el-tag>
    </div>

    <div class="dash__body">
      <!-- 左侧：积分大数字 -->
      <div class="dash__points">
        <span class="dash__points-label">积分余额</span>
        <span class="dash__points-value app-num">{{ displayedPoints }}</span>
        <span v-if="level !== 'VIP'" class="dash__points-tip">签到 +5/天 · 连续 7 天 +10</span>
        <span v-else class="dash__points-tip">VIP 免积分消耗</span>
      </div>

      <!-- 右侧：签到周期环形进度 -->
      <div class="dash__ring-wrap">
        <svg viewBox="0 0 120 120" class="dash__ring">
          <circle cx="60" cy="60" r="52" class="dash__ring-bg" />
          <circle
            cx="60"
            cy="60"
            r="52"
            class="dash__ring-fg"
            :stroke-dasharray="RING_C"
            :stroke-dashoffset="ringOffset"
          />
        </svg>
        <div class="dash__ring-center">
          <span class="dash__ring-num app-num">{{ streakInCycle }}</span>
          <span class="dash__ring-label">/7 天</span>
        </div>
      </div>
    </div>

    <!-- 指标行 -->
    <div class="dash__stats">
      <div class="dash__stat">
        <span class="dash__stat-icon">📅</span>
        <div class="dash__stat-text">
          <span class="dash__stat-value app-num">{{ streakDays }}</span>
          <span class="dash__stat-label">连续签到</span>
        </div>
      </div>
      <div class="dash__stat">
        <span class="dash__stat-icon">🎁</span>
        <div class="dash__stat-text">
          <span class="dash__stat-value app-num">{{ invitedCount }}</span>
          <span class="dash__stat-label">邀请好友</span>
        </div>
      </div>
      <div class="dash__stat">
        <span class="dash__stat-icon">🏅</span>
        <div class="dash__stat-text">
          <span class="dash__stat-value app-num">{{ achvUnlocked }}/{{ achvTotal }}</span>
          <span class="dash__stat-label">成就解锁</span>
        </div>
      </div>
    </div>

    <p v-if="remainToBonus > 0" class="dash__hint">再签 {{ remainToBonus }} 天解锁 +10 积分奖励</p>
    <p v-else class="dash__hint">本周周期已完成，明天开启新周期 🎉</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useCountUp } from '../composables/useCountUp';

const props = withDefaults(
  defineProps<{
    points: number;
    level: string;
    streakDays: number;
    streakInCycle: number;
    streakPct: number;
    remainToBonus: number;
    invitedCount: number;
    achvUnlocked: number;
    achvTotal: number;
  }>(),
  { points: 0, level: 'FREE', streakDays: 0, streakInCycle: 0, streakPct: 0, remainToBonus: 0, invitedCount: 0, achvUnlocked: 0, achvTotal: 0 },
);

const RING_C = 2 * Math.PI * 52; // 周长 326.7
const displayedPoints = useCountUp(computed(() => props.points));
const ringOffset = computed(() => RING_C * (1 - Math.min(100, Math.max(0, props.streakPct)) / 100));
</script>

<style scoped>
.dash {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  box-shadow: var(--app-shadow-md);
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  position: relative;
  overflow: hidden;
  transition: all 0.25s ease;
}

.dash::before {
  content: '';
  position: absolute;
  top: -60px;
  right: -60px;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(47, 107, 255, 0.12) 0%, transparent 70%);
  pointer-events: none;
}

.dash:hover {
  transform: translateY(-3px);
  box-shadow: var(--app-shadow-lg);
}

.dash__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.dash__title {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
}

.dash__sub {
  margin: 2px 0 0;
  font-size: 11px;
  color: var(--app-text-secondary);
}

.dash__level {
  flex-shrink: 0;
}

.dash__body {
  display: flex;
  align-items: center;
  gap: 16px;
}

.dash__points {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.dash__points-label {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.dash__points-value {
  font-size: 40px;
  font-weight: 800;
  line-height: 1.1;
  background: linear-gradient(135deg, #2f6bff 0%, #7b5bff 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.dash__points-tip {
  font-size: 11px;
  color: var(--app-text-secondary);
}

.dash__ring-wrap {
  position: relative;
  width: 104px;
  height: 104px;
  flex-shrink: 0;
}

.dash__ring {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.dash__ring-bg {
  fill: none;
  stroke: var(--app-border);
  stroke-width: 10;
}

.dash__ring-fg {
  fill: none;
  stroke: url(#none);
  stroke: #2f6bff;
  stroke-width: 10;
  stroke-linecap: round;
  transition: stroke-dashoffset 1s cubic-bezier(0.22, 1, 0.36, 1);
  filter: drop-shadow(0 0 4px rgba(47, 107, 255, 0.45));
}

.dash__ring-center {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: baseline;
  justify-content: center;
  padding-top: 34px;
}

.dash__ring-num {
  font-size: 26px;
  font-weight: 800;
  color: var(--app-primary);
}

.dash__ring-label {
  font-size: 11px;
  color: var(--app-text-secondary);
}

.dash__stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.dash__stat {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: var(--app-bg-deep);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  transition: all 0.18s ease;
}

.dash__stat:hover {
  transform: translateY(-2px);
  box-shadow: var(--app-shadow-sm);
}

.dash__stat-icon {
  font-size: 16px;
}

.dash__stat-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.dash__stat-value {
  font-size: 15px;
  font-weight: 800;
  color: var(--app-text);
}

.dash__stat-label {
  font-size: 10px;
  color: var(--app-text-secondary);
}

.dash__hint {
  margin: 0;
  font-size: 11px;
  color: var(--app-accent-orange);
}

/* 移动端：积分数字与指标行紧凑化 */
@media (max-width: 420px) {
  .dash__points-value {
    font-size: 32px;
  }

  .dash__ring-wrap {
    width: 88px;
    height: 88px;
  }

  .dash__ring-center {
    padding-top: 28px;
  }

  .dash__stat {
    padding: 6px 8px;
    gap: 6px;
  }

  .dash__stat-value {
    font-size: 13px;
  }
}
</style>
