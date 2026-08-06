<template>
  <div class="practice-page">
    <div class="practice-page__bar">
      <router-link to="/bagu" class="practice-page__back">← 返回练习场</router-link>
      <h1 class="practice-page__title">学习记录</h1>
    </div>

    <main class="practice-page__body">
      <!-- 打卡卡片 -->
      <section class="practice-card practice-checkin">
        <div class="practice-checkin__info">
          <div class="practice-checkin__label">每日打卡</div>
          <div class="practice-checkin__nums">
            <span class="practice-checkin__num"><b class="pixel-font">{{ status.streak }}</b> 连续天数</span>
            <span class="practice-checkin__num"><b class="pixel-font">{{ status.totalDays }}</b> 累计天数</span>
          </div>
        </div>
        <el-button
          type="primary"
          size="large"
          class="practice-checkin__btn pixel-btn"
          :disabled="status.todayChecked || checkinLoading"
          @click="handleCheckin"
        >
          {{ status.todayChecked ? '今日已打卡' : checkinLoading ? '打卡中…' : '今日打卡' }}
        </el-button>
      </section>

      <!-- 学习统计 -->
      <section class="practice-card practice-stats">
        <div class="practice-stat">
          <div class="practice-stat__num pixel-font">{{ stats.totalWrong }}</div>
          <div class="practice-stat__label">累计错题</div>
        </div>
        <div class="practice-stat">
          <div class="practice-stat__num pixel-font" style="color: #67c23a">{{ stats.masteredCount }}</div>
          <div class="practice-stat__label">已掌握</div>
        </div>
        <div class="practice-stat">
          <div class="practice-stat__num pixel-font" style="color: #e6a23c">{{ stats.activeWrong }}</div>
          <div class="practice-stat__label">待复习</div>
        </div>
      </section>

      <!-- 错题列表 -->
      <section class="practice-card">
        <h2 class="practice-card__title">错题本（{{ wrongList.length }}）</h2>
        <div v-if="wrongList.length === 0" class="practice-empty">
          暂无错题，去练习场随机刷一题吧 🎲
        </div>
        <div v-for="w in wrongList" :key="w.id" class="practice-wrong">
          <div class="practice-wrong__head">
            <el-tag size="small" :type="tagType(w.category ?? '')">{{ w.category || '未分类' }}</el-tag>
            <span class="practice-wrong__meta">
              答错 <b class="pixel-font">{{ w.wrongCount }}</b> 次 · {{ formatTime(w.lastWrongAt) }}
            </span>
          </div>
          <p class="practice-wrong__content" :class="{ 'practice-wrong__content--expanded': expanded[w.id] }">
            {{ w.questionContent }}
          </p>
          <div class="practice-wrong__actions">
            <el-button size="small" link type="primary" @click="toggleExpand(w.id)">
              {{ expanded[w.id] ? '收起' : '展开全文' }}
            </el-button>
            <el-button size="small" link type="success" @click="handleMastered(w.id)">已掌握</el-button>
            <el-button size="small" link type="danger" @click="handleDelete(w.id)">删除</el-button>
          </div>
        </div>
      </section>
    </main>

    <!-- 背景装饰 -->
    <div class="practice-page__bg" aria-hidden="true">
      <span class="app-orb app-orb--blue practice-orb practice-orb--1" />
      <span class="app-orb app-orb--orange practice-orb practice-orb--2" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  checkinBagu,
  deleteBaguWrong,
  getBaguCheckinStatus,
  getBaguStats,
  getBaguWrong,
  markBaguMastered,
  type BaguWrong,
  type CheckinStatus,
  type PracticeStats,
} from '../api/bagu';

const status = reactive<CheckinStatus>({ todayChecked: false, streak: 0, totalDays: 0 });
const stats = reactive<PracticeStats>({ totalWrong: 0, masteredCount: 0, activeWrong: 0, streak: 0, totalDays: 0 });
const wrongList = ref<BaguWrong[]>([]);
const expanded = ref<Record<number, boolean>>({});
const checkinLoading = ref(false);

function formatTime(t?: string) {
  if (!t) return '';
  return String(t).slice(0, 19).replace('T', ' ');
}

function tagType(cat: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    后端: 'primary',
    前端: 'success',
    算法: 'warning',
    面试: 'danger',
    校招流程: 'info',
  };
  return map[cat] ?? 'info';
}

function toggleExpand(id: number) {
  expanded.value = { ...expanded.value, [id]: !expanded.value[id] };
}

async function loadAll() {
  try {
    const [wrongRes, checkinRes, statsRes] = await Promise.all([
      getBaguWrong(),
      getBaguCheckinStatus(),
      getBaguStats(),
    ]);
    wrongList.value = wrongRes.data ?? [];
    Object.assign(status, checkinRes.data ?? {});
    Object.assign(stats, statsRes.data ?? {});
  } catch {
    // 401 由拦截器处理
  }
}

async function handleCheckin() {
  checkinLoading.value = true;
  try {
    const res = await checkinBagu();
    Object.assign(status, res.data ?? {});
    ElMessage.success(`打卡成功，已连续 ${status.streak} 天`);
    await loadAll();
  } catch {
    // 拦截器已提示
  } finally {
    checkinLoading.value = false;
  }
}

async function handleMastered(id: number) {
  try {
    await markBaguMastered(id);
    ElMessage.success('已标记掌握');
    await loadAll();
  } catch {
    // 拦截器已提示
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除这道错题吗？', '提示', { type: 'warning' });
    await deleteBaguWrong(id);
    ElMessage.success('已删除');
    await loadAll();
  } catch {
    // 取消或失败
  }
}

onMounted(loadAll);
</script>

<style scoped>
.practice-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: linear-gradient(165deg, #f6f8fb 0%, #eef2f7 50%, #e6ebf2 100%);
  color: var(--app-text);
  padding: 0 var(--app-space-xl) 60px;
}

.theme-dark .practice-page {
  background: linear-gradient(165deg, #14171c 0%, #101318 50%, #0d1014 100%);
}

.practice-page__bar {
  position: relative;
  z-index: 2;
  max-width: 860px;
  width: 100%;
  margin: 0 auto;
  padding: var(--app-space-lg) 0;
  display: flex;
  align-items: center;
  gap: var(--app-space-lg);
}

.practice-page__back {
  font-size: 14px;
  color: var(--app-accent-blue);
  text-decoration: none;
}

.practice-page__back:hover {
  text-decoration: underline;
}

.practice-page__title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 1px;
}

.practice-page__body {
  position: relative;
  z-index: 2;
  max-width: 860px;
  width: 100%;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: var(--app-space-lg);
}

.practice-card {
  background: var(--app-card);
  border: 2px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  padding: var(--app-space-lg);
  box-shadow: 0 4px 0 var(--app-border), 0 12px 32px rgba(0, 0, 0, 0.05);
}

.practice-card__title {
  margin: 0 0 var(--app-space-md);
  font-size: 16px;
  font-weight: 700;
}

/* 打卡卡片 */
.practice-checkin {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--app-space-lg);
  flex-wrap: wrap;
}

.practice-checkin__label {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: var(--app-space-xs);
}

.practice-checkin__nums {
  display: flex;
  gap: var(--app-space-xl);
}

.practice-checkin__num {
  font-size: 13px;
  color: var(--app-text-secondary);
}

.practice-checkin__num b {
  font-size: 20px;
  color: var(--app-accent-blue);
  margin-right: 4px;
}

.practice-checkin__btn {
  min-width: 140px;
}

/* 统计 */
.practice-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--app-space-md);
}

.practice-stat {
  text-align: center;
  padding: var(--app-space-md);
}

.practice-stat__num {
  font-size: 28px;
  font-weight: 800;
  color: var(--app-accent-blue);
}

.practice-stat__label {
  margin-top: 4px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

/* 错题列表 */
.practice-empty {
  text-align: center;
  padding: 48px 0;
  color: var(--app-text-secondary);
  font-size: 14px;
}

.practice-wrong {
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  padding: var(--app-space-md);
  margin-bottom: var(--app-space-sm);
}

.practice-wrong__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--app-space-sm);
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.practice-wrong__meta {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.practice-wrong__meta b {
  color: #e6a23c;
  margin: 0 2px;
}

.practice-wrong__content {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--app-text);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  white-space: pre-wrap;
}

.practice-wrong__content--expanded {
  -webkit-line-clamp: unset;
  overflow: visible;
}

.practice-wrong__actions {
  margin-top: 8px;
  display: flex;
  gap: 4px;
}

/* 背景 */
.practice-page__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.practice-orb--1 {
  width: 380px;
  height: 380px;
  top: -120px;
  right: -100px;
}

.practice-orb--2 {
  width: 300px;
  height: 300px;
  bottom: -80px;
  left: -100px;
  animation-delay: 2s;
}

@media (max-width: 767px) {
  .practice-page {
    padding: 0 var(--app-space-md) 40px;
  }

  .practice-checkin {
    flex-direction: column;
    align-items: stretch;
  }

  .practice-stats {
    grid-template-columns: repeat(3, 1fr);
    gap: var(--app-space-xs);
  }
}
</style>
