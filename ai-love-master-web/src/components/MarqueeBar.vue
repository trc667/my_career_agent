<template>
  <div class="mq" :class="{ 'mq--slow': slow }">
    <div class="mq__track">
      <div v-for="g in 2" :key="g" class="mq__group">
        <span
          v-for="(item, i) in items"
          :key="g + '-' + i"
          class="mq__item"
          :class="{ 'mq__item--link': item.to }"
          @click="item.to && go(item.to)"
        >
          <span class="mq__icon">{{ item.icon }}</span>
          <span class="mq__text">{{ item.text }}</span>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const props = withDefaults(
  defineProps<{
    /** 徽章项：icon 表情 + text 文案 + 可选 to 跳转 */
    items: { icon: string; text: string; to?: string }[];
    /** 慢速滚动（小屏/减少干扰） */
    slow?: boolean;
  }>(),
  { items: () => [], slow: false },
);

const router = useRouter();

function go(to: string) {
  router.push(to);
}
</script>

<style scoped>
.mq {
  position: relative;
  width: 100%;
  overflow: hidden;
  mask-image: linear-gradient(90deg, transparent 0, #000 8%, #000 92%, transparent 100%);
  -webkit-mask-image: linear-gradient(90deg, transparent 0, #000 8%, #000 92%, transparent 100%);
}

.mq__track {
  display: flex;
  width: max-content;
  animation: mq-scroll 32s linear infinite;
}

.mq--slow .mq__track {
  animation-duration: 48s;
}

.mq:hover .mq__track {
  animation-play-state: paused;
}

.mq__group {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-right: 10px;
}

.mq__item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 14px;
  border-radius: 9999px;
  background: var(--app-card);
  border: 1px solid var(--app-border);
  color: var(--app-text-secondary);
  font-size: 12px;
  white-space: nowrap;
  box-shadow: var(--app-shadow-sm);
  cursor: default;
  transition: all 0.18s ease;
  user-select: none;
}

.mq__item--link {
  cursor: pointer;
}

.mq__item--link:hover {
  color: var(--app-primary);
  border-color: rgba(47, 107, 255, 0.35);
  transform: translateY(-1px);
}

.mq__icon {
  font-size: 14px;
}

@keyframes mq-scroll {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(-50%);
  }
}
</style>
