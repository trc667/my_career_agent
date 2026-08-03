<template>
  <div class="nav-page">
    <div class="nav-page__bar">
      <router-link to="/" class="nav-page__back">← 返回首页</router-link>
      <h1 class="nav-page__title">网站导航</h1>
    </div>

    <main class="nav-page__body">
      <section v-for="cat in categories" :key="cat.name" class="nav-group">
        <h2 class="nav-group__title">
          <span class="nav-group__dot" :style="{ background: cat.color }" />
          {{ cat.name }}
        </h2>
        <div class="nav-group__grid">
          <a
            v-for="site in cat.sites"
            :key="site.name"
            class="nav-site"
            :href="site.url"
            target="_blank"
            rel="noopener noreferrer"
          >
            <div class="nav-site__icon" :style="{ background: cat.color }">
              {{ site.name.slice(0, 1) }}
            </div>
            <div class="nav-site__text">
              <div class="nav-site__name">{{ site.name }}</div>
              <div class="nav-site__desc">{{ site.desc }}</div>
            </div>
            <span class="nav-site__arrow">↗</span>
          </a>
        </div>
      </section>
    </main>

    <!-- 背景装饰 -->
    <div class="nav-page__bg" aria-hidden="true">
      <span class="app-orb app-orb--blue nav-orb nav-orb--1" />
      <span class="app-orb app-orb--orange nav-orb nav-orb--2" />
    </div>
  </div>
</template>

<script setup lang="ts">
interface NavSite {
  name: string;
  url: string;
  desc: string;
}

interface NavCategory {
  name: string;
  color: string;
  sites: NavSite[];
}

const categories: NavCategory[] = [
  {
    name: '学习资源',
    color: '#409eff',
    sites: [
      { name: 'JavaGuide', url: 'https://javaguide.cn', desc: 'Java 面试学习指南' },
      { name: '小林coding', url: 'https://xiaolincoding.com', desc: '图解计算机基础' },
      { name: '菜鸟教程', url: 'https://www.runoob.com', desc: '入门编程教程' },
      { name: 'MDN Web Docs', url: 'https://developer.mozilla.org', desc: '前端权威文档' },
      { name: 'CSDN', url: 'https://www.csdn.net', desc: '技术博客社区' },
      { name: 'B 站大学', url: 'https://www.bilibili.com', desc: '免费课程视频' },
    ],
  },
  {
    name: '刷题面试',
    color: '#e6a23c',
    sites: [
      { name: 'LeetCode 力扣', url: 'https://leetcode.cn', desc: '算法刷题必备' },
      { name: '牛客网', url: 'https://www.nowcoder.com', desc: '校招真题 / 面经' },
      { name: '洛谷', url: 'https://www.luogu.com.cn', desc: '算法竞赛训练' },
      { name: 'Codeforces', url: 'https://codeforces.com', desc: '国际算法竞赛' },
      { name: '剑指 Offer', url: 'https://leetcode.cn/problem-list/xb9nqhhg/', desc: '经典面试题集' },
    ],
  },
  {
    name: '开发工具',
    color: '#67c23a',
    sites: [
      { name: 'GitHub', url: 'https://github.com', desc: '全球代码托管' },
      { name: 'Gitee', url: 'https://gitee.com', desc: '国内代码托管' },
      { name: 'CodePen', url: 'https://codepen.io', desc: '前端在线 Demo' },
      { name: 'Apifox', url: 'https://apifox.com', desc: 'API 调试协作' },
      { name: 'regex101', url: 'https://regex101.com', desc: '正则在线测试' },
      { name: 'JSON.cn', url: 'https://www.json.cn', desc: 'JSON 格式化' },
    ],
  },
  {
    name: '实用工具',
    color: '#f56c6c',
    sites: [
      { name: 'TinyPNG', url: 'https://tinypng.com', desc: '图片压缩' },
      { name: 'Smallpdf', url: 'https://smallpdf.com/cn', desc: 'PDF 在线处理' },
      { name: 'Convertio', url: 'https://convertio.co/zh/', desc: '文件格式转换' },
      { name: '临时邮箱', url: 'https://www.emailnator.com', desc: '一次性邮箱' },
      { name: 'ProcessOn', url: 'https://www.processon.com', desc: '在线流程图/思维导图' },
    ],
  },
  {
    name: '设计资源',
    color: '#9254de',
    sites: [
      { name: 'iconfont', url: 'https://www.iconfont.cn', desc: '阿里图标库' },
      { name: 'IconPark', url: 'https://iconpark.oceanengine.com', desc: '字节图标库' },
      { name: 'Canva', url: 'https://www.canva.com', desc: '在线设计' },
      { name: 'WallHaven', url: 'https://wallhaven.cc', desc: '高清壁纸' },
      { name: 'Coolors', url: 'https://coolors.co', desc: '配色方案生成' },
    ],
  },
];
</script>

<style scoped>
.nav-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: linear-gradient(165deg, #f6f8fb 0%, #eef2f7 50%, #e6ebf2 100%);
  color: var(--app-text);
  padding: 0 var(--app-space-xl) 60px;
}

.theme-dark .nav-page {
  background: linear-gradient(165deg, #14171c 0%, #101318 50%, #0d1014 100%);
}

.nav-page__bar {
  position: relative;
  z-index: 2;
  max-width: var(--app-content-max);
  width: 100%;
  margin: 0 auto;
  padding: var(--app-space-lg) 0;
  display: flex;
  align-items: center;
  gap: var(--app-space-lg);
}

.nav-page__back {
  font-size: 14px;
  color: var(--app-accent-blue);
  text-decoration: none;
}

.nav-page__back:hover {
  text-decoration: underline;
}

.nav-page__title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 1px;
}

.nav-page__body {
  position: relative;
  z-index: 2;
  max-width: var(--app-content-max);
  width: 100%;
  margin: 0 auto;
}

.nav-group {
  margin-bottom: 36px;
  animation: app-fade-up 0.5s ease both;
}

.nav-group__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 700;
  color: var(--app-text);
}

.nav-group__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.nav-group__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: var(--app-space-md);
}

.nav-site {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-md);
  text-decoration: none;
  color: var(--app-text);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.nav-site:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-color: rgba(64, 158, 255, 0.4);
}

.nav-site__icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  flex-shrink: 0;
}

.nav-site__text {
  flex: 1;
  min-width: 0;
}

.nav-site__name {
  font-size: 14px;
  font-weight: 600;
}

.nav-site__desc {
  font-size: 12px;
  color: var(--app-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-site__arrow {
  font-size: 14px;
  color: var(--app-text-secondary);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.nav-site:hover .nav-site__arrow {
  opacity: 1;
}

.nav-page__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.nav-orb--1 {
  width: 380px;
  height: 380px;
  top: -120px;
  right: -100px;
}

.nav-orb--2 {
  width: 320px;
  height: 320px;
  bottom: -80px;
  left: -100px;
  animation-delay: 2s;
}

@media (max-width: 767px) {
  .nav-page {
    padding: 0 var(--app-space-md) 40px;
  }

  .nav-group__grid {
    grid-template-columns: 1fr;
  }
}
</style>
