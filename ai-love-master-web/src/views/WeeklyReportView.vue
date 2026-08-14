<template>
  <div class="wr-page">
    <div class="wr-page__bar">
      <router-link to="/user-center" class="wr-page__back">← 返回个人中心</router-link>
      <h1 class="wr-page__title">学习周报</h1>
      <span v-if="report.week" class="wr-page__week">{{ report.week }}</span>
      <el-button v-if="loaded" type="primary" round size="small" class="wr-page__share" @click="showShareCard = true">
        📸 生成分享卡
      </el-button>
    </div>

    <div v-if="!loaded" class="wr-skeleton">
      <el-skeleton animated class="wr-skeleton__advice">
        <template #template>
          <el-skeleton-item variant="text" style="width: 90%; height: 16px" />
          <el-skeleton-item variant="text" style="width: 60%; height: 16px" />
        </template>
      </el-skeleton>
      <div class="wr-skeleton__grid">
        <el-skeleton v-for="n in 4" :key="n" animated class="wr-skeleton__card">
          <template #template>
            <el-skeleton-item variant="text" style="width: 40%; height: 14px" />
            <el-skeleton-item variant="h3" style="width: 55%; height: 32px" />
            <el-skeleton-item variant="text" style="width: 80%; height: 12px" />
          </template>
        </el-skeleton>
      </div>
    </div>

    <template v-else>
      <!-- 周报建议 -->
      <div class="wr-advice">
        <span class="wr-advice__icon">💡</span>
        <p class="wr-advice__text">{{ report.advice }}</p>
      </div>

      <!-- 数据卡片 -->
      <div class="wr-grid">
        <div class="wr-card">
          <div class="wr-card__head"><span class="wr-card__icon">💬</span><span class="wr-card__label">对话探索</span></div>
          <div class="wr-card__value app-num">{{ report.conversation?.count ?? 0 }}</div>
          <div class="wr-card__sub">次 AI 咨询</div>
          <div v-if="report.conversation?.topics?.length" class="wr-card__topics">
            <el-tag v-for="t in report.conversation.topics" :key="t" size="small" effect="plain" class="wr-card__topic">{{ t }}</el-tag>
          </div>
          <div v-else class="wr-card__sub">本周暂无主题记录</div>
        </div>

        <div class="wr-card">
          <div class="wr-card__head"><span class="wr-card__icon">📅</span><span class="wr-card__label">学习投入</span></div>
          <div class="wr-card__value app-num">{{ report.learning?.signDays ?? 0 }}<small class="wr-card__unit">天</small></div>
          <div class="wr-card__sub">
            签到 {{ report.learning?.signDays ?? 0 }} 天 · 八股打卡 {{ report.learning?.checkinDays ?? 0 }} 天
          </div>
          <div class="wr-card__sub">错题新增 {{ report.learning?.newWrong ?? 0 }} · 已掌握 {{ report.learning?.masteredWrong ?? 0 }}</div>
        </div>

        <div class="wr-card">
          <div class="wr-card__head"><span class="wr-card__icon">🎯</span><span class="wr-card__label">求职产出</span></div>
          <div class="wr-card__value app-num">{{ report.output?.resumeReviews ?? 0 }}</div>
          <div class="wr-card__sub">次简历评分</div>
          <div class="wr-card__sub">成就 {{ report.achievements?.unlocked ?? 0 }}/{{ report.achievements?.total ?? 0 }} 枚</div>
        </div>

        <div class="wr-card">
          <div class="wr-card__head"><span class="wr-card__icon">🪙</span><span class="wr-card__label">积分账本</span></div>
          <div class="wr-card__value app-num" :class="netClass">{{ report.points?.net ?? 0 }}</div>
          <div class="wr-card__sub">本周净变化（赚 {{ report.points?.earned ?? 0 }} / 花 {{ report.points?.spent ?? 0 }}）</div>
          <div v-if="(report.points?.redeemCount ?? 0) > 0" class="wr-card__sub">积分商城兑换 {{ report.points.redeemCount }} 次</div>
        </div>
      </div>

      <!-- 说明 -->
      <p class="wr-note">数据每早自动统计本周（周一起）的学习轨迹，下一周从零开始计数。</p>
    </template>

    <!-- 背景装饰 -->
    <div class="wr-page__bg" aria-hidden="true">
      <span class="app-orb app-orb--blue wr-orb wr-orb--1" />
      <span class="app-orb app-orb--purple wr-orb wr-orb--2" />
    </div>
  </div>

  <ShareCardDialog
    v-model="showShareCard"
    title="我的学习周报"
    :subtitle="report.week || '本周学习成果'"
    :items="shareItems"
    slogan="越努力，越幸运 · 下一周继续加油"
  />
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { getWeeklyReport, type WeeklyReport } from '../api/user';
import ShareCardDialog from '../components/ShareCardDialog.vue';

const report = ref<WeeklyReport>({
  week: '',
  conversation: { count: 0, topics: [] },
  learning: { signDays: 0, checkinDays: 0, newWrong: 0, masteredWrong: 0 },
  output: { resumeReviews: 0, interviews: 0 },
  points: { earned: 0, spent: 0, net: 0, redeemCount: 0 },
  achievements: { unlocked: 0, total: 0 },
  advice: '',
});
const loaded = ref(false);

const netClass = computed(() => {
  const net = report.value.points?.net ?? 0;
  return net > 0 ? 'is-plus' : net < 0 ? 'is-minus' : '';
});

const showShareCard = ref(false);

/** 分享卡数据（与页面数据卡片对应，社交传播获客） */
const shareItems = computed(() => [
  { label: 'AI 对话', value: `${report.value.conversation?.count ?? 0} 次` },
  { label: '学习签到', value: `${report.value.learning?.signDays ?? 0} 天` },
  { label: '简历评分', value: `${report.value.output?.resumeReviews ?? 0} 次` },
  { label: '面试模拟', value: `${report.value.output?.interviews ?? 0} 场` },
  { label: '积分净变', value: `${(report.value.points?.net ?? 0) > 0 ? '+' : ''}${report.value.points?.net ?? 0}` },
  { label: '成就解锁', value: `${report.value.achievements?.unlocked ?? 0}/${report.value.achievements?.total ?? 0}` },
]);

onMounted(async () => {
  try {
    const res = await getWeeklyReport();
    if (res.data) report.value = res.data;
  } catch {
    // 拦截器已提示
  } finally {
    loaded.value = true;
  }
});
</script>

<style scoped>
.wr-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: linear-gradient(180deg, #f8fbfe 0%, #f0f4fa 55%, #eaf0f8 100%);
  color: var(--app-text);
  padding: 0 var(--app-space-xl) 60px;
}

.theme-dark .wr-page {
  background: linear-gradient(180deg, #10141c 0%, #0d1118 55%, #0a0e14 100%);
}

.wr-page__bar {
  position: relative;
  z-index: 2;
  max-width: 860px;
  width: 100%;
  margin: 0 auto;
  padding: var(--app-space-lg) 0;
  display: flex;
  align-items: center;
  gap: var(--app-space-lg);
  flex-wrap: wrap;
}

.wr-page__back {
  font-size: 14px;
  color: var(--app-primary);
  text-decoration: none;
}

.wr-page__title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  flex: 1;
}

.wr-page__week {
  font-size: 13px;
  color: var(--app-text-secondary);
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 9999px;
  padding: 4px 14px;
  box-shadow: var(--app-shadow-sm);
}

.wr-loading {
  position: relative;
  z-index: 2;
  text-align: center;
  color: var(--app-text-secondary);
  padding: 80px 0;
}

.wr-skeleton {
  position: relative;
  z-index: 2;
  max-width: 860px;
  width: 100%;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.wr-skeleton__advice {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.wr-skeleton__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: var(--app-space-lg);
}

.wr-skeleton__card {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  box-shadow: var(--app-shadow-sm);
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.wr-advice {
  position: relative;
  z-index: 2;
  max-width: 860px;
  width: 100%;
  margin: 0 auto 16px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: linear-gradient(135deg, #eef4ff 0%, #f7f9ff 100%);
  border: 1px solid #dbe6ff;
  border-radius: var(--app-radius-lg);
  padding: 16px 20px;
  animation: app-fade-up 0.5s ease both;
}

.theme-dark .wr-advice {
  background: linear-gradient(135deg, #1a2233 0%, #121826 100%);
  border-color: #2a3550;
}

.wr-advice__icon {
  font-size: 20px;
}

.wr-advice__text {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
}

.wr-grid {
  position: relative;
  z-index: 2;
  max-width: 860px;
  width: 100%;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: var(--app-space-lg);
}

.wr-card {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  box-shadow: var(--app-shadow-md);
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  animation: app-fade-up 0.5s ease both;
}

.wr-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.wr-card__icon {
  font-size: 16px;
}

.wr-card__label {
  font-size: 13px;
  color: var(--app-text-secondary);
  font-weight: 600;
}

.wr-card__value {
  font-size: 34px;
  font-weight: 800;
  color: var(--app-primary);
  line-height: 1.1;
}

.wr-card__value.is-plus {
  color: #16a34a;
}

.wr-card__value.is-minus {
  color: #ef4444;
}

.wr-card__unit {
  font-size: 14px;
  color: var(--app-text-secondary);
  font-weight: 500;
  margin-left: 2px;
}

.wr-card__sub {
  font-size: 12px;
  color: var(--app-text-secondary);
  line-height: 1.6;
}

.wr-card__topics {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 2px;
}

.wr-note {
  position: relative;
  z-index: 2;
  max-width: 860px;
  width: 100%;
  margin: 20px auto 0;
  text-align: center;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.wr-page__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.wr-orb--1 {
  width: 380px;
  height: 380px;
  top: -120px;
  right: -100px;
}

.wr-orb--2 {
  width: 320px;
  height: 320px;
  bottom: -80px;
  left: -100px;
  animation-delay: 2s;
}

@media (max-width: 767px) {
  .wr-page {
    padding: 0 var(--app-space-md) 40px;
  }
}
</style>
