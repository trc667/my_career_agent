<template>
  <span
    class="pixel-icon"
    :style="{ width: size + 'px', height: size + 'px' }"
    v-html="svg"
    role="img"
    aria-hidden="true"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue';
// 静态引入常用像素图标（pixelarticons，MIT，24x24 网格，currentColor 填充）
import briefcase from 'pixelarticons/svg/briefcase.svg?raw';
import cpu from 'pixelarticons/svg/cpu.svg?raw';
import bookOpen from 'pixelarticons/svg/book-open.svg?raw';
import cardText from 'pixelarticons/svg/card-text.svg?raw';
import globe from 'pixelarticons/svg/globe.svg?raw';
import comment from 'pixelarticons/svg/comment.svg?raw';
import clock from 'pixelarticons/svg/clock.svg?raw';
import search from 'pixelarticons/svg/search.svg?raw';
import send from 'pixelarticons/svg/send.svg?raw';
import star from 'pixelarticons/svg/star.svg?raw';
import user from 'pixelarticons/svg/user.svg?raw';
import chevronRight from 'pixelarticons/svg/chevron-right.svg?raw';
import message from 'pixelarticons/svg/message.svg?raw';
import note from 'pixelarticons/svg/note.svg?raw';
import trash from 'pixelarticons/svg/trash.svg?raw';
import plus from 'pixelarticons/svg/plus.svg?raw';
import chart from 'pixelarticons/svg/chart.svg?raw';
import check from 'pixelarticons/svg/check.svg?raw';
import heart from 'pixelarticons/svg/heart.svg?raw';
import database from 'pixelarticons/svg/database.svg?raw';

const ICONS: Record<string, string> = {
  briefcase,
  cpu,
  'book-open': bookOpen,
  'card-text': cardText,
  globe,
  comment,
  clock,
  search,
  send,
  star,
  user,
  'chevron-right': chevronRight,
  message,
  note,
  trash,
  plus,
  chart,
  check,
  heart,
  database,
};

const props = withDefaults(defineProps<{ name: string; size?: number }>(), { size: 24 });

const svg = computed(() => ICONS[props.name] ?? '');
</script>

<style scoped>
.pixel-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.pixel-icon :deep(svg) {
  width: 100%;
  height: 100%;
  display: block;
  /* 像素图标禁用抗锯齿，保证 24px 整数倍渲染的锐利边缘 */
  shape-rendering: crispEdges;
}
</style>
