<template>
  <el-dialog
    :model-value="modelValue"
    title="📸 学习成果分享卡"
    width="min(480px, 92vw)"
    align-center
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="share-card__preview">
      <canvas ref="canvasRef" class="share-card__canvas" />
    </div>
    <p class="share-card__hint">长按图片保存，或点下方按钮下载，分享到朋友圈 / 群聊，记录你的成长</p>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
      <el-button type="primary" @click="download">下载图片</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue';

interface ShareItem {
  label: string;
  value: string;
}

const props = defineProps<{
  modelValue: boolean;
  /** 卡片主标题，如「我的学习周报」 */
  title: string;
  /** 副标题，如「2026 第 32 周」 */
  subtitle: string;
  /** 指标行（最多 6 项，2 列网格展示） */
  items: ShareItem[];
  /** 底部标语 */
  slogan?: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void;
}>();

const canvasRef = ref<HTMLCanvasElement | null>(null);

const W = 600;
const H = 850;

watch(
  () => props.modelValue,
  (open) => {
    if (open) nextTick(draw);
  },
);

function roundRect(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  w: number,
  h: number,
  r: number,
) {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + w, y, x + w, y + h, r);
  ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r);
  ctx.arcTo(x, y, x + w, y, r);
  ctx.closePath();
}

/** 绘制分享卡：品牌渐变背景 + 指标卡片网格 + 底部标语 */
function draw() {
  const canvas = canvasRef.value;
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  if (!ctx) return;
  canvas.width = W;
  canvas.height = H;

  // 背景渐变（品牌蓝 → 青）
  const bg = ctx.createLinearGradient(0, 0, 0, H);
  bg.addColorStop(0, '#2f6bff');
  bg.addColorStop(0.55, '#3f7dff');
  bg.addColorStop(1, '#17c3f8');
  ctx.fillStyle = bg;
  ctx.fillRect(0, 0, W, H);

  // 装饰圆环
  ctx.globalAlpha = 0.14;
  ctx.fillStyle = '#ffffff';
  ctx.beginPath();
  ctx.arc(W - 40, 70, 130, 0, Math.PI * 2);
  ctx.fill();
  ctx.beginPath();
  ctx.arc(50, H - 60, 100, 0, Math.PI * 2);
  ctx.fill();
  ctx.globalAlpha = 1;

  // 主标题
  ctx.fillStyle = '#ffffff';
  ctx.textAlign = 'center';
  ctx.font = 'bold 46px "PingFang SC", "Microsoft YaHei", sans-serif';
  ctx.fillText(props.title, W / 2, 140);

  // 副标题
  ctx.font = '24px "PingFang SC", "Microsoft YaHei", sans-serif';
  ctx.globalAlpha = 0.85;
  ctx.fillText(props.subtitle, W / 2, 192);
  ctx.globalAlpha = 1;

  // 指标卡片（2 列，最多 6 项）
  const cols = 2;
  const cardW = 252;
  const cardH = 156;
  const gapX = 22;
  const gapY = 22;
  const startX = (W - (cardW * cols + gapX * (cols - 1))) / 2;
  const startY = 250;

  props.items.slice(0, 6).forEach((item, i) => {
    const col = i % cols;
    const row = Math.floor(i / cols);
    const x = startX + col * (cardW + gapX);
    const y = startY + row * (cardH + gapY);

    roundRect(ctx, x, y, cardW, cardH, 20);
    ctx.fillStyle = 'rgba(255, 255, 255, 0.96)';
    ctx.fill();

    ctx.fillStyle = '#1f2733';
    ctx.font = 'bold 42px "PingFang SC", "Microsoft YaHei", sans-serif';
    ctx.fillText(item.value, x + cardW / 2, y + 78);

    ctx.fillStyle = '#7a8699';
    ctx.font = '21px "PingFang SC", "Microsoft YaHei", sans-serif';
    ctx.fillText(item.label, x + cardW / 2, y + 128);
  });

  // 底部标语 + 品牌
  ctx.fillStyle = '#ffffff';
  ctx.globalAlpha = 0.92;
  ctx.font = '23px "PingFang SC", "Microsoft YaHei", sans-serif';
  ctx.fillText(props.slogan ?? '', W / 2, H - 128);
  ctx.globalAlpha = 1;

  ctx.font = 'bold 30px "PingFang SC", "Microsoft YaHei", sans-serif';
  ctx.fillText('cs-careeragent 智能平台', W / 2, H - 74);
}

function download() {
  const canvas = canvasRef.value;
  if (!canvas) return;
  canvas.toBlob((blob) => {
    if (!blob) return;
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = '学习成果分享卡.png';
    a.click();
    URL.revokeObjectURL(url);
  }, 'image/png');
}
</script>

<style scoped>
.share-card__preview {
  display: flex;
  justify-content: center;
  background: var(--app-bg);
  border-radius: var(--app-radius-md);
  padding: 12px;
}

.share-card__canvas {
  width: 100%;
  max-width: 360px;
  height: auto;
  border-radius: 12px;
  box-shadow: var(--app-shadow-md);
}

.share-card__hint {
  margin: 10px 0 0;
  font-size: 12px;
  line-height: 1.7;
  color: var(--app-text-secondary);
  text-align: center;
}
</style>
