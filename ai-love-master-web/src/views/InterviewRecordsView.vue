<template>
  <div class="ir-page">
    <div class="ir-page__bar">
      <router-link to="/user-center" class="ir-page__back">← 返回个人中心</router-link>
      <h1 class="ir-page__title">面试记录</h1>
      <router-link to="/interview" class="ir-page__new">+ 开始新面试</router-link>
    </div>

    <div v-if="!loaded" class="ir-skeleton">
      <el-skeleton v-for="n in 3" :key="n" animated class="ir-skeleton__card">
        <template #template>
          <div class="ir-skeleton__head">
            <el-skeleton-item variant="text" style="width: 60px; height: 22px" />
            <el-skeleton-item variant="text" style="width: 50px; height: 26px" />
          </div>
          <el-skeleton-item variant="text" style="width: 100%; height: 8px" />
          <el-skeleton-item variant="text" style="width: 80%; height: 8px" />
        </template>
      </el-skeleton>
    </div>

    <div v-else-if="!records.length" class="ir-empty">
      <p class="ir-empty__icon">🎯</p>
      <p class="ir-empty__text">还没有面试记录，去面试模拟来一场吧！</p>
      <router-link to="/interview"><el-button type="primary" round>开始面试</el-button></router-link>
    </div>

    <div v-else class="ir-list">
      <!-- 面试进步趋势 -->
      <div v-if="records.length >= 2" class="ir-trend">
        <div class="ir-trend__head">
          <span class="ir-trend__title">📈 面试进步趋势</span>
          <span class="ir-trend__tip">最近 {{ records.length }} 场</span>
        </div>
        <svg viewBox="0 0 600 180" class="ir-trend__svg">
          <defs>
            <linearGradient id="trendGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#2f6bff" />
              <stop offset="100%" stop-color="#2f6bff" stop-opacity="0" />
            </linearGradient>
          </defs>
          <line v-for="gy in [40, 95, 150]" :key="gy" x1="60" :x2="540" :y1="gy" :y2="gy" class="ir-trend__grid" />
          <polygon :points="trendArea" fill="url(#trendGrad)" />
          <polyline :points="trendPoints" fill="none" stroke="#2f6bff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
          <g v-for="(p, i) in trendData" :key="i">
            <circle :cx="p.x" :cy="p.y" r="4.5" :fill="i === trendData.length - 1 ? '#16a34a' : '#2f6bff'" stroke="#fff" stroke-width="1.5" />
            <text :x="p.x" :y="p.y - 12" text-anchor="middle" class="ir-trend__label">{{ p.score }}</text>
            <text :x="p.x" :y="173" text-anchor="middle" class="ir-trend__x">第 {{ i + 1 }} 场</text>
          </g>
        </svg>
      </div>

      <div v-for="r in records" :key="r.id" class="ir-card" @click="openDetail(r)">
        <div class="ir-card__head">
          <div class="ir-card__left">
            <span class="ir-card__position">{{ r.position }}岗</span>
            <span class="ir-card__time">{{ formatTime(r.createdAt) }}</span>
          </div>
          <span class="ir-card__score app-num" :class="scoreClass(r.totalScore)">{{ r.totalScore }}</span>
        </div>
        <div v-if="r.dimensions?.length" class="ir-card__dims">
          <div v-for="d in r.dimensions" :key="d.name" class="ir-card__dim">
            <span class="ir-card__dim-name">{{ d.name }}</span>
            <el-progress :percentage="d.score" :stroke-width="5" color="#2f6bff" />
            <span class="ir-card__dim-score app-num">{{ d.score }}</span>
          </div>
        </div>
        <span class="ir-card__more">查看逐题点评 →</span>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="showDetail" :title="detailTitle" width="min(560px, 94vw)" align-center>
      <div v-if="detail" class="ir-detail">
        <div class="ir-detail__head">
          <span class="ir-detail__label">{{ detail.position }}岗 · 面试总分</span>
          <span class="ir-detail__score app-num" :class="scoreClass(detail.totalScore)">{{ detail.totalScore }}</span>
        </div>
        <div v-for="(it, i) in detail.items" :key="i" class="ir-detail__item">
          <div class="ir-detail__q">
            <span class="ir-detail__q-no">{{ i + 1 }}</span>
            <span class="ir-detail__q-text">{{ it.question }}</span>
            <span class="ir-detail__q-score app-num" :class="scoreClass(it.score)">{{ it.score }}</span>
          </div>
          <p class="ir-detail__comment">{{ it.comment }}</p>
          <div v-if="it.score < 60" class="ir-detail__wrong">
            <el-button size="small" type="warning" plain :loading="wrongLoadingIdx === i" @click="handleAddWrong(i)">
              📕 加入错题本
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 背景装饰 -->
    <div class="ir-page__bg" aria-hidden="true">
      <span class="app-orb app-orb--blue ir-orb ir-orb--1" />
      <span class="app-orb app-orb--purple ir-orb ir-orb--2" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  addInterviewWrong,
  getInterviewRecordDetail,
  getInterviewRecords,
  type InterviewRecordDetail,
  type InterviewRecordItem,
} from '../api/interview';

const records = ref<InterviewRecordItem[]>([]);
const loaded = ref(false);
const showDetail = ref(false);
const detail = ref<InterviewRecordDetail | null>(null);
const detailTitle = computed(() => (detail.value ? `${detail.value.position}岗 · 面试详情` : '面试详情'));
const wrongLoadingIdx = ref(-1);

/* ===== 面试进步趋势（SVG 折线，不引 ECharts） ===== */
const trendData = computed(() =>
  records.value.map((r, i) => ({
    score: r.totalScore,
    x: 60 + (i * 480) / Math.max(1, records.value.length - 1),
    y: 150 - (Math.min(100, r.totalScore) / 100) * 110,
  })),
);
const trendPoints = computed(() => trendData.value.map((p) => `${p.x},${p.y}`).join(' '));
const trendArea = computed(() => {
  const pts = trendData.value;
  if (!pts.length) return '';
  return `${pts[0]!.x},150 ${pts.map((p) => `${p.x},${p.y}`).join(' ')} ${pts[pts.length - 1]!.x},150`;
});

onMounted(loadRecords);

async function loadRecords() {
  try {
    const res = await getInterviewRecords();
    records.value = res.data ?? [];
  } catch {
    // 拦截器已提示
  } finally {
    loaded.value = true;
  }
}

async function openDetail(r: InterviewRecordItem) {
  try {
    const res = await getInterviewRecordDetail(r.id);
    detail.value = res.data ?? null;
    showDetail.value = true;
  } catch {
    // 拦截器已提示
  }
}

/** 低分题一键加入错题本（幂等，同题重复只 +1） */
async function handleAddWrong(index: number) {
  if (!detail.value) return;
  wrongLoadingIdx.value = index;
  try {
    const res = await addInterviewWrong(detail.value.id, index);
    ElMessage.success(res.data?.added ? '已加入错题本，去「八股练习场」复习吧' : '该题已在错题本中（次数 +1）');
  } catch {
    // 拦截器已提示
  } finally {
    wrongLoadingIdx.value = -1;
  }
}

function scoreClass(s: number) {
  return s >= 75 ? 'is-good' : s >= 60 ? 'is-mid' : 'is-low';
}

function formatTime(t?: string) {
  if (!t) return '';
  return t.replace('T', ' ').slice(0, 16);
}
</script>

<style scoped>
.ir-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: linear-gradient(180deg, #f8fbfe 0%, #f0f4fa 55%, #eaf0f8 100%);
  color: var(--app-text);
  padding: 0 var(--app-space-xl) 60px;
}

.theme-dark .ir-page {
  background: linear-gradient(180deg, #10141c 0%, #0d1118 55%, #0a0e14 100%);
}

.ir-page__bar {
  position: relative;
  z-index: 2;
  max-width: 720px;
  width: 100%;
  margin: 0 auto;
  padding: var(--app-space-lg) 0;
  display: flex;
  align-items: center;
  gap: var(--app-space-lg);
  flex-wrap: wrap;
}

.ir-page__back {
  font-size: 14px;
  color: var(--app-primary);
  text-decoration: none;
}

.ir-page__title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  flex: 1;
}

.ir-page__new {
  font-size: 13px;
  color: var(--app-primary);
  font-weight: 600;
  text-decoration: none;
}

.ir-loading {
  position: relative;
  z-index: 2;
  text-align: center;
  color: var(--app-text-secondary);
  padding: 80px 0;
}

.ir-skeleton {
  position: relative;
  z-index: 2;
  max-width: 720px;
  width: 100%;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ir-skeleton__card {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  box-shadow: var(--app-shadow-sm);
  padding: 16px 18px;
}

.ir-skeleton__head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 14px;
}

.ir-empty {
  position: relative;
  z-index: 2;
  text-align: center;
  padding: 70px 0;
  color: var(--app-text-secondary);
}

.ir-empty__icon {
  font-size: 42px;
  margin: 0 0 10px;
}

.ir-empty__text {
  margin: 0 0 16px;
  font-size: 14px;
}

.ir-list {
  position: relative;
  z-index: 2;
  max-width: 720px;
  width: 100%;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 面试进步趋势 */
.ir-trend {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  box-shadow: var(--app-shadow-md);
  padding: 14px 16px 10px;
  animation: app-fade-up 0.5s ease both;
}

.ir-trend__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.ir-trend__title {
  font-size: 14px;
  font-weight: 800;
}

.ir-trend__tip {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.ir-trend__svg {
  width: 100%;
  height: auto;
  display: block;
}

.ir-trend__grid {
  stroke: var(--app-border);
  stroke-width: 1;
  stroke-dasharray: 4 4;
}

.ir-trend__label {
  font-size: 12px;
  font-weight: 700;
  fill: var(--app-primary);
}

.ir-trend__x {
  font-size: 10px;
  fill: var(--app-text-secondary);
}

.ir-card {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  box-shadow: var(--app-shadow-md);
  padding: 16px 18px;
  cursor: pointer;
  transition: all 0.18s ease;
  animation: app-fade-up 0.5s ease both;
}

.ir-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--app-shadow-lg);
}

.ir-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.ir-card__left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ir-card__position {
  font-size: 14px;
  font-weight: 700;
  color: var(--app-primary);
  background: var(--app-primary-soft);
  padding: 2px 10px;
  border-radius: 9999px;
}

.ir-card__time {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.ir-card__score {
  font-size: 26px;
  font-weight: 800;
}

.ir-card__score.is-good { color: #16a34a; }
.ir-card__score.is-mid { color: #f59e0b; }
.ir-card__score.is-low { color: #ef4444; }

.ir-card__dims {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ir-card__dim {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ir-card__dim-name {
  width: 64px;
  font-size: 12px;
  color: var(--app-text-secondary);
  flex-shrink: 0;
}

.ir-card__dim .el-progress {
  flex: 1;
}

.ir-card__dim-score {
  width: 30px;
  font-size: 13px;
  font-weight: 700;
  text-align: right;
}

.ir-card__more {
  display: block;
  margin-top: 10px;
  font-size: 12px;
  color: var(--app-primary);
  text-align: right;
}

.ir-detail__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 14px;
}

.ir-detail__label {
  font-size: 13px;
  color: var(--app-text-secondary);
}

.ir-detail__score {
  font-size: 34px;
  font-weight: 800;
}

.ir-detail__score.is-good { color: #16a34a; }
.ir-detail__score.is-mid { color: #f59e0b; }
.ir-detail__score.is-low { color: #ef4444; }

.ir-detail__item {
  border-top: 1px solid var(--app-border);
  padding: 12px 0;
}

.ir-detail__q {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.ir-detail__q-no {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--app-primary);
  color: #fff;
  font-size: 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 1px;
}

.ir-detail__q-text {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.6;
}

.ir-detail__q-score {
  font-size: 18px;
  font-weight: 800;
  flex-shrink: 0;
}

.ir-detail__q-score.is-good { color: #16a34a; }
.ir-detail__q-score.is-mid { color: #f59e0b; }
.ir-detail__q-score.is-low { color: #ef4444; }

.ir-detail__comment {
  margin: 6px 0 0 30px;
  font-size: 13px;
  color: var(--app-text-secondary);
  line-height: 1.7;
}

.ir-detail__wrong {
  margin: 8px 0 0 30px;
}

.ir-page__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.ir-orb--1 {
  width: 380px;
  height: 380px;
  top: -120px;
  right: -100px;
}

.ir-orb--2 {
  width: 320px;
  height: 320px;
  bottom: -80px;
  left: -100px;
  animation-delay: 2s;
}

@media (max-width: 767px) {
  .ir-page {
    padding: 0 var(--app-space-md) 40px;
  }
}
</style>
