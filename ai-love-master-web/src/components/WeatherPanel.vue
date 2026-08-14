<template>
  <div class="wp" :class="`wp--${weather.effect || 'cloudy'}`">
    <!-- 动态天气画布（雨/雪/雾/晴光等） -->
    <canvas ref="fxCanvas" class="wp__fx" :class="{ 'is-snow': weather.effect === 'snow' }"></canvas>

    <!-- 加载骨架（数据未返回时） -->
    <el-skeleton v-if="!weather.city" animated class="wp__skeleton">
      <template #template>
        <div class="wp__sk-top">
          <el-skeleton-item variant="text" style="width: 45%; height: 18px" />
        </div>
        <div class="wp__sk-main">
          <el-skeleton-item variant="h3" style="width: 45%; height: 44px" />
          <el-skeleton-item variant="text" style="width: 30%; height: 14px" />
        </div>
        <div class="wp__sk-chips">
          <el-skeleton-item v-for="n in 5" :key="n" variant="button" style="width: 42px; height: 20px" />
        </div>
      </template>
    </el-skeleton>

    <template v-else>
      <div class="wp__top">
        <span class="wp__city">{{ weather.city }}</span>
        <div class="wp__actions">
          <el-tooltip content="定位当前城市" placement="top">
            <button class="wp__btn" @click="locate">📍</button>
          </el-tooltip>
          <el-tooltip content="刷新" placement="top">
            <button class="wp__btn" :class="{ 'is-spinning': loading }" @click="load">🔄</button>
          </el-tooltip>
        </div>
      </div>

      <div class="wp__main">
        <span class="wp__temp app-num">{{ Math.round(weather.temp ?? 0) }}<small>°C</small></span>
        <div class="wp__meta">
          <span class="wp__desc">{{ weather.description || '—' }}</span>
          <span class="wp__wind">💨 {{ weather.windSpeed ?? 0 }} km/h</span>
        </div>
      </div>

      <!-- 常用城市切换 -->
      <div class="wp__cities">
        <button
          v-for="c in cities"
          :key="c"
          class="wp__city-chip"
          :class="{ 'is-active': weather.city === c }"
          @click="pick(c)"
        >
          {{ c }}
        </button>
      </div>
    </template>

    <!-- 顶部装饰云（晴天/多云用，多朵差异化滚动） -->
    <div v-if="['sunny', 'partly', 'cloudy'].includes(weather.effect)" class="wp__deco" aria-hidden="true">
      <span v-for="n in 5" :key="n" class="wp__cloud" :style="cloudStyle(n)"></span>
    </div>

    <!-- 太阳动效（晴朗专属：中心光晕呼吸 + 光芒旋转） -->
    <span v-if="weather.effect === 'sunny'" class="wp__sun" aria-hidden="true">
      <span class="wp__sun-rays"></span>
      <span class="wp__sun-core"></span>
    </span>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { getWeather, type WeatherData, type WeatherEffect } from '../api/weather';

const cities = ['北京', '上海', '广州', '深圳', '成都', '杭州'];

const weather = ref<WeatherData>({ city: '', temp: 0, weatherCode: 0, description: '', effect: 'cloudy', windSpeed: 0, updatedAt: '' });
const loading = ref(false);
const fxCanvas = ref<HTMLCanvasElement | null>(null);

let fxTimer = 0;

onMounted(() => {
  load();
  startFx();
});

onBeforeUnmount(() => cancelAnimationFrame(fxTimer));

watch(() => weather.value.effect, () => startFx());

async function load() {
  loading.value = true;
  try {
    const res = await getWeather();
    if (res.data) weather.value = res.data;
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false;
  }
}

async function pick(city: string) {
  loading.value = true;
  try {
    const res = await getWeather(city);
    if (res.data) weather.value = res.data;
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false;
  }
}

/** 浏览器定位 → 经纬度天气 */
function locate() {
  if (!navigator.geolocation) {
    ElMessage.info('当前浏览器不支持定位，请选择城市');
    return;
  }
  loading.value = true;
  navigator.geolocation.getCurrentPosition(
    async (pos) => {
      try {
        const res = await getWeather(undefined, pos.coords.latitude, pos.coords.longitude);
        if (res.data) weather.value = res.data;
      } catch {
        // 拦截器已提示
      } finally {
        loading.value = false;
      }
    },
    () => {
      loading.value = false;
      ElMessage.info('未获得定位权限，默认展示北京天气');
      pick('北京');
    },
    { timeout: 8000 },
  );
}

/* ===== Canvas 动态天气特效 ===== */
/** 云朵差异化样式（尺寸/高度/速度/延迟各不相同） */
function cloudStyle(n: number) {
  const sizes = [90, 62, 120, 74, 104];
  const tops = [22, 46, 60, 34, 52];
  const durs = [16, 20, 24, 18, 22];
  const size = sizes[n % sizes.length] ?? 90;
  return {
    width: size + 'px',
    height: Math.round(size * 0.33) + 'px',
    top: tops[n % tops.length] + '%',
    left: -(size + 40) + 'px',
    animationDuration: durs[n % durs.length] + 's',
    animationDelay: n * 3.4 + 's',
  };
}

function startFx() {
  cancelAnimationFrame(fxTimer);
  const effect = weather.value.effect as WeatherEffect;
  if (!['rain', 'drizzle', 'snow', 'thunder'].includes(effect)) {
    // 非粒子天气（晴/多云/雾）：清空画布，避免残留上一场雨滴画面
    clearCanvas();
    return;
  }
  // 等待 DOM 布局完成后启动粒子，避免画布尺寸为 0 导致画面静止
  nextTick(() => runParticles(effect));
}

/** 清空天气画布（晴天/多云等非粒子天气调用） */
function clearCanvas() {
  const canvas = fxCanvas.value;
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  if (!ctx) return;
  ctx.clearRect(0, 0, canvas.width, canvas.height);
}

function runParticles(effect: WeatherEffect) {
  const canvas = fxCanvas.value;
  if (!canvas) return;
  const rect = canvas.getBoundingClientRect();
  const W = rect.width;
  const H = rect.height;
  if (W < 2 || H < 2) {
    // 尺寸未就绪：短暂重试
    fxTimer = window.setTimeout(() => runParticles(effect), 150);
    return;
  }
  const dpr = window.devicePixelRatio || 1;
  canvas.width = W * dpr;
  canvas.height = H * dpr;
  const ctx = canvas.getContext('2d');
  if (!ctx) return;
  ctx.scale(dpr, dpr);

  const isSnow = effect === 'snow';
  const isThunder = effect === 'thunder';
  const count = isSnow ? 90 : effect === 'drizzle' ? 60 : 110;
  const drops: { x: number; y: number; v: number; len: number; s: number }[] = [];
  for (let i = 0; i < count; i++) {
    drops.push({
      x: Math.random() * W,
      y: Math.random() * H,
      v: isSnow ? 0.6 + Math.random() * 1.2 : (isThunder ? 10 : 5) + Math.random() * 8,
      len: isSnow ? 3 + Math.random() * 3 : 8 + Math.random() * 10,
      s: isSnow ? 1.5 + Math.random() * 2.5 : 0,
    });
  }
  let flash = 0;

  const draw = () => {
    try {
      ctx.clearRect(0, 0, W, H);
      for (const d of drops) {
        d.y += d.v;
        if (isSnow) {
          d.x += Math.sin(d.y / 30 + d.s) * 0.8;
          if (d.y > H) {
            d.y = -5;
            d.x = Math.random() * W;
          }
          ctx.beginPath();
          ctx.arc(d.x, d.y, d.s, 0, Math.PI * 2);
          ctx.fillStyle = 'rgba(255,255,255,0.85)';
          ctx.fill();
        } else {
          if (d.y > H) {
            d.y = -10;
            d.x = Math.random() * W;
          }
          ctx.beginPath();
          ctx.moveTo(d.x, d.y);
          ctx.lineTo(d.x - 1.5, d.y + d.len);
          ctx.strokeStyle = effect === 'drizzle' ? 'rgba(150,190,255,0.45)' : 'rgba(120,170,255,0.55)';
          ctx.lineWidth = 1.2;
          ctx.stroke();
        }
      }
      // 雷暴闪电闪烁
      if (isThunder) {
        flash -= 1;
        if (flash <= 0 && Math.random() < 0.012) flash = 8;
        if (flash > 0) {
          ctx.fillStyle = `rgba(255,255,220,${(flash / 8) * 0.35})`;
          ctx.fillRect(0, 0, W, H);
        }
      }
      fxTimer = requestAnimationFrame(draw);
    } catch (e) {
      // 单帧异常不中断整个动画循环
      console.warn('weather fx error:', e);
      fxTimer = requestAnimationFrame(draw);
    }
  };
  draw();
}
</script>

<style scoped>
.wp {
  position: relative;
  overflow: hidden;
  border-radius: var(--app-radius-lg);
  padding: 16px 18px 12px;
  min-height: 210px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  color: #fff;
  box-shadow: var(--app-shadow-lg);
  transition: box-shadow 0.3s ease;
}

.wp:hover {
  box-shadow: 0 14px 36px rgba(47, 107, 255, 0.22);
}

/* 天气主题渐变（晴天用明亮天蓝，与站点商业蓝冷色调统一） */
.wp--sunny { background: linear-gradient(135deg, #6fc2f7 0%, #3d9bf5 100%); }
.wp--partly { background: linear-gradient(135deg, #5b8ff9 0%, #4a6cf7 100%); }
.wp--cloudy { background: linear-gradient(135deg, #8fa3c2 0%, #6b7f9e 100%); }
.wp--fog { background: linear-gradient(135deg, #b8c4d6 0%, #8fa0b8 100%); }
.wp--drizzle { background: linear-gradient(135deg, #7f9fd4 0%, #5a7bb8 100%); }
.wp--rain { background: linear-gradient(135deg, #4f6f9e 0%, #334b76 100%); }
.wp--snow { background: linear-gradient(135deg, #a8c4e8 0%, #7d9cc4 100%); }
.wp--thunder { background: linear-gradient(135deg, #3a4a63 0%, #232f45 100%); }

.wp__fx {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

/* 加载骨架 */
.wp__skeleton {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  gap: 14px;
  flex: 1;
}

.wp__skeleton :deep(.el-skeleton__item) {
  background: rgba(255, 255, 255, 0.35);
}

.wp__sk-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.wp__sk-chips {
  display: flex;
  gap: 6px;
  margin-top: auto;
}

.wp__top {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.wp__city {
  font-size: 14px;
  font-weight: 700;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
}

.wp__actions {
  display: flex;
  gap: 6px;
}

.wp__btn {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.wp__btn:hover {
  background: rgba(255, 255, 255, 0.38);
}

.wp__btn.is-spinning {
  animation: wp-spin 0.8s linear infinite;
}

@keyframes wp-spin {
  to { transform: rotate(360deg); }
}

.wp__main {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.wp__temp {
  font-size: 46px;
  font-weight: 800;
  line-height: 1;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
}

.wp__temp small {
  font-size: 18px;
  font-weight: 600;
  margin-left: 2px;
}

.wp__meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.wp__desc {
  font-size: 15px;
  font-weight: 600;
}

.wp__wind {
  font-size: 11px;
  opacity: 0.85;
}

.wp__cities {
  position: relative;
  z-index: 2;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: auto;
}

.wp__city-chip {
  border: none;
  border-radius: 9999px;
  padding: 2px 10px;
  font-size: 11px;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  cursor: pointer;
  transition: all 0.18s ease;
}

.wp__city-chip:hover {
  background: rgba(255, 255, 255, 0.32);
}

.wp__city-chip.is-active {
  background: #fff;
  color: var(--app-primary);
  font-weight: 700;
}

/* 云层装饰（晴天/多云可见，v-for 差异化滚动） */
.wp__deco {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.wp__cloud {
  position: absolute;
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.35);
  animation: wp-cloud linear infinite;
}

@keyframes wp-cloud {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(calc(100vw + 260px));
  }
}

/* 太阳动效（晴朗专属） */
.wp__sun {
  position: absolute;
  top: 10px;
  right: 18px;
  width: 72px;
  height: 72px;
  z-index: 1;
  pointer-events: none;
  animation: sun-breathe 3.2s ease-in-out infinite;
}

.wp__sun-rays {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: repeating-conic-gradient(rgba(255, 222, 130, 0.9) 0deg 7deg, transparent 7deg 30deg);
  animation: sun-spin 22s linear infinite;
}

.wp__sun-core {
  position: absolute;
  inset: 20px;
  border-radius: 50%;
  background: radial-gradient(circle, #fff8dc 0%, #ffd76a 55%, #ffb347 100%);
  box-shadow: 0 0 26px 12px rgba(255, 200, 80, 0.5);
}

@keyframes sun-spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes sun-breathe {
  0%, 100% {
    transform: scale(1);
    filter: brightness(1);
  }
  50% {
    transform: scale(1.08);
    filter: brightness(1.12);
  }
}
</style>
