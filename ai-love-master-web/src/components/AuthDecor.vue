<template>
  <div class="auth-decor" aria-hidden="true">
    <!-- 顶部渐变 blob（仿 afgprogrammer mYQQJV，配色替换为站点商业蓝） -->
    <div class="auth-blob auth-blob--top">
      <svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" height="1337" width="1337">
        <defs>
          <path
            id="auth-blob-top"
            opacity="1"
            fill-rule="evenodd"
            d="M1337,668.5 C1337,1037.455193874239 1037.455193874239,1337 668.5,1337 C523.6725684305388,1337 337,1236 370.50000000000006,1094 C434.03835568300906,824.6732385973953 6.906089672974592e-14,892.6277623047779 0,668.5000000000001 C0,299.5448061257611 299.5448061257609,1.1368683772161603e-13 668.4999999999999,0 C1037.455193874239,0 1337,299.544806125761 1337,668.5Z"
          />
          <linearGradient id="auth-blob-top-grad" x1="0.79" y1="0.62" x2="0.21" y2="0.86">
            <stop offset="0" stop-color="#2f6bff" stop-opacity="1" />
            <stop offset="1" stop-color="#17c3f8" stop-opacity="1" />
          </linearGradient>
        </defs>
        <g opacity="0.55">
          <use xlink:href="#auth-blob-top" fill="url(#auth-blob-top-grad)" fill-opacity="1" />
        </g>
      </svg>
    </div>
    <!-- 底部渐变 blob -->
    <div class="auth-blob auth-blob--bottom">
      <svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" height="896" width="968">
        <defs>
          <path
            id="auth-blob-bottom"
            opacity="1"
            fill-rule="evenodd"
            d="M896,448 C1142.6325445712241,465.5747656464056 695.2579309733121,896 448,896 C200.74206902668806,896 5.684341886080802e-14,695.2579309733121 0,448.0000000000001 C0,200.74206902668806 200.74206902668791,5.684341886080802e-14 447.99999999999994,0 C695.2579309733121,0 475,418 896,448Z"
          />
          <linearGradient id="auth-blob-bottom-grad" x1="0.5" y1="0" x2="0.5" y2="1">
            <stop offset="0" stop-color="#17c3f8" stop-opacity="1" />
            <stop offset="1" stop-color="#2f6bff" stop-opacity="1" />
          </linearGradient>
        </defs>
        <g opacity="0.5">
          <use xlink:href="#auth-blob-bottom" fill="url(#auth-blob-bottom-grad)" fill-opacity="1" />
        </g>
      </svg>
    </div>
    <!-- 浮动代码符号（贴合计算机学生用户群） -->
    <span v-for="(c, i) in codeSpots" :key="i" class="auth-code" :style="codeStyle(c, i)">
      {{ c.text }}
    </span>
  </div>
</template>

<script setup lang="ts">
interface CodeSpot {
  text: string;
  left: string;
  top: string;
  size: string;
  delay: string;
  rotate: string;
}

const codeSpots: CodeSpot[] = [
  { text: '{ }', left: '7%', top: '16%', size: '24px', delay: '0s', rotate: '-6deg' },
  { text: '</>', left: '90%', top: '20%', size: '20px', delay: '1.4s', rotate: '8deg' },
  { text: 'while(1)', left: '12%', top: '72%', size: '18px', delay: '2.2s', rotate: '-4deg' },
  { text: '#include', left: '84%', top: '70%', size: '18px', delay: '0.8s', rotate: '5deg' },
  { text: '() => {}', left: '20%', top: '38%', size: '17px', delay: '3s', rotate: '10deg' },
  { text: 'git push', left: '78%', top: '42%', size: '17px', delay: '1.8s', rotate: '-8deg' },
  { text: 'if (offer)', left: '5%', top: '52%', size: '17px', delay: '2.6s', rotate: '4deg' },
  { text: 'async()', left: '93%', top: '54%', size: '16px', delay: '0.4s', rotate: '-10deg' },
];

function codeStyle(c: CodeSpot) {
  return {
    left: c.left,
    top: c.top,
    fontSize: c.size,
    animationDelay: c.delay,
    transform: `rotate(${c.rotate})`,
  };
}
</script>

<style scoped>
.auth-decor {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.auth-blob {
  position: absolute;
}

.auth-blob--top {
  top: -900px;
  right: -300px;
}

.auth-blob--bottom {
  bottom: -500px;
  left: -200px;
}

/* 浮动代码符号 */
.auth-code {
  position: absolute;
  font-family: 'JetBrains Mono', 'Consolas', monospace;
  font-weight: 700;
  color: #2f6bff;
  opacity: 0.14;
  user-select: none;
  animation: auth-code-float 9s ease-in-out infinite;
  white-space: nowrap;
}

.theme-dark .auth-code {
  color: #6ea8ff;
  opacity: 0.18;
}

@keyframes auth-code-float {
  0%,
  100% {
    margin-top: 0;
    opacity: 0.14;
  }
  50% {
    margin-top: -18px;
    opacity: 0.26;
  }
}
</style>
