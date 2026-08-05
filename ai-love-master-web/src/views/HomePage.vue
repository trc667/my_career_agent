<template>
  <div class="home">
    <!-- 顶部导航 -->
    <header class="home__nav">
      <div class="home__brand">
        <span class="home__brand-logo">AI</span>
        <span class="home__brand-name">职规助手</span>
        <small class="home__brand-sub">career-master</small>
      </div>
      <div class="home__nav-right">
        <template v-if="authStore.isAuthenticated()">
          <el-dropdown trigger="click" @command="handleUserCommand">
            <span class="home__user-trigger">
              <el-avatar :size="26" class="home__user-avatar" :src="authStore.avatar || undefined">{{ authStore.avatar ? '' : (authStore.username || '我')[0] }}</el-avatar>
              <span class="home__user-name">{{ authStore.username }}</span>
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
        <router-link to="/navigate" class="home__quick-link">🌐 网站导航</router-link>
        <router-link to="/feedback" class="home__quick-link">💬 意见反馈</router-link>
      </div>

      <!-- 状态条：实时时钟 + 倒计时 -->
      <div class="home__statusbar">
        <span class="home__statusbar-item">🕐 {{ clockText }}</span>
        <span class="home__statusbar-item">🏖 距周末 {{ weekendText }}</span>
        <span class="home__statusbar-item">🎉 距{{ holidayName }} {{ holidayText }}</span>
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
        <el-button type="primary" size="large" class="home__search-btn" @click="doSearch">
          搜索
        </el-button>
      </div>
    </main>

    <!-- 应用卡片：3D 翻转 -->
    <section class="home__apps">
      <div
        class="app-card"
        @click="goToApp('/career-master')"
        @mousemove="onCardMove($event, $event.currentTarget as HTMLElement)"
        @mouseleave="onCardLeave($event.currentTarget as HTMLElement)"
      >
        <div class="app-card__inner">
          <div class="app-card__face app-card__face--front">
            <div class="app-card__icon">🎓</div>
            <h3 class="app-card__name">AI 职规大师</h3>
            <p class="app-card__desc">不懂怎么规划职业？<br />简历、面试、校招随时问</p>
            <span class="app-card__hint">悬停看看能帮你做什么 →</span>
          </div>
          <div class="app-card__face app-card__face--back">
            <h3 class="app-card__back-title">AI 职规大师</h3>
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
        class="app-card"
        @click="goToApp('/super-agent')"
        @mousemove="onCardMove($event, $event.currentTarget as HTMLElement)"
        @mouseleave="onCardLeave($event.currentTarget as HTMLElement)"
      >
        <div class="app-card__inner">
          <div class="app-card__face app-card__face--front">
            <div class="app-card__icon">🤖</div>
            <h3 class="app-card__name">AI 超级智能体</h3>
            <p class="app-card__desc">一个任务，AI 帮你跑完<br />查资料、找地点、出 PDF 一条龙</p>
            <span class="app-card__hint">悬停看看能帮你做什么 →</span>
          </div>
          <div class="app-card__face app-card__face--back">
            <h3 class="app-card__back-title">AI 超级智能体</h3>
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
        class="app-card"
        @click="goToApp('/bagu')"
        @mousemove="onCardMove($event, $event.currentTarget as HTMLElement)"
        @mouseleave="onCardLeave($event.currentTarget as HTMLElement)"
      >
        <div class="app-card__inner">
          <div class="app-card__face app-card__face--front">
            <div class="app-card__icon">📚</div>
            <h3 class="app-card__name">AI 八股练习场</h3>
            <p class="app-card__desc">面试八股随便刷<br />按主题速览 + 随机抽题</p>
            <span class="app-card__hint">悬停看看能帮你做什么 →</span>
          </div>
          <div class="app-card__face app-card__face--back">
            <h3 class="app-card__back-title">AI 八股练习场</h3>
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
    </section>

    <!-- 背景装饰：柔和光斑 + 漂浮圆点 -->
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
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { ArrowDown } from '@element-plus/icons-vue';
import { useAuthStore } from '../store/authStore';
import { getLatestNotice, type Notice } from '../api/notice';

const router = useRouter();
const authStore = useAuthStore();

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
  el.style.transform = `perspective(1200px) rotateY(${(px * 8).toFixed(2)}deg) rotateX(${(-py * 8).toFixed(2)}deg)`;
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
  if (authStore.isAuthenticated()) authStore.fetchAvatar();
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
  background: linear-gradient(165deg, #f6f8fb 0%, #eef2f7 45%, #e6ebf2 100%);
  color: var(--app-text);
  padding: 0 var(--app-space-xl);
}

.theme-dark .home {
  background: linear-gradient(165deg, #14171c 0%, #101318 50%, #0d1014 100%);
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
  background: linear-gradient(135deg, #409eff, #5db2ff);
  color: #fff;
  font-weight: 800;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 14px rgba(64, 158, 255, 0.35);
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
  letter-spacing: 2px;
  background: linear-gradient(120deg, #409eff, #5db2ff 50%, #7ec3ff);
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
  transition: transform 0.65s cubic-bezier(0.4, 0.2, 0.2, 1);
  border-radius: var(--app-radius-lg);
}

.app-card:hover .app-card__inner {
  transform: rotateY(180deg);
}

.app-card:active .app-card__inner {
  transform: scale(0.97);
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
  background: var(--app-card);
  border: 1px solid var(--app-border);
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
