<template>
  <el-dialog
    :model-value="modelValue"
    title="💎 VIP 会员权益"
    width="min(560px, 92vw)"
    align-center
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="vip-compare">
      <div class="vip-compare__row vip-compare__row--head">
        <span>权益</span>
        <span>免费用户</span>
        <span class="is-vip">VIP 会员</span>
      </div>
      <div v-for="b in benefits" :key="b.name" class="vip-compare__row">
        <span class="vip-compare__name">{{ b.icon }} {{ b.name }}</span>
        <span class="vip-compare__free">{{ b.free }}</span>
        <span class="vip-compare__vip is-vip">{{ b.vip }}</span>
      </div>
    </div>
    <p class="vip-compare__tip">
      🛍 可在「积分商城」用 200 积分兑换 7 天 VIP 体验卡（签到/邀请/新手任务都能攒积分）
    </p>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
      <el-button type="warning" round @click="goShop">去商城兑换 VIP</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void;
}>();

const router = useRouter();

/** 权益对比数据（与后端积分计费/限流规则保持一致） */
const benefits = [
  { icon: '💬', name: 'AI 对话（职规大师 / 超级智能体）', free: '按 token 消耗积分', vip: '全部免费' },
  { icon: '🎯', name: 'AI 面试模拟', free: '每日 2 次', vip: '不限次数' },
  { icon: '🧠', name: '面试点评深度', free: '标准点评', vip: 'qwen-max 深度点评（4 维度）' },
  { icon: '📄', name: '简历评分与优化', free: '评分 1 分 · 优化 2 分', vip: '全部免费' },
  { icon: '⚡', name: '对话频率上限', free: '20 次 / 分钟', vip: '60 次 / 分钟' },
  { icon: '🏅', name: '会员标识', free: '—', vip: '全站 VIP 徽章' },
];

function goShop() {
  emit('update:modelValue', false);
  router.push('/shop');
}
</script>

<style scoped>
.vip-compare {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.vip-compare__row {
  display: grid;
  grid-template-columns: 1.6fr 1fr 1fr;
  gap: 8px;
  align-items: center;
  padding: 10px 12px;
  background: var(--app-bg);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  font-size: 13px;
}

.vip-compare__row--head {
  background: var(--app-primary-soft);
  border-color: transparent;
  font-weight: 700;
  color: var(--app-text);
}

.vip-compare__name {
  font-weight: 600;
  color: var(--app-text);
}

.vip-compare__free {
  color: var(--app-text-secondary);
}

.vip-compare__vip {
  font-weight: 700;
}

.is-vip {
  color: #d48806;
}

.theme-dark .is-vip {
  color: #f0b429;
}

.vip-compare__tip {
  margin: 12px 0 0;
  font-size: 12px;
  line-height: 1.7;
  color: var(--app-text-secondary);
}
</style>
