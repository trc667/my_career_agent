<template>
  <div class="iv-page">
    <div class="iv-page__bar">
      <router-link to="/" class="iv-page__back">← 返回首页</router-link>
      <h1 class="iv-page__title">面试模拟</h1>
      <span v-if="quota.vip" class="iv-page__quota">👑 VIP 不限次</span>
      <span v-else class="iv-page__quota">今日剩余 <b class="app-num">{{ quota.quotaLeft }}</b>/{{ quota.dailyLimit }} 次</span>
    </div>

    <!-- 阶段 1：选择岗位 -->
    <div v-if="stage === 'select'" class="iv-select">
      <h2 class="iv-select__title">选择你要面试的岗位</h2>
      <p class="iv-select__desc">AI 面试官将从知识库按岗位出 5 道题，逐题点评打分，结束后出总结报告</p>
      <div class="iv-select__grid">
        <button
          v-for="p in positions"
          :key="p.name"
          class="iv-select__item pixel-hover"
          :class="{ 'is-active': selected === p.name }"
          @click="selected = p.name"
        >
          <span class="iv-select__icon">{{ p.icon }}</span>
          <span class="iv-select__name">{{ p.name }}</span>
          <span class="iv-select__desc2">{{ p.desc }}</span>
        </button>
      </div>
      <div class="iv-select__actions">
        <el-button v-if="!quota.vip && quota.quotaLeft <= 0" type="warning" size="large" round class="shimmer-btn" @click="showVipBenefits = true">
          今日次数已用完，开通 VIP 不限次 →
        </el-button>
        <el-button v-else type="primary" size="large" round :loading="starting" class="shimmer-btn" @click="handleStart">
          开始面试
        </el-button>
      </div>
    </div>

    <!-- 阶段 2：答题 -->
    <div v-else-if="stage === 'answer'" class="iv-answer">
      <div class="iv-answer__progress">
        <span class="iv-answer__step app-num">{{ index }}/{{ total }}</span>
        <el-progress :percentage="pct" :stroke-width="6" :show-text="false" color="#2f6bff" class="iv-answer__bar" />
      </div>
      <div class="iv-answer__card">
        <h3 class="iv-answer__q">{{ question }}</h3>
        <el-input
          v-model="answerText"
          type="textarea"
          :rows="7"
          maxlength="2000"
          show-word-limit
          placeholder="组织好你的回答，尽量条理清晰、覆盖要点…"
          :disabled="reviewing"
        />
        <div class="iv-answer__actions">
          <el-button type="primary" round :loading="reviewing" :disabled="!answerText.trim()" class="shimmer-btn" @click="handleAnswer">
            提交回答
          </el-button>
        </div>
      </div>

      <!-- 点评结果 -->
      <div v-if="review" class="iv-review">
        <div class="iv-review__head">
          <span class="iv-review__score-label">本题得分</span>
          <span class="iv-review__score app-num">{{ review.totalScore }}</span>
        </div>
        <div class="iv-review__dims">
          <div v-for="d in review.dimensions" :key="d.name" class="iv-review__dim">
            <span class="iv-review__dim-name">{{ d.name }}</span>
            <el-progress :percentage="d.score" :stroke-width="6" :color="d.score >= 75 ? '#16a34a' : d.score >= 60 ? '#f59e0b' : '#ef4444'" />
            <span class="iv-review__dim-score app-num">{{ d.score }}</span>
          </div>
        </div>
        <div class="iv-review__comment">{{ review.comment }}</div>
        <details class="iv-review__ref">
          <summary>查看参考要点</summary>
          <p>{{ review.reference }}</p>
        </details>
        <div class="iv-review__actions">
          <el-button v-if="!finished" type="primary" round @click="goNext">下一题（{{ index + 1 }}/{{ total }}）</el-button>
          <el-button v-else type="success" round @click="loadReport">查看总结报告</el-button>
        </div>
      </div>
    </div>

    <!-- 阶段 3：报告 -->
    <div v-else-if="stage === 'report'" class="iv-report">
      <div class="iv-report__card">
        <div class="iv-report__head">
          <span class="iv-report__label">{{ report.position }} · 面试总分</span>
          <span class="iv-report__score app-num">{{ report.totalScore }}</span>
        </div>
        <div v-if="report.dimensions?.length" class="iv-report__radar-wrap">
          <div ref="radarEl" class="iv-report__radar"></div>
          <div class="iv-report__dims">
            <div v-for="d in report.dimensions" :key="d.name" class="iv-report__dim">
              <span class="iv-report__dim-name">{{ d.name }}</span>
              <el-progress :percentage="d.score" :stroke-width="6" color="#2f6bff" />
              <span class="iv-report__dim-score app-num">{{ d.score }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="iv-report__items">
        <div v-for="(it, i) in report.items" :key="i" class="iv-report__item">
          <span class="iv-report__item-q">{{ i + 1 }}. {{ it.question }}</span>
          <span class="iv-report__item-score app-num" :class="scoreClass(it.score)">{{ it.score }}</span>
          <span class="iv-report__item-comment">{{ it.comment }}</span>
        </div>
      </div>
      <el-button type="primary" round size="large" @click="reset">再来一次</el-button>
      <p class="iv-report__saved">✅ 本场面试已存入<a href="#/interview-records" class="iv-report__link">「我的面试」</a></p>
    </div>

    <!-- 背景装饰 -->
    <div class="iv-page__bg" aria-hidden="true">
      <span class="app-orb app-orb--blue iv-orb iv-orb--1" />
      <span class="app-orb app-orb--purple iv-orb iv-orb--2" />
    </div>
  </div>

  <VipBenefitsDialog v-model="showVipBenefits" />
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import {
  answerInterview,
  getInterviewQuota,
  getInterviewReport,
  startInterview,
  type InterviewQuota,
  type InterviewReport,
  type InterviewReview,
} from '../api/interview';
import VipBenefitsDialog from '../components/VipBenefitsDialog.vue';
import * as echarts from 'echarts/core';
import { RadarChart } from 'echarts/charts';
import { TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

echarts.use([RadarChart, TooltipComponent, CanvasRenderer]);

const positions = [
  { name: '后端', icon: '🖥️', desc: 'Java/Spring/数据库/分布式' },
  { name: '前端', icon: '🎨', desc: 'Vue/React/CSS/工程化' },
  { name: '算法', icon: '🧠', desc: '数据结构/模型/复杂度' },
  { name: '测试', icon: '🧪', desc: '自动化/用例/质量' },
  { name: '运维', icon: '☁️', desc: '容器/监控/部署' },
  { name: '通用', icon: '💼', desc: '综合技术面随机出题' },
];

const stage = ref<'select' | 'answer' | 'report'>('select');
const selected = ref('后端');
const quota = ref<InterviewQuota>({ vip: false, dailyLimit: 2, quotaLeft: 2 });

const starting = ref(false);
const reviewing = ref(false);
const showVipBenefits = ref(false);
const sessionId = ref('');
const question = ref('');
const index = ref(0);
const total = ref(5);
const answerText = ref('');
const review = ref<InterviewReview | null>(null);
const finished = ref(false);
const nextQuestion = ref('');
const report = ref<InterviewReport>({ position: '', totalScore: 0, dimensions: [], items: [] });

const pct = computed(() => Math.round((index.value / total.value) * 100));

/* ===== 报告雷达图（轻量 canvas 自绘，不引 ECharts） ===== */
const radarEl = ref<HTMLDivElement | null>(null);
let radarChart: ReturnType<typeof echarts.init> | null = null;

watch(stage, (s) => {
  if (s === 'report') {
    nextTick(drawRadar);
  } else {
    // 离开报告页时销毁图表实例：v-if 会移除 DOM，保留实例会导致下次进入时
    // init 判断跳过、setOption 作用在已脱离文档的旧元素上而白屏
    radarChart?.dispose();
    radarChart = null;
  }
});

function handleResize() {
  if (stage.value === 'report') drawRadar();
}

onMounted(() => {
  loadQuota();
  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  radarChart?.dispose();
  radarChart = null;
});

function drawRadar() {
  const el = radarEl.value;
  const dims = report.value.dimensions;
  if (!el || !dims?.length) return;
  if (!radarChart) {
    radarChart = echarts.init(el);
  }
  const css = getComputedStyle(document.documentElement);
  const primary = css.getPropertyValue('--app-primary').trim() || '#2f6bff';
  const textColor = css.getPropertyValue('--app-text-secondary').trim() || '#7a8699';
  const gridColor = css.getPropertyValue('--app-border').trim() || '#eef1f6';

  const isDark = document.documentElement.classList.contains('theme-dark');
  radarChart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: isDark ? '#1d2433' : '#ffffff',
      borderColor: isDark ? '#2c3648' : '#eef1f6',
      textStyle: { color: isDark ? '#e6ebf4' : '#1f2733' },
    },
    animationDuration: 300,
    radar: {
      indicator: dims.map((d) => ({ name: d.name, max: 100 })),
      radius: '68%',
      axisName: { color: textColor, fontSize: 12 },
      splitLine: { lineStyle: { color: gridColor } },
      splitArea: { show: false },
      axisLine: { lineStyle: { color: gridColor } },
    },
    series: [
      {
        type: 'radar',
        symbol: 'circle',
        symbolSize: 6,
        data: [
          {
            value: dims.map((d) => Math.max(0, Math.min(100, d.score))),
            name: '面试表现',
            areaStyle: { color: primary + '33' },
            lineStyle: { color: primary, width: 2 },
            itemStyle: { color: primary },
          },
        ],
      },
    ],
  });
}

async function loadQuota() {
  try {
    const res = await getInterviewQuota();
    quota.value = res.data ?? { vip: false, dailyLimit: 2, quotaLeft: 2 };
  } catch {
    // 拦截器已提示
  }
}

async function handleStart() {
  starting.value = true;
  try {
    const res = await startInterview(selected.value);
    sessionId.value = res.data?.sessionId ?? '';
    question.value = res.data?.question ?? '';
    index.value = res.data?.index ?? 1;
    total.value = res.data?.total ?? 5;
    quota.value.vip = !!res.data?.vip;
    quota.value.quotaLeft = res.data?.quotaLeft ?? 0;
    answerText.value = '';
    review.value = null;
    finished.value = false;
    stage.value = 'answer';
    nextQuestion.value = '';
  } catch {
    // 拦截器已提示
  } finally {
    starting.value = false;
  }
}

async function handleAnswer() {
  reviewing.value = true;
  try {
    const res = await answerInterview(sessionId.value, answerText.value);
    review.value = res.data?.review ?? null;
    finished.value = !!res.data?.finished;
    index.value = res.data?.index ?? index.value;
    nextQuestion.value = res.data?.nextQuestion ?? '';
    answerText.value = '';
  } catch {
    // 拦截器已提示
  } finally {
    reviewing.value = false;
  }
}

function goNext() {
  question.value = nextQuestion.value;
  nextQuestion.value = '';
  review.value = null;
}

function loadReport() {
  const sid = sessionId.value;
  getInterviewReport(sid)
    .then((res) => {
      report.value = res.data ?? { position: '', totalScore: 0, dimensions: [], items: [] };
      stage.value = 'report';
    })
    .catch(() => {
      // 拦截器已提示
    });
}

function reset() {
  stage.value = 'select';
  review.value = null;
  finished.value = false;
  report.value = { position: '', totalScore: 0, dimensions: [], items: [] };
  loadQuota();
}

function scoreClass(s: number) {
  return s >= 75 ? 'is-good' : s >= 60 ? 'is-mid' : 'is-low';
}
</script>

<style scoped>
.iv-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: linear-gradient(180deg, #f8fbfe 0%, #f0f4fa 55%, #eaf0f8 100%);
  color: var(--app-text);
  padding: 0 var(--app-space-xl) 60px;
}

.theme-dark .iv-page {
  background: linear-gradient(180deg, #10141c 0%, #0d1118 55%, #0a0e14 100%);
}

.iv-page__bar {
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

.iv-page__back {
  font-size: 14px;
  color: var(--app-primary);
  text-decoration: none;
}

.iv-page__title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  flex: 1;
}

.iv-page__quota {
  font-size: 13px;
  color: var(--app-text-secondary);
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 9999px;
  padding: 4px 14px;
  box-shadow: var(--app-shadow-sm);
}

/* 选岗 */
.iv-select,
.iv-answer,
.iv-report {
  position: relative;
  z-index: 2;
  max-width: 720px;
  width: 100%;
  margin: 0 auto;
  animation: app-fade-up 0.5s ease both;
}

.iv-select__title {
  margin: 24px 0 6px;
  font-size: 22px;
  font-weight: 800;
  text-align: center;
}

.iv-select__desc {
  margin: 0 0 24px;
  font-size: 13px;
  color: var(--app-text-secondary);
  text-align: center;
}

.iv-select__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: var(--app-space-md);
}

.iv-select__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 20px 12px;
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-md);
  box-shadow: var(--app-shadow-sm);
  cursor: pointer;
  font-family: inherit;
  color: var(--app-text);
  transition: all 0.18s ease;
}

.iv-select__item.is-active {
  border-color: var(--app-primary);
  box-shadow: 0 0 0 2px rgba(47, 107, 255, 0.18), var(--app-shadow-md);
  transform: translateY(-2px);
}

.iv-select__icon {
  font-size: 30px;
}

.iv-select__name {
  font-size: 16px;
  font-weight: 700;
}

.iv-select__desc2 {
  font-size: 12px;
  color: var(--app-text-secondary);
  text-align: center;
}

.iv-select__actions {
  text-align: center;
  margin-top: 28px;
}

/* 答题 */
.iv-answer__progress {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: var(--app-space-lg);
}

.iv-answer__step {
  font-size: 14px;
  font-weight: 700;
  color: var(--app-primary);
}

.iv-answer__bar {
  flex: 1;
}

.iv-answer__card {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  box-shadow: var(--app-shadow-md);
  padding: 24px;
}

.iv-answer__q {
  margin: 0 0 var(--app-space-lg);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.7;
}

.iv-answer__actions {
  text-align: right;
  margin-top: var(--app-space-md);
}

/* 点评 */
.iv-review {
  margin-top: var(--app-space-lg);
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  box-shadow: var(--app-shadow-md);
  padding: 20px 24px;
  animation: app-fade-up 0.4s ease both;
}

.iv-review__head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: var(--app-space-md);
}

.iv-review__score-label {
  font-size: 13px;
  color: var(--app-text-secondary);
}

.iv-review__score {
  font-size: 32px;
  font-weight: 800;
  color: var(--app-primary);
}

.iv-review__dims {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: var(--app-space-md);
}

.iv-review__dim {
  display: flex;
  align-items: center;
  gap: 10px;
}

.iv-review__dim-name {
  width: 64px;
  font-size: 12px;
  color: var(--app-text-secondary);
  flex-shrink: 0;
}

.iv-review__dim .el-progress {
  flex: 1;
}

.iv-review__dim-score {
  width: 30px;
  font-size: 14px;
  font-weight: 700;
  text-align: right;
}

.iv-review__comment {
  font-size: 14px;
  line-height: 1.7;
  color: var(--app-text);
  white-space: pre-wrap;
}

.iv-review__ref {
  margin-top: 12px;
  font-size: 13px;
  color: var(--app-text-secondary);
}

.iv-review__ref summary {
  cursor: pointer;
  color: var(--app-primary);
}

.iv-review__actions {
  text-align: right;
  margin-top: var(--app-space-lg);
}

/* 报告 */
.iv-report__card {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  box-shadow: var(--app-shadow-md);
  padding: 24px;
  margin-bottom: var(--app-space-lg);
}

.iv-report__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: var(--app-space-lg);
}

.iv-report__label {
  font-size: 14px;
  color: var(--app-text-secondary);
}

.iv-report__score {
  font-size: 40px;
  font-weight: 800;
  color: var(--app-primary);
}

.iv-report__dims {
  flex: 1;
  min-width: 200px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.iv-report__dim {
  display: flex;
  align-items: center;
  gap: 10px;
}

.iv-report__dim-name {
  width: 70px;
  font-size: 13px;
  color: var(--app-text-secondary);
}

.iv-report__dim .el-progress {
  flex: 1;
}

.iv-report__dim-score {
  width: 34px;
  font-size: 15px;
  font-weight: 700;
  text-align: right;
}

.iv-report__radar-wrap {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.iv-report__radar {
  width: 240px;
  height: 240px;
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .iv-report__radar {
    width: 200px;
    height: 200px;
    margin: 0 auto;
  }
  .iv-report__radar-wrap {
    justify-content: center;
  }
}

.iv-report__items {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 24px;
}

.iv-report__item {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 4px 12px;
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-md);
  padding: 14px 16px;
  box-shadow: var(--app-shadow-sm);
}

.iv-report__item-q {
  font-size: 14px;
  font-weight: 600;
  line-height: 1.6;
}

.iv-report__item-score {
  font-size: 20px;
  font-weight: 800;
  align-self: start;
}

.iv-report__item-score.is-good { color: #16a34a; }
.iv-report__item-score.is-mid { color: #f59e0b; }
.iv-report__item-score.is-low { color: #ef4444; }

.iv-report__item-comment {
  grid-column: 1 / -1;
  font-size: 13px;
  color: var(--app-text-secondary);
  line-height: 1.6;
}

.iv-report .el-button {
  display: block;
  margin: 0 auto;
}

.iv-report__saved {
  margin: 14px 0 0;
  text-align: center;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.iv-report__link {
  color: var(--app-primary);
  text-decoration: none;
}

/* 背景 */
.iv-page__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.iv-orb--1 {
  width: 380px;
  height: 380px;
  top: -120px;
  right: -100px;
}

.iv-orb--2 {
  width: 320px;
  height: 320px;
  bottom: -80px;
  left: -100px;
  animation-delay: 2s;
}

@media (max-width: 767px) {
  .iv-page {
    padding: 0 var(--app-space-md) 40px;
  }
}
</style>
