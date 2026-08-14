<template>
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
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import PixelIcon from '../PixelIcon.vue';
import { useAuthStore } from '../../store/authStore';

const router = useRouter();
const authStore = useAuthStore();

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
</script>

<style scoped>
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

@media (max-width: 767px) {
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
