/**
 * 视差效果：根据滚动位置对元素施加 translateY 偏移
 * 速度因子 > 0 表示比页面滚动慢（背景感），< 0 表示更快
 */
import { onMounted, onUnmounted, ref } from 'vue';

export function useParallax(speedFactor = 0.3) {
  const offsetY = ref(0);

  const onScroll = () => {
    const scrollY = window.scrollY || document.documentElement.scrollTop;
    offsetY.value = scrollY * speedFactor;
  };

  onMounted(() => {
    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll(); // 初始
  });

  onUnmounted(() => {
    window.removeEventListener('scroll', onScroll);
  });

  return { offsetY };
}
