/**
 * 滚动触发动画：元素进入视口时淡入、上移
 * 使用 Intersection Observer，无需额外依赖
 */
import { onMounted, onUnmounted, type Ref } from 'vue';

export interface UseScrollRevealOptions {
  /** 触发可见的阈值 0-1，默认 0.1 */
  threshold?: number;
  /** 提前多少 px 触发，默认 0 */
  rootMargin?: string;
  /** 触发一次后是否保持，默认 true */
  once?: boolean;
}

const defaultOptions: UseScrollRevealOptions = {
  threshold: 0.1,
  rootMargin: '0px',
  once: true,
};

export function useScrollReveal(
  target: Ref<HTMLElement | null | undefined>,
  options: UseScrollRevealOptions = {}
) {
  const opts = { ...defaultOptions, ...options };

  const addRevealedClass = (el: Element) => {
    el.classList.add('scroll-reveal--visible');
  };

  let observer: IntersectionObserver | null = null;

  onMounted(() => {
    const el = target.value;
    if (!el) return;

    observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            addRevealedClass(entry.target);
            if (opts.once) observer?.unobserve(entry.target);
          }
        }
      },
      {
        threshold: opts.threshold,
        rootMargin: opts.rootMargin,
      }
    );

    observer.observe(el);
  });

  onUnmounted(() => {
    observer?.disconnect();
  });
}

/** 批量监听多个元素，支持交错延迟 */
export function useScrollRevealMultiple(
  targets: Ref<HTMLElement[] | null | undefined>,
  options: UseScrollRevealOptions & { staggerMs?: number } = {}
) {
  const { staggerMs = 0, ...opts } = { ...defaultOptions, ...options };

  const addRevealedClass = (el: Element, delay = 0) => {
    if (delay > 0) {
      (el as HTMLElement).style.animationDelay = `${delay}ms`;
    }
    el.classList.add('scroll-reveal--visible');
  };

  let observer: IntersectionObserver | null = null;

  onMounted(() => {
    const els = targets.value;
    if (!els?.length) return;

    observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            const idx = els.indexOf(entry.target as HTMLElement);
            const delay = idx >= 0 ? idx * staggerMs : 0;
            addRevealedClass(entry.target, delay);
            if (opts.once) observer?.unobserve(entry.target);
          }
        }
      },
      {
        threshold: opts.threshold,
        rootMargin: opts.rootMargin,
      }
    );

    els.forEach((el) => observer?.observe(el));
  });

  onUnmounted(() => {
    observer?.disconnect();
  });
}
