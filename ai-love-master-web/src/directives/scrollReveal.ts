/**
 * v-scroll-reveal 指令：元素进入视口时添加 scroll-reveal--visible 类
 * 支持修饰符 .once（默认 true）、.stagger 及 value 为延迟 ms
 */
import type { Directive } from 'vue';

const observerMap = new WeakMap<Element, IntersectionObserver>();

export const vScrollReveal: Directive = {
  mounted(el: HTMLElement, binding) {
    el.classList.add('scroll-reveal');
    const once = binding.modifiers?.once !== false;
    const delay = typeof binding.value === 'number' ? binding.value : 0;
    if (delay > 0) el.style.transitionDelay = `${delay}ms`;

    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            entry.target.classList.add('scroll-reveal--visible');
            if (once) observer.unobserve(entry.target);
          }
        }
      },
      { threshold: 0.1, rootMargin: '0px 0px -30px 0px' }
    );

    observer.observe(el);
    observerMap.set(el, observer);
  },
  unmounted(el: Element) {
    const observer = observerMap.get(el);
    if (observer) {
      observer.disconnect();
      observerMap.delete(el);
    }
  },
};
