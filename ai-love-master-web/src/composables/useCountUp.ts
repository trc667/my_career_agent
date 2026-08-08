import { ref, watch, type Ref } from 'vue';

/**
 * 数字滚动动效（count-up）：数值变化时从旧值缓动滚到新值（ease-out cubic）。
 * 用于积分/统计数字展示，感知更"活"。
 *
 * @param target 目标值（响应式）
 * @param duration 动画时长 ms
 * @returns 显示值（响应式，模板直接绑定）
 */
export function useCountUp(target: Ref<number>, duration = 700) {
  const display = ref(0);
  let raf = 0;

  watch(
    target,
    (val) => {
      cancelAnimationFrame(raf);
      const from = display.value;
      const t0 = performance.now();
      const tick = (t: number) => {
        const p = Math.min(1, (t - t0) / duration);
        // ease-out cubic：先快后慢
        const eased = 1 - Math.pow(1 - p, 3);
        display.value = Math.round(from + (val - from) * eased);
        if (p < 1) {
          raf = requestAnimationFrame(tick);
        } else {
          display.value = val;
        }
      };
      raf = requestAnimationFrame(tick);
    },
    { immediate: true },
  );

  return display;
}
