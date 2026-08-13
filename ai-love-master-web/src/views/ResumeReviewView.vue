<template>
  <div class="resume-page" :class="`theme-${theme}`">
    <div class="resume-page__bar">
      <router-link to="/" class="resume-page__back">← 返回首页</router-link>
      <div class="resume-page__title">📄 简历评分</div>
    </div>

    <div class="resume-layout">
      <!-- 左：简历输入 -->
      <section class="resume-card resume-card--input">
        <h2 class="resume-card__title">粘贴你的简历</h2>
        <p class="resume-card__desc">AI 将结合知识库中的简历写作规范，分 6 个维度评分并给出优化版简历</p>
        <el-input
          v-model="resumeText"
          type="textarea"
          :rows="14"
          maxlength="5000"
          show-word-limit
          placeholder="将简历全文粘贴到这里（支持文本简历，如：姓名/联系方式/教育背景/项目经历/技能/荣誉…）"
          class="resume-textarea"
        />
        <el-input
          v-model="targetPosition"
          maxlength="128"
          placeholder="目标岗位（可选），如：Java 后端开发工程师"
          class="resume-position"
        />
        <el-button
          type="primary"
          size="large"
          class="resume-submit pixel-btn"
          :loading="loading"
          :disabled="!resumeText.trim() || loading"
          @click="handleAnalyze"
        >
          {{ loading ? 'AI 正在分析简历…' : '开始分析（1 积分）' }}
        </el-button>
      </section>

      <!-- 右：评分结果 -->
      <section class="resume-card resume-card--result">
        <template v-if="result">
          <div class="resume-score">
            <div class="resume-score__num pixel-font" :style="{ color: scoreColor(result.totalScore) }">
              {{ result.totalScore }}
            </div>
            <div class="resume-score__label">总分 / 100</div>
          </div>

          <div class="resume-section">
            <div class="resume-section__title">总体评价</div>
            <p class="resume-section__text">{{ result.summary }}</p>
          </div>

          <div v-if="result.dimensions?.length" class="resume-section">
            <div class="resume-section__title">维度评分</div>
            <div
              v-for="d in result.dimensions"
              :key="d.name"
              class="resume-dim"
            >
              <div class="resume-dim__head">
                <span class="resume-dim__name">{{ d.name }}</span>
                <span class="resume-dim__score">{{ d.score }}</span>
              </div>
              <el-progress
                :percentage="d.score"
                :color="scoreColor(d.score)"
                :stroke-width="8"
                :show-text="false"
              />
              <p v-if="d.comment" class="resume-dim__comment">{{ d.comment }}</p>
              <p v-if="d.suggestion" class="resume-dim__suggestion">建议：{{ d.suggestion }}</p>
            </div>
          </div>

          <div v-if="result.highlights?.length" class="resume-section">
            <div class="resume-section__title">简历亮点</div>
            <div class="resume-tags">
              <el-tag v-for="h in result.highlights" :key="h" type="success" effect="plain">{{ h }}</el-tag>
            </div>
          </div>

          <div v-if="result.weaknesses?.length" class="resume-section">
            <div class="resume-section__title">主要不足</div>
            <div class="resume-tags">
              <el-tag v-for="w in result.weaknesses" :key="w" type="warning" effect="plain">{{ w }}</el-tag>
            </div>
          </div>

          <div class="resume-section">
            <div class="resume-section__title resume-section__title--row">
              优化版简历
              <el-button
                v-if="result.improvedResume"
                size="small"
                type="primary"
                plain
                class="pixel-btn"
                @click="copyResume"
              >复制全文</el-button>
              <el-button
                v-else
                size="small"
                type="primary"
                class="pixel-btn"
                :loading="optimizing"
                @click="handleOptimize"
              >生成优化版（2 积分）</el-button>
            </div>
            <pre v-if="result.improvedResume" class="resume-improved">{{ result.improvedResume }}</pre>
            <p v-else class="resume-optimize-tip">基于上面的评分意见生成优化后的完整简历（约 1 分钟），消耗 2 积分</p>
          </div>
        </template>
        <div v-else-if="loading" class="resume-loading">
          <el-icon class="is-loading resume-loading__icon"><Loading /></el-icon>
          <p class="resume-loading__title">AI 正在分析简历…</p>
          <p class="resume-loading__desc">正在从 6 个维度分析并生成评分意见<br />通常 30 秒内完成，请稍候</p>
        </div>
        <div v-else class="resume-empty">
          <div class="resume-empty__icon">📄</div>
          <p>在左侧粘贴简历后点击「开始评分」，<br />AI 将从项目经历、量化成果、岗位匹配等维度给出评分与优化建议。</p>
        </div>
      </section>
    </div>

    <!-- 历史记录 -->
    <section v-if="history.length" class="resume-card resume-history">
      <h2 class="resume-card__title">历史评分记录</h2>
      <div class="resume-history__list">
        <div
          v-for="h in history"
          :key="h.id"
          class="resume-history__item pixel-hover"
          :class="{ 'resume-history__item--active': h.id === currentHistoryId }"
          @click="loadHistory(h.id)"
        >
          <div class="resume-history__info">
            <div class="resume-history__pos">{{ h.targetPosition || '未指定岗位' }}</div>
            <div class="resume-history__time">{{ formatTime(h.createdAt) }}</div>
          </div>
          <div class="resume-history__score" :style="{ color: scoreColor(h.totalScore) }">{{ h.totalScore }}</div>
          <el-button
            link
            type="danger"
            size="small"
            @click.stop="handleDelete(h.id)"
          >
            删除
          </el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Loading } from '@element-plus/icons-vue';
import { useLoveMasterStore } from '../store/loveMasterStore';
import {
  analyzeResume,
  optimizeResume,
  listResumeReviews,
  getResumeReview,
  deleteResumeReview,
  type ResumeReviewResultDto,
  type ResumeReviewSummaryDto,
} from '../api/resume';

const store = useLoveMasterStore();
const theme = store.theme;

const resumeText = ref('');
const targetPosition = ref('');
const loading = ref(false);
const optimizing = ref(false);
const currentRecordId = ref<number | null>(null);
const result = ref<ResumeReviewResultDto | null>(null);
const history = ref<ResumeReviewSummaryDto[]>([]);
const currentHistoryId = ref<number | null>(null);

/** 分数 → 颜色：90+ 绿 / 75+ 蓝 / 60+ 橙 / 以下红 */
function scoreColor(score: number): string {
  if (score >= 90) return '#67c23a';
  if (score >= 75) return '#409eff';
  if (score >= 60) return '#e6a23c';
  return '#f56c6c';
}

function formatTime(t?: string): string {
  if (!t) return '';
  const d = new Date(t);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

async function handleAnalyze() {
  const text = resumeText.value.trim();
  if (!text) {
    ElMessage.warning('请先粘贴简历内容');
    return;
  }
  loading.value = true;
  result.value = null;
  currentRecordId.value = null;
  currentHistoryId.value = null;
  try {
    const res = await analyzeResume({
      resumeText: text,
      targetPosition: targetPosition.value.trim() || undefined,
    });
    currentRecordId.value = res.data.id ?? null;
    result.value = res.data.result;
    ElMessage.success(`评分完成：${res.data.result.totalScore} 分`);
    await loadHistoryList();
  } catch {
    // 错误提示由 http 拦截器处理
  } finally {
    loading.value = false;
  }
}

/** 第二步：生成优化版简历（2 分，需先完成分析） */
async function handleOptimize() {
  if (!currentRecordId.value) return;
  optimizing.value = true;
  try {
    const res = await optimizeResume(currentRecordId.value);
    result.value = res.data;
    ElMessage.success('优化版简历已生成');
  } catch {
    // 错误提示由 http 拦截器处理
  } finally {
    optimizing.value = false;
  }
}

async function copyResume() {
  if (!result.value?.improvedResume) return;
  try {
    await navigator.clipboard.writeText(result.value.improvedResume);
    ElMessage.success('已复制优化版简历');
  } catch {
    ElMessage.error('复制失败，请手动选择复制');
  }
}

async function loadHistoryList() {
  try {
    const res = await listResumeReviews();
    history.value = (res.data ?? []).map((h) => ({ ...h, totalScore: h.totalScore ?? 0 }));
  } catch {
    // 静默
  }
}

async function loadHistory(id: number) {
  currentHistoryId.value = id;
  currentRecordId.value = id;
  try {
    const res = await getResumeReview(id);
    result.value = res.data.result;
    resumeText.value = res.data.resumeText ?? resumeText.value;
    targetPosition.value = res.data.targetPosition ?? targetPosition.value;
  } catch {
    // 静默
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除这条评分记录吗？', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }
  try {
    await deleteResumeReview(id);
    ElMessage.success('已删除');
    if (currentHistoryId.value === id) {
      result.value = null;
      currentHistoryId.value = null;
    }
    await loadHistoryList();
  } catch {
    // 错误提示由拦截器处理
  }
}

onMounted(() => {
  loadHistoryList();
});
</script>

<style scoped>
.resume-page {
  min-height: 100vh;
  padding: var(--app-space-xl);
  background: linear-gradient(165deg, #f6f8fb 0%, #eef2f7 50%, #e6ebf2 100%);
  color: var(--app-text);
}

.theme-dark .resume-page {
  background: linear-gradient(165deg, #14171c 0%, #101318 50%, #0d1014 100%);
}

.resume-page__bar {
  max-width: 1080px;
  margin: 0 auto var(--app-space-lg);
  display: flex;
  align-items: center;
  gap: var(--app-space-md);
}

.resume-page__back {
  font-size: 14px;
  color: var(--app-accent-blue);
  text-decoration: none;
}

.resume-page__back:hover {
  text-decoration: underline;
}

.resume-page__title {
  font-size: 20px;
  font-weight: 800;
}

.resume-layout {
  max-width: 1080px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: var(--app-space-lg);
  align-items: start;
}

.resume-card {
  background: var(--app-card);
  border: 2px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  padding: var(--app-space-lg);
  box-shadow: 0 4px 0 var(--app-border), 0 12px 40px rgba(0, 0, 0, 0.06);
}

.resume-card__title {
  margin: 0 0 var(--app-space-sm);
  font-size: 17px;
  font-weight: 700;
}

.resume-card__desc {
  margin: 0 0 var(--app-space-md);
  font-size: 13px;
  color: var(--app-text-secondary);
  line-height: 1.6;
}

.resume-textarea {
  width: 100%;
}

.resume-position {
  margin-top: var(--app-space-md);
  width: 100%;
}

.resume-submit {
  margin-top: var(--app-space-md);
  width: 100%;
  border-radius: 9999px;
  font-weight: 600;
}

/* 结果区 */
.resume-score {
  text-align: center;
  padding: var(--app-space-lg) 0 var(--app-space-md);
  border-bottom: 1px solid var(--app-border);
}

.resume-score__num {
  font-size: 64px;
  font-weight: 800;
  line-height: 1;
}

.resume-score__label {
  margin-top: var(--app-space-xs);
  font-size: 13px;
  color: var(--app-text-secondary);
}

.resume-section {
  margin-top: var(--app-space-lg);
}

.resume-section__title {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: var(--app-space-sm);
}

.resume-section__title--row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.resume-section__text {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--app-text-secondary);
}

.resume-dim {
  margin-bottom: var(--app-space-md);
}

.resume-dim__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.resume-dim__name {
  font-size: 13px;
  font-weight: 600;
}

.resume-dim__score {
  font-size: 13px;
  font-weight: 700;
  color: var(--app-text-secondary);
}

.resume-dim__comment {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--app-text-secondary);
  line-height: 1.6;
}

.resume-dim__suggestion {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--app-accent-blue);
  line-height: 1.6;
}

.resume-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--app-space-xs);
}

.resume-tags :deep(.el-tag) {
  white-space: normal;
  height: auto;
  line-height: 1.5;
  padding: 4px 8px;
}

.resume-improved {
  margin: 0;
  padding: var(--app-space-md);
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-md);
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 420px;
  overflow: auto;
  font-family: inherit;
}

.theme-dark .resume-improved {
  background: rgba(255, 255, 255, 0.06);
}

.resume-empty {
  text-align: center;
  padding: 80px var(--app-space-lg);
  color: var(--app-text-secondary);
  font-size: 14px;
  line-height: 2;
}

/* AI 评审中的友好加载提示（长简历耗时长，避免用户干等） */
.resume-loading {
  text-align: center;
  padding: 70px var(--app-space-lg);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.resume-loading__icon {
  font-size: 42px;
  color: var(--app-primary);
}

.resume-loading__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text);
}

.resume-loading__desc {
  font-size: 13px;
  color: var(--app-text-secondary);
  line-height: 1.8;
}

/* 优化版简历待生成提示 */
.resume-optimize-tip {
  margin: 10px 0 0;
  font-size: 13px;
  color: var(--app-text-secondary);
  line-height: 1.7;
}

.resume-empty__icon {
  font-size: 44px;
  margin-bottom: var(--app-space-sm);
}

/* 历史记录 */
.resume-history {
  max-width: 1080px;
  margin: var(--app-space-lg) auto 0;
}

.resume-history__list {
  display: flex;
  flex-direction: column;
  gap: var(--app-space-sm);
}

.resume-history__item {
  display: flex;
  align-items: center;
  gap: var(--app-space-md);
  padding: var(--app-space-md);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-md);
  cursor: pointer;
  transition: border-color 0.2s;
}

.resume-history__item:hover {
  border-color: var(--el-color-primary-light-5);
}

.resume-history__item--active {
  border-color: var(--el-color-primary);
  outline: 2px solid rgba(64, 158, 255, 0.3);
}

.resume-history__info {
  flex: 1;
  min-width: 0;
}

.resume-history__pos {
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resume-history__time {
  font-size: 12px;
  color: var(--app-text-secondary);
  margin-top: 2px;
}

.resume-history__score {
  font-size: 22px;
  font-weight: 800;
}

@media (max-width: 900px) {
  .resume-page {
    padding: var(--app-space-md);
  }

  .resume-layout {
    grid-template-columns: 1fr;
  }
}
</style>
