<template>
  <div class="home">
    <!-- 顶部导航 -->
    <header class="home__nav">
      <div class="home__brand">
        <span class="home__brand-logo pixel-font">AI</span>
        <span class="home__brand-name">职规助手</span>
        <small class="home__brand-sub pixel-font">career-master</small>
      </div>
      <div class="home__nav-right">
        <template v-if="authStore.isAuthenticated()">
          <el-dropdown trigger="click" @command="handleUserCommand">
            <span class="home__user-trigger">
              <el-avatar :size="26" class="home__user-avatar" :src="authStore.avatar || undefined">{{ authStore.avatar ? '' : (authStore.username || '我')[0] }}</el-avatar>
              <span class="home__user-name">{{ authStore.username }}</span>
              <span v-if="points > 0 || level === 'VIP'" class="home__points-badge" title="点击查看积分明细">⚡ {{ points }}</span>
              <el-tag v-if="level === 'VIP'" type="warning" size="small" effect="dark" class="home__vip-tag">VIP</el-tag>
              <el-icon class="home__user-caret"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="center">个人中心</el-dropdown-item>
                <el-dropdown-item command="feedback">意见反馈</el-dropdown-item>
                <el-dropdown-item v-if="authStore.isAdmin()" command="admin">管理后台</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <router-link to="/login" class="home__link">登录</router-link>
          <router-link to="/register" class="home__link home__link--primary">注册</router-link>
        </template>
      </div>
    </header>

    <!-- 主视觉：品牌标题 + 打字机标语 -->
    <main class="home__hero">
      <h1 class="home__title">AI 职规助手</h1>
      <p class="home__slogan">
        <span class="home__slogan-prefix">"</span>
        <span class="home__slogan-text">{{ typedSlogan }}<span class="home__slogan-cursor">|</span></span>
        <span class="home__slogan-suffix">"</span>
      </p>
      <p class="home__subtitle">职业规划 · 学习路线 · 校招备战 · 面试辅导</p>

      <!-- 快捷入口 -->
      <div class="home__quick">
        <router-link to="/navigate" class="home__quick-link">
          <PixelIcon name="globe" :size="18" /> 网站导航
        </router-link>
        <router-link to="/feedback" class="home__quick-link">
          <PixelIcon name="comment" :size="18" /> 意见反馈
        </router-link>
        <template v-if="authStore.isAuthenticated()">
          <router-link to="/bagu" class="home__quick-link">
            <PixelIcon name="book-open" :size="18" /> 八股练习
          </router-link>
          <router-link to="/bagu-practice" class="home__quick-link">
            <PixelIcon name="note" :size="18" /> 错题本
          </router-link>
          <router-link to="/interview-records" class="home__quick-link">
            <PixelIcon name="star" :size="18" /> 面试记录
          </router-link>
          <router-link to="/shop" class="home__quick-link">
            <PixelIcon name="heart" :size="18" /> 积分商城
          </router-link>
        </template>
      </div>

      <!-- 状态条：实时时钟 + 倒计时 + 快捷签到 -->
      <div class="home__statusbar">
        <span class="home__statusbar-item">
          <PixelIcon name="clock" :size="14" /> <span class="pixel-font">{{ clockText }}</span>
        </span>
        <span class="home__statusbar-item">🏖 距周末 <span class="pixel-font">{{ weekendText }}</span></span>
        <span class="home__statusbar-item">🎉 距{{ holidayName }} <span class="pixel-font">{{ holidayText }}</span></span>
        <span v-if="authStore.isAuthenticated()" class="home__statusbar-item">
          <el-button
            v-if="!signedToday"
            link
            type="primary"
            size="small"
            :loading="signing"
            class="home__signin-btn"
            @click="handleQuickSignIn"
          >📅 签到 +5</el-button>
          <span v-else>📅 已签到 · 连续 <span class="pixel-font">{{ streakDays }}</span> 天</span>
        </span>
      </div>

      <!-- 数据统计卡片（商用仪表盘，参考工作台排版） -->
      <div v-if="authStore.isAuthenticated()" class="home__stats">
        <div class="home__stat">
          <span class="home__stat-icon">⚡</span>
          <div class="home__stat-body">
            <span class="home__stat-num app-num">{{ displayedPoints }}</span>
            <span class="home__stat-label">积分余额</span>
          </div>
        </div>
        <div class="home__stat">
          <span class="home__stat-icon">📅</span>
          <div class="home__stat-body">
            <span class="home__stat-num app-num">{{ streakDays }}</span>
            <span class="home__stat-label">连续签到 · 天</span>
          </div>
        </div>
        <div class="home__stat">
          <span class="home__stat-icon">👑</span>
          <div class="home__stat-body">
            <span class="home__stat-num">{{ level === 'VIP' ? 'VIP' : 'FREE' }}</span>
            <span class="home__stat-label">会员等级</span>
          </div>
        </div>
        <div class="home__stat">
          <span class="home__stat-icon">🎁</span>
          <div class="home__stat-body">
            <span class="home__stat-num app-num">{{ invitedCount }}</span>
            <span class="home__stat-label">已邀请好友</span>
          </div>
        </div>
      </div>

      <!-- 签到周期进度（7 天解锁 +10） -->
      <div v-if="authStore.isAuthenticated() && streakDays > 0" class="home__streak">
        <span class="home__streak-text">
          签到周期 <b class="app-num">{{ streakInCycle }}/7</b> 天 · 再签 <b class="app-num">{{ remainToBonus }}</b> 天解锁 +10
        </span>
        <el-progress :percentage="streakPct" :stroke-width="6" :show-text="false" color="#2f6bff" class="home__streak-bar" />
      </div>

      <!-- 多引擎搜索框 -->
      <div class="home__search">
        <el-select v-model="searchEngine" size="large" class="home__search-engine">
          <el-option v-for="e in engines" :key="e.value" :label="e.label" :value="e.value" />
        </el-select>
        <el-input
          v-model="searchText"
          size="large"
          placeholder="搜索一下，或直接输入网址访问…"
          class="home__search-input"
          clearable
          @keyup.enter="doSearch"
        />
        <el-button type="primary" size="large" class="home__search-btn pixel-btn" @click="doSearch">
          搜索
        </el-button>
      </div>
    </main>

    <!-- 数据面板区：左侧学习仪表盘 + 右侧动态天气 -->
    <section class="home__panels">
      <DashboardPanel
        class="home__panel home__panel--dash"
        :points="points"
        :level="level"
        :streak-days="streakDays"
        :streak-in-cycle="streakInCycle"
        :streak-pct="streakPct"
        :remain-to-bonus="remainToBonus"
        :invited-count="invitedCount"
        :achv-unlocked="achvUnlocked"
        :achv-total="achvTotal"
      />
      <WeatherPanel class="home__panel home__panel--weather" />
    </section>

    <!-- 应用卡片：3D 翻转 -->
    <section class="home__apps">
      <div
        class="app-card pixel-hover"
        @click="goToApp('/career-master')"
        @mousemove="onCardMove($event, $event.currentTarget as HTMLElement)"
        @mouseleave="onCardLeave($event.currentTarget as HTMLElement)"
      >
        <div class="app-card__inner">
          <div class="app-card__face app-card__face--front">
            <div class="app-card__icon"><PixelIcon name="briefcase" :size="52" /></div>
            <h3 class="app-card__name">职规大师</h3>
            <p class="app-card__desc">不懂怎么规划职业？<br />简历、面试、校招随时问</p>
            <span class="app-card__hint">悬停看看能帮你做什么 →</span>
          </div>
          <div class="app-card__face app-card__face--back">
            <h3 class="app-card__back-title">职规大师</h3>
            <ul class="app-card__features">
              <li>帮你定方向，不再迷茫</li>
              <li>教你写简历、过面试</li>
              <li>规划学习路线和时间</li>
              <li>校招实习一手攻略</li>
            </ul>
            <span class="app-card__go">去聊聊 →</span>
          </div>
        </div>
      </div>

      <div
        class="app-card pixel-hover"
        @click="goToApp('/super-agent')"
        @mousemove="onCardMove($event, $event.currentTarget as HTMLElement)"
        @mouseleave="onCardLeave($event.currentTarget as HTMLElement)"
      >
        <div class="app-card__inner">
          <div class="app-card__face app-card__face--front">
            <div class="app-card__icon"><PixelIcon name="cpu" :size="52" /></div>
            <h3 class="app-card__name">超级智能体</h3>
            <p class="app-card__desc">一个任务，AI 帮你跑完<br />查资料、找地点、出 PDF 一条龙</p>
            <span class="app-card__hint">悬停看看能帮你做什么 →</span>
          </div>
          <div class="app-card__face app-card__face--back">
            <h3 class="app-card__back-title">超级智能体</h3>
            <ul class="app-card__features">
              <li>帮我查附近图书馆</li>
              <li>上网搜最新资料</li>
              <li>生成学习计划 PDF</li>
              <li>保存笔记和文件</li>
            </ul>
            <span class="app-card__go">去试试 →</span>
          </div>
        </div>
      </div>

      <div
        class="app-card pixel-hover"
        @click="goToApp('/bagu')"
        @mousemove="onCardMove($event, $event.currentTarget as HTMLElement)"
        @mouseleave="onCardLeave($event.currentTarget as HTMLElement)"
      >
        <div class="app-card__inner">
          <div class="app-card__face app-card__face--front">
            <div class="app-card__icon"><PixelIcon name="book-open" :size="52" /></div>
            <h3 class="app-card__name">八股练习场</h3>
            <p class="app-card__desc">面试八股随便刷<br />按主题速览 + 随机抽题</p>
            <span class="app-card__hint">悬停看看能帮你做什么 →</span>
          </div>
          <div class="app-card__face app-card__face--back">
            <h3 class="app-card__back-title">八股练习场</h3>
            <ul class="app-card__features">
              <li>按主题刷八股，不怕问</li>
              <li>不会的搜一下就有</li>
              <li>碎片时间随机来一题</li>
              <li>不懂让 AI 讲给你听</li>
            </ul>
            <span class="app-card__go">去刷题 →</span>
          </div>
        </div>
      </div>

      <div
        class="app-card pixel-hover"
        @click="goToApp('/resume-review')"
        @mousemove="onCardMove($event, $event.currentTarget as HTMLElement)"
        @mouseleave="onCardLeave($event.currentTarget as HTMLElement)"
      >
        <div class="app-card__inner">
          <div class="app-card__face app-card__face--front">
            <div class="app-card__icon"><PixelIcon name="card-text" :size="52" /></div>
            <h3 class="app-card__name">简历评分</h3>
            <p class="app-card__desc">简历行不行？AI 帮你把关<br />分维度打分 + 优化版简历</p>
            <span class="app-card__hint">悬停看看能帮你做什么 →</span>
          </div>
          <div class="app-card__face app-card__face--back">
            <h3 class="app-card__back-title">简历评分</h3>
            <ul class="app-card__features">
              <li>6 大维度量化打分</li>
              <li>项目经历深度诊断</li>
              <li>量化成果优化建议</li>
              <li>一键生成优化版简历</li>
            </ul>
            <span class="app-card__go">去评分 →</span>
          </div>
        </div>
      </div>

      <div
        class="app-card pixel-hover"
        @click="goToApp('/interview')"
        @mousemove="onCardMove($event, $event.currentTarget as HTMLElement)"
        @mouseleave="onCardLeave($event.currentTarget as HTMLElement)"
      >
        <div class="app-card__inner">
          <div class="app-card__face app-card__face--front">
            <div class="app-card__icon"><PixelIcon name="comment" :size="52" /></div>
            <h3 class="app-card__name">面试模拟</h3>
            <p class="app-card__desc">AI 面试官按岗位出 5 题<br />逐题点评 + 总结报告</p>
            <span class="app-card__hint">悬停看看能帮你做什么 →</span>
          </div>
          <div class="app-card__face app-card__face--back">
            <h3 class="app-card__back-title">面试模拟</h3>
            <ul class="app-card__features">
              <li>按岗位抽题，5 题一轮</li>
              <li>作答后 AI 实时点评打分</li>
              <li>三维度评分 + 参考要点</li>
              <li>总结报告看整体水平</li>
            </ul>
            <span class="app-card__go">去面试 →</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 本周学习概览（复用周报数据，填充页面底部空白） -->
    <section v-if="authStore.isAuthenticated() && weekly" class="home__weekly">
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

    <!-- 背景装饰：柔和光斑 + 漂浮圆点 + 像素云/星 -->
    <div class="home__bg" aria-hidden="true">
      <span class="app-orb app-orb--blue home__orb home__orb--1" />
      <span class="app-orb app-orb--orange home__orb home__orb--2" />
      <span class="app-orb app-orb--purple home__orb home__orb--3" />
      <span
        v-for="n in 14"
        :key="n"
        class="home__dot"
        :style="dotStyle(n)"
      />
      <!-- 背景装饰 -->
      <div class="home-page__bg" aria-hidden="true">
        <span class="app-orb app-orb--blue home-orb home-orb--1" />
        <span class="app-orb app-orb--purple home-orb home-orb--2" />
      </div>
    </div>

    <!-- 最新公告弹窗 -->
    <el-dialog
      v-model="showNoticeDialog"
      title="📢 最新公告"
      width="min(440px, 92vw)"
      :close-on-click-modal="true"
      align-center
    >
      <div class="home__notice-title">{{ latestNotice?.title }}</div>
      <div class="home__notice-content">{{ latestNotice?.content }}</div>
      <template #footer>
        <el-button @click="closeNotice">知道了</el-button>
        <router-link to="/notice" @click="closeNotice">
          <el-button type="primary">查看全部公告</el-button>
        </router-link>
      </template>
    </el-dialog>

    <!-- 页脚 -->
    <footer class="home__footer">
      <span>© 2026 AI 职规助手</span>
      <span class="home__footer-sep">·</span>
      <router-link to="/agreement" class="home__footer-link">用户协议</router-link>
      <span class="home__footer-sep">·</span>
      <router-link to="/agreement#privacy" class="home__footer-link">隐私政策</router-link>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { ArrowDown } from '@element-plus/icons-vue';
import PixelIcon from '../components/PixelIcon.vue';
import DashboardPanel from '../components/DashboardPanel.vue';
import WeatherPanel from '../components/WeatherPanel.vue';
import { useAuthStore } from '../store/authStore';
import { getLatestNotice, type Notice } from '../api/notice';
import { getAchievements, getInvite, getPoints, getWeeklyReport, signIn, type WeeklyReport } from '../api/user';
import { useCountUp } from '../composables/useCountUp';

const router = useRouter();
const authStore = useAuthStore();

/* ===== 首页积分/签到（登录后顶栏徽章 + 状态条快捷签到） ===== */
const points = ref(0);
const level = ref('FREE');
const signedToday = ref(false);
const streakDays = ref(0);
const signing = ref(false);
const invitedCount = ref(0);
const achvUnlocked = ref(0);
const achvTotal = ref(0);
const weekly = ref<WeeklyReport | null>(null);

/* 积分数字滚动 + 签到 7 天周期进度 */
const displayedPoints = useCountUp(computed(() => points.value));
const streakInCycle = computed(() => {
  const d = streakDays.value % 7;
  return d === 0 ? 7 : d;
});
const remainToBonus = computed(() => 7 - streakInCycle.value);
const streakPct = computed(() => Math.round((streakInCycle.value / 7) * 100));

/** 加载积分画像（登录时） */
async function loadPoints() {
  if (!authStore.isAuthenticated()) return;
  try {
    const res = await getPoints();
    points.value = res.data?.points ?? 0;
    level.value = res.data?.level ?? 'FREE';
    signedToday.value = !!res.data?.signedToday;
    streakDays.value = res.data?.streakDays ?? 0;
  } catch {
    // 拦截器已提示
  }
}

/** 加载邀请数据（首页统计卡） */
async function loadInviteCount() {
  if (!authStore.isAuthenticated()) return;
  try {
    const res = await getInvite();
    invitedCount.value = res.data?.invitedCount ?? 0;
  } catch {
    // 拦截器已提示
  }
}

/** 加载成就解锁数（仪表盘展示） */
async function loadAchievements() {
  if (!authStore.isAuthenticated()) return;
  try {
    const res = await getAchievements();
    achvUnlocked.value = (res.data ?? []).filter((a) => a.unlocked).length;
    achvTotal.value = (res.data ?? []).length;
  } catch {
    // 拦截器已提示
  }
}

/** 加载本周学习概览（首页数据条，复用周报聚合） */
async function loadWeekly() {
  if (!authStore.isAuthenticated()) return;
  try {
    const res = await getWeeklyReport();
    weekly.value = res.data ?? null;
  } catch {
    // 拦截器已提示
  }
}

/** 状态条快捷签到（幂等） */
async function handleQuickSignIn() {
  signing.value = true;
  try {
    const res = await signIn();
    ElMessageBox.confirm(`签到成功 +${res.data?.points ?? 0} 分${res.data?.bonus ? '（连续奖励）' : ''}，去个人中心查看明细？`, '签到成功', {
      confirmButtonText: '查看积分',
      cancelButtonText: '继续逛逛',
      type: 'success',
    })
      .then(() => router.push('/user-center'))
      .catch(() => {});
    await loadPoints();
  } catch {
    // 拦截器已提示
  } finally {
    signing.value = false;
  }
}

/* ===== 最新公告弹窗 ===== */
const latestNotice = ref<Notice | null>(null);
const showNoticeDialog = ref(false);

async function checkNotice() {
  try {
    const res = await getLatestNotice();
    const n = res.data;
    if (!n) return;
    const key = `notice_read_${n.id}`;
    if (localStorage.getItem(key)) return;
    latestNotice.value = n;
    showNoticeDialog.value = true;
  } catch {
    // 公告拉取失败不阻塞页面
  }
}

function closeNotice() {
  if (latestNotice.value) {
    localStorage.setItem(`notice_read_${latestNotice.value.id}`, '1');
  }
  showNoticeDialog.value = false;
}

function handleLogout() {
  authStore.logout();
  router.push('/');
}

function handleUserCommand(command: string) {
  if (command === 'center') router.push('/user-center');
  else if (command === 'feedback') router.push('/feedback');
  else if (command === 'admin') router.push('/admin');
  else if (command === 'logout') handleLogout();
}

/** 访客点击功能卡片：弹窗引导登录 */
function goToApp(path: string) {
  if (authStore.isAuthenticated()) {
    router.push(path);
    return;
  }
  ElMessageBox.confirm('当前为访客模式，如需体验完整功能请先登录', '提示', {
    confirmButtonText: '去登录',
    cancelButtonText: '暂不登录',
    type: 'warning',
  })
    .then(() => router.push({ name: 'login', query: { redirect: path } }))
    .catch(() => {});
}

/* ===== 多引擎搜索 ===== */
const engines = [
  { label: '百度', value: 'baidu', url: (q: string) => `https://www.baidu.com/s?wd=${encodeURIComponent(q)}` },
  { label: 'Google', value: 'google', url: (q: string) => `https://www.google.com/search?q=${encodeURIComponent(q)}` },
  { label: 'CSDN', value: 'csdn', url: (q: string) => `https://so.csdn.net/so/search?q=${encodeURIComponent(q)}` },
  { label: 'Bing', value: 'bing', url: (q: string) => `https://www.bing.com/search?q=${encodeURIComponent(q)}` },
  { label: 'DuckDuckGo', value: 'duckduckgo', url: (q: string) => `https://duckduckgo.com/?q=${encodeURIComponent(q)}` },
];
const searchEngine = ref('baidu');
const searchText = ref('');

function looksLikeUrl(s: string) {
  if (/^https?:\/\//i.test(s)) return true;
  return /^([\w-]+\.)+[a-zA-Z]{2,}(:\d+)?(\/\S*)?$/.test(s) || /^\d{1,3}(\.\d{1,3}){3}(:\d+)?$/.test(s);
}

/** 打开外部链接：优先新标签页；若被拦截（如 iframe 预览沙箱），降级为当前页打开 */
function openExternal(url: string) {
  const win = window.open(url, '_blank');
  if (!win) {
    window.location.href = url;
  }
}

function doSearch() {
  const q = searchText.value.trim();
  if (!q) return;
  if (looksLikeUrl(q)) {
    const target = /^https?:\/\//i.test(q) ? q : `http://${q}`;
    openExternal(target);
    return;
  }
  const engine = engines.find((e) => e.value === searchEngine.value) ?? engines[0]!;
  openExternal(engine.url(q));
}

/* ===== 实时时钟 + 倒计时 ===== */
const now = ref(new Date());
let clockTimer: ReturnType<typeof setInterval> | null = null;

const clockText = computed(() =>
  now.value.toLocaleTimeString('zh-CN', { hour12: false }),
);

function countdownTo(target: Date): string {
  const diff = target.getTime() - Date.now();
  if (diff <= 0) return '今天';
  const days = Math.floor(diff / 86400000);
  const hours = Math.floor((diff % 86400000) / 3600000);
  const mins = Math.floor((diff % 3600000) / 60000);
  return `${days}天${hours}时${mins}分`;
}

function nextWeekend(): Date {
  const d = new Date();
  let diff = (6 - d.getDay() + 7) % 7;
  if (diff === 0) diff = 7;
  const t = new Date(d);
  t.setDate(t.getDate() + diff);
  t.setHours(0, 0, 0, 0);
  return t;
}

/* 节假日列表（公历；农历节日如春节/端午/中秋请按年份更新公历日期） */
interface Holiday {
  name: string;
  month: number;
  day: number;
}

const holidays: Holiday[] = [
  { name: '元旦', month: 1, day: 1 },
  { name: '春节', month: 2, day: 17 }, // 2026 正月初一，每年需更新
  { name: '清明', month: 4, day: 5 },
  { name: '劳动节', month: 5, day: 1 },
  { name: '端午', month: 6, day: 19 }, // 2026 五月初五，每年需更新
  { name: '中秋', month: 9, day: 25 }, // 2026 八月十五，每年需更新
  { name: '国庆', month: 10, day: 1 },
  { name: '圣诞', month: 12, day: 25 },
];

function nextHoliday(): Holiday & { date: Date } {
  const now = new Date();
  let best: (Holiday & { date: Date }) | null = null;
  for (const h of holidays) {
    let d = new Date(now.getFullYear(), h.month - 1, h.day, 0, 0, 0);
    if (d.getTime() < now.getTime()) {
      d = new Date(now.getFullYear() + 1, h.month - 1, h.day, 0, 0, 0);
    }
    if (!best || d.getTime() < best.date.getTime()) {
      best = { ...h, date: d };
    }
  }
  return best ?? { name: '元旦', month: 1, day: 1, date: new Date(now.getFullYear() + 1, 0, 1) };
}

/* 倒计时需依赖 now 每秒刷新 */
const weekendText = computed(() => {
  void now.value;
  return countdownTo(nextWeekend());
});

const holidayInfo = computed(() => {
  void now.value;
  return nextHoliday();
});
const holidayName = computed(() => holidayInfo.value.name);
const holidayText = computed(() => {
  void now.value;
  return countdownTo(holidayInfo.value.date);
});

function startClock() {
  clockTimer = setInterval(() => {
    now.value = new Date();
  }, 1000);
}

/* ===== 卡片 3D 倾斜跟随鼠标 ===== */
function onCardMove(e: MouseEvent, el: HTMLElement) {
  const rect = el.getBoundingClientRect();
  const px = (e.clientX - rect.left) / rect.width - 0.5;
  const py = (e.clientY - rect.top) / rect.height - 0.5;
  // 3D 倾斜 + 像素风 hover 微位移（translate 与内联 transform 合并，避免被覆盖）
  el.style.transform = `perspective(1200px) translate(-2px, -2px) rotateY(${(px * 8).toFixed(2)}deg) rotateX(${(-py * 8).toFixed(2)}deg)`;
}

function onCardLeave(el: HTMLElement) {
  el.style.transform = '';
}

/* ===== 打字机标语 ===== */
const slogans = [
  '校招准备，先定方向，再补能力。',
  '每天两道算法题，秋招不慌张。',
  '简历一页纸，亮点要前置。',
  '八股要懂原理，项目要有亮点。',
];
const typedSlogan = ref('');
let sloganIdx = 0;
let charIdx = 0;
let deleting = false;
let timer: ReturnType<typeof setTimeout> | null = null;

function typeLoop() {
  const current = slogans[sloganIdx] ?? '';
  if (!deleting) {
    typedSlogan.value = current.slice(0, ++charIdx);
    if (charIdx === current.length) {
      deleting = true;
      timer = setTimeout(typeLoop, 1800);
      return;
    }
  } else {
    typedSlogan.value = current.slice(0, --charIdx);
    if (charIdx === 0) {
      deleting = false;
      sloganIdx = (sloganIdx + 1) % slogans.length;
    }
  }
  timer = setTimeout(typeLoop, deleting ? 45 : 95);
}

onMounted(() => {
  typeLoop();
  checkNotice();
  startClock();
  if (authStore.isAuthenticated()) {
    authStore.fetchAvatar();
    loadPoints();
    loadInviteCount();
    loadAchievements();
    loadWeekly();
  }
});
onBeforeUnmount(() => {
  if (timer) clearTimeout(timer);
  if (clockTimer) clearInterval(clockTimer);
});

/* ===== 背景圆点随机样式 ===== */
function dotStyle(n: number) {
  const size = 4 + ((n * 7) % 8);
  const left = 4 + ((n * 13) % 88);
  const top = 8 + ((n * 17) % 80);
  const delay = (n % 6) * 0.7;
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    top: `${top}%`,
    animationDelay: `${delay}s`,
  };
}
</script>

<style scoped>
.home {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #f8fbfe 0%, #f0f4fa 55%, #eaf0f8 100%);
  color: var(--app-text);
  padding: 0 var(--app-space-xl);
}

.theme-dark .home {
  background: linear-gradient(165deg, #14171c 0%, #101318 50%, #0d1014 100%);
}

/* ===== 数据统计卡片（商用仪表盘） ===== */
.home__stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: var(--app-space-md);
  max-width: 720px;
  margin: 24px auto 0;
}

.home__stat {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-md);
  box-shadow: var(--app-shadow-sm);
  transition: box-shadow 0.18s ease, transform 0.18s ease;
}

.home__stat:hover {
  box-shadow: var(--app-shadow-md);
  transform: translateY(-2px);
}

.home__stat-icon {
  font-size: 22px;
  flex-shrink: 0;
}

.home__stat-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.home__stat-num {
  font-size: 22px;
  font-weight: 800;
  color: var(--app-text);
  line-height: 1.1;
}

.home__stat-label {
  font-size: 12px;
  color: var(--app-text-secondary);
}

/* 签到周期进度条 */
.home__streak {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  max-width: 720px;
  margin: 16px auto 0;
  padding: 10px 18px;
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 9999px;
  box-shadow: var(--app-shadow-sm);
  flex-wrap: wrap;
}

.home__streak-text {
  font-size: 13px;
  color: var(--app-text-secondary);
  white-space: nowrap;
}

.home__streak-text b {
  color: var(--app-primary);
}

.home__streak-bar {
  flex: 1;
  min-width: 140px;
}

/* ===== 顶部导航 ===== */
.home__nav {
  position: relative;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--app-space-lg) 0;
  max-width: var(--app-content-max);
  width: 100%;
  margin: 0 auto;
}

.home__brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.home__brand-logo {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #4a8bff, #2f6bff);
  color: #fff;
  font-weight: 800;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 14px rgba(47, 107, 255, 0.32);
}

.home__brand-name {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
}

.home__brand-sub {
  font-size: 11px;
  color: var(--app-text-secondary);
  letter-spacing: 1px;
}

.home__nav-right {
  display: flex;
  align-items: center;
  gap: var(--app-space-md);
}

.home__user-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--app-text);
}

.home__user-avatar {
  background: linear-gradient(135deg, #409eff, #5db2ff);
  font-weight: 700;
}

.home__user-name {
  font-size: 14px;
  color: var(--app-text);
}

/* 顶栏积分徽章 + VIP 标签 */
.home__points-badge {
  font-size: 12px;
  font-weight: 700;
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.12);
  border: 1px solid rgba(230, 162, 60, 0.35);
  border-radius: 9999px;
  padding: 1px 8px;
  line-height: 1.5;
  white-space: nowrap;
  cursor: pointer;
}

.home__points-badge:hover {
  background: rgba(230, 162, 60, 0.2);
}

.home__vip-tag {
  margin-left: 2px;
}

/* 状态条快捷签到 */
.home__signin-btn {
  padding: 0;
  font-weight: 600;
}

.home__user-caret {
  font-size: 12px;
  color: var(--app-text-secondary);
}

/* 多引擎搜索框 */
.home__search {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: nowrap;
  max-width: 640px;
  margin: 28px auto 0;
  padding: 6px 6px 6px 10px;
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 9999px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
}

.home__search-engine {
  width: 110px;
  flex-shrink: 0;
}

.home__search :deep(.el-select__wrapper) {
  box-shadow: none !important;
  border-radius: 9999px;
  background: #f5f7fa;
  min-height: 38px;
}

.home__search :deep(.el-select__wrapper.is-focused) {
  box-shadow: none !important;
}

.home__search-input {
  flex: 1;
  min-width: 0;
}

.home__search-input :deep(.el-input__wrapper) {
  box-shadow: none !important;
  border-radius: 9999px;
  background: transparent;
}

.home__search-btn {
  flex-shrink: 0;
  border-radius: 9999px;
  font-weight: 600;
  padding-left: 22px;
  padding-right: 22px;
}

.home__link {
  font-size: 14px;
  color: var(--app-text-secondary);
  text-decoration: none;
  transition: color 0.2s;
  cursor: pointer;
  background: none;
  border: none;
  padding: 0;
  font-family: inherit;
}

.home__link:hover {
  color: var(--app-accent-blue);
}

.home__link--primary {
  color: #fff;
  background: var(--app-accent-blue);
  padding: 7px 18px;
  border-radius: 9999px;
  box-shadow: 0 4px 14px rgba(64, 158, 255, 0.35);
}

.home__link--primary:hover {
  color: #fff;
  opacity: 0.9;
}

.home__link--btn:hover {
  color: var(--app-accent-red);
}

/* ===== 主视觉 ===== */
.home__hero {
  position: relative;
  z-index: 2;
  text-align: center;
  padding: 48px 0 32px;
  animation: app-fade-up 0.7s ease both;
}

.home__title {
  margin: 0 0 var(--app-space-md);
  font-size: clamp(30px, 5vw, 46px);
  font-weight: 800;
  letter-spacing: 1px;
  background: linear-gradient(120deg, #2f6bff, #4a8bff 50%, #6fa3ff);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.home__slogan {
  margin: 0 auto var(--app-space-sm);
  font-size: clamp(15px, 2.6vw, 19px);
  color: var(--app-text);
  min-height: 30px;
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 2px;
}

.home__slogan-prefix,
.home__slogan-suffix {
  color: var(--app-accent-blue);
  font-weight: 700;
}

.home__slogan-cursor {
  display: inline-block;
  width: 2px;
  background: var(--app-accent-blue);
  color: transparent;
  animation: app-blink 0.8s step-end infinite;
}

@keyframes app-blink {
  50% { opacity: 0; }
}

.home__subtitle {
  margin: 0;
  font-size: 13px;
  color: var(--app-text-secondary);
  letter-spacing: 3px;
}

/* 快捷入口 */
.home__quick {
  display: flex;
  justify-content: center;
  gap: var(--app-space-lg);
  margin-top: var(--app-space-lg);
}

.home__quick-link {
  font-size: 13px;
  color: var(--app-text-secondary);
  text-decoration: none;
  padding: 6px 18px;
  border: 1px solid var(--app-border);
  border-radius: 9999px;
  background: var(--app-card);
  transition: all 0.2s ease;
}

.home__quick-link:hover {
  color: var(--app-accent-blue);
  border-color: rgba(64, 158, 255, 0.4);
  transform: translateY(-1px);
}

/* 状态条：时钟 + 倒计时 */
.home__statusbar {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px 20px;
  margin-top: var(--app-space-md);
}

.home__statusbar-item {
  font-size: 13px;
  color: var(--app-text-secondary);
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 9999px;
  padding: 4px 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

/* ===== 应用卡片（3D 翻转） ===== */
/* 数据面板区：左仪表盘 + 右天气 */
.home__panels {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: var(--app-space-xl);
  max-width: var(--app-content-max);
  width: 100%;
  margin: 0 auto var(--app-space-2xl);
}

.home__panel {
  animation: app-fade-up 0.6s ease both;
}

.home__panel--dash {
  animation-delay: 0.05s;
}

.home__panel--weather {
  animation-delay: 0.15s;
}

@media (max-width: 900px) {
  .home__panels {
    grid-template-columns: 1fr;
  }
}

/* 本周学习概览 */
.home__weekly {
  position: relative;
  z-index: 2;
  max-width: var(--app-content-max);
  width: 100%;
  margin: 0 auto var(--app-space-2xl);
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  box-shadow: var(--app-shadow-md);
  padding: 18px 20px;
}

.home__weekly-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.home__weekly-title {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
}

.home__weekly-more {
  font-size: 12px;
  color: var(--app-primary);
  text-decoration: none;
}

.home__weekly-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: var(--app-space-md);
}

.home__weekly-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: var(--app-bg-deep);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  transition: all 0.18s ease;
}

.home__weekly-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--app-shadow-sm);
}

.home__weekly-icon {
  font-size: 20px;
}

.home__weekly-body {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.home__weekly-num {
  font-size: 18px;
  font-weight: 800;
  color: var(--app-primary);
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

.home__apps {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--app-space-xl);
  max-width: var(--app-content-max);
  width: 100%;
  margin: 0 auto;
  padding: 16px 0 80px;
}

.app-card {
  perspective: 1200px;
  cursor: pointer;
  animation: app-fade-up 0.7s ease both;
  transition: transform 0.25s ease;
}

.app-card:nth-child(2) {
  animation-delay: 0.12s;
}

.app-card__inner {
  position: relative;
  width: 100%;
  height: 300px;
  transform-style: preserve-3d;
  transition: transform 0.35s ease;
  border-radius: var(--app-radius-lg);
}

/* 悬停翻转展示背面功能列表（与「悬停看看能帮你做什么」文案一致） */
.app-card:hover .app-card__inner {
  transform: rotateY(180deg);
}

.app-card:hover .app-card__face--front {
  box-shadow: var(--app-shadow-lg);
  border-color: rgba(47, 107, 255, 0.22);
}

.app-card:active .app-card__inner {
  transform: rotateY(180deg) scale(0.97);
}

.app-card__face {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  border-radius: var(--app-radius-lg);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: var(--app-space-xl);
  text-align: center;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
}

/* 正面：浅色信息卡 */
.app-card__face--front {
  background: linear-gradient(165deg, #ffffff, #f7faff);
  border: 1px solid var(--app-border);
  box-shadow: var(--app-shadow-sm);
  transition: box-shadow 0.25s ease, border-color 0.25s ease;
}

.theme-dark .app-card__face--front {
  background: linear-gradient(165deg, #18202f, #151b28);
}

.app-card__icon {
  font-size: 52px;
  filter: drop-shadow(0 6px 12px rgba(64, 158, 255, 0.25));
}

.app-card__name {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--app-text);
}

.app-card__desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--app-text-secondary);
}

.app-card__hint {
  font-size: 12px;
  color: var(--app-accent-blue);
  opacity: 0.85;
}

/* 背面：深色卡（黑底白字 + 强调色） */
.app-card__face--back {
  background: rgb(21, 21, 21);
  color: #fff;
  transform: rotateY(180deg);
  align-items: flex-start;
  text-align: left;
  border: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.35);
}

.app-card__back-title {
  margin: 0 0 6px;
  font-size: 19px;
  font-weight: 700;
  color: rgb(255, 170, 92);
}

.app-card__features {
  margin: 0 0 auto;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13.5px;
  line-height: 1.5;
  color: rgba(255, 255, 255, 0.92);
}

.app-card__features li::before {
  content: '▸ ';
  color: rgb(255, 110, 92);
  font-weight: 700;
}

.app-card__go {
  align-self: flex-end;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: rgb(255, 170, 92);
  padding: 8px 20px;
  border-radius: 9999px;
  box-shadow: 0 4px 16px rgba(255, 170, 92, 0.4);
}

/* ===== 背景装饰 ===== */
.home__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.home__orb--1 {
  width: 420px;
  height: 420px;
  top: -120px;
  left: -100px;
}

.home__orb--2 {
  width: 360px;
  height: 360px;
  bottom: -80px;
  right: -80px;
  animation-delay: 2s;
}

.home__orb--3 {
  width: 260px;
  height: 260px;
  top: 40%;
  left: 55%;
  animation-delay: 4s;
  opacity: 0.8;
}

.home__dot {
  position: absolute;
  border-radius: 50%;
  background: rgba(64, 158, 255, 0.28);
  animation: app-float 6s ease-in-out infinite;
}

/* 公告弹窗 */
.home__notice-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 10px;
  color: var(--app-text);
}

.home__notice-content {
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
  color: var(--app-text-secondary);
}

/* ===== 页脚 ===== */
.home__footer {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--app-space-sm);
  padding: var(--app-space-lg) 0 var(--app-space-xl);
  font-size: 13px;
  color: var(--app-text-secondary);
}

.home__footer-sep {
  opacity: 0.6;
}

.home__footer-link {
  color: var(--app-text-secondary);
  text-decoration: none;
}

.home__footer-link:hover {
  color: var(--app-accent-blue);
  text-decoration: underline;
}

/* ===== 像素风点缀 ===== */
/* 品牌 logo：主色渐变圆角方块（商用风） */
.home__brand-logo {
  border-radius: 10px;
  box-shadow: 0 4px 14px rgba(47, 107, 255, 0.28);
  letter-spacing: 0;
}

/* 背景柔和光斑定位（商用风） */
.home-orb--1 {
  width: 380px;
  height: 380px;
  top: -120px;
  right: -100px;
}

.home-orb--2 {
  width: 320px;
  height: 320px;
  bottom: -80px;
  left: -100px;
  animation-delay: 2s;
}

/* 像素图标不再需要柔光阴影 */
.app-card__icon {
  filter: none;
  color: var(--app-accent-blue);
}

.app-card__icon :deep(.pixel-icon) {
  color: var(--app-accent-blue);
}

/* 卡片背面跳转按钮：像素化 */
.app-card__go {
  border-radius: 3px;
  box-shadow: 0 4px 0 rgb(214, 124, 44);
  transition: transform 0.1s ease, box-shadow 0.1s ease;
}

.app-card:hover .app-card__go {
  transform: translateY(-1px);
  box-shadow: 0 6px 0 rgb(214, 124, 44);
}

.app-card:active .app-card__go {
  transform: translateY(4px);
  box-shadow: 0 0 0 rgb(214, 124, 44);
}

/* 修复：卡片悬停像素描边（边框在 face 上，非外层容器） */
.app-card:hover .app-card__face--front {
  border-color: var(--el-color-primary);
}

/* 修复：卡片悬停位移——覆盖入场动画 fill-mode 的优先级 */
.app-card:hover {
  animation: none;
}

/* 修复：快捷入口像素化（原来是胶囊圆角） */
.home__quick-link {
  border-radius: 3px;
  border: 2px solid var(--app-border);
  box-shadow: 0 3px 0 var(--app-border);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: transform 0.08s ease, box-shadow 0.08s ease, color 0.2s ease;
}

.home__quick-link:hover {
  color: var(--app-accent-blue);
  border-color: var(--app-accent-blue);
  box-shadow: 0 5px 0 var(--app-border);
  transform: translateY(-1px);
}

.home__quick-link:active {
  transform: translateY(3px);
  box-shadow: 0 0 0 var(--app-border);
}

@media (max-width: 767px) {
  .home {
    padding: 0 var(--app-space-md);
  }

  /* 顶部导航：隐藏副标题与用户名，避免窄屏挤压 */
  .home__brand-sub {
    display: none;
  }

  .home__brand-name {
    font-size: 16px;
  }

  .home__user-name {
    display: none;
  }

  .home__hero {
    padding: 36px 0 24px;
  }

  /* 快捷入口在小屏允许换行 */
  .home__quick {
    flex-wrap: wrap;
    gap: var(--app-space-sm);
  }

  .home__search {
    flex-wrap: wrap;
    border-radius: var(--app-radius-lg);
  }

  .home__search-engine {
    width: 100%;
  }

  .home__search-btn {
    width: 100%;
  }

  .home__apps {
    grid-template-columns: 1fr;
    gap: var(--app-space-lg);
    padding-bottom: 60px;
  }

  .app-card__inner {
    height: 280px;
  }
}
</style>
