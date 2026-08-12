<template>
  <div class="uc-page">
    <div class="uc-page__bar">
      <router-link to="/" class="uc-page__back">← 返回首页</router-link>
    </div>

    <div class="uc-card">
      <div class="uc-head">
        <el-avatar :size="56" class="uc-head__avatar" :src="info.avatar || undefined">{{ info.avatar ? '' : (info.username || '我')[0] }}</el-avatar>
        <div class="uc-head__text">
          <h1 class="uc-title">{{ info.username || '用户' }}</h1>
          <p class="uc-sub">注册时间：{{ info.createTime || '—' }}</p>
          <el-upload
            class="uc-head__upload"
            :show-file-list="false"
            accept="image/jpeg,image/png,image/webp,image/gif"
            :http-request="handleUploadAvatar"
          >
            <el-button size="small" link type="primary">更换头像</el-button>
          </el-upload>
        </div>
      </div>

      <el-divider />

      <!-- 积分与会员 -->
      <div class="uc-points">
        <div class="uc-points__top">
          <div class="uc-points__left">
            <span class="uc-points__label">积分余额</span>
            <span class="uc-points__value">{{ displayedPoints }}</span>
            <el-tag :type="pointProfile.level === 'VIP' ? 'warning' : 'info'" size="small" effect="dark">
              {{ pointProfile.level === 'VIP' ? 'VIP 会员' : '免费用户' }}
            </el-tag>
            <span v-if="pointProfile.level === 'VIP' && pointProfile.vipExpireAt" class="uc-points__vip-expire">
              到期 {{ formatTime(pointProfile.vipExpireAt) }}
            </span>
          </div>
          <div class="uc-points__actions">
            <el-button type="primary" round :disabled="pointProfile.signedToday" :loading="signing" @click="handleSignIn">
              {{ pointProfile.signedToday ? `已签到 · 连续 ${pointProfile.streakDays} 天` : '每日签到' }}
            </el-button>
            <router-link to="/shop">
              <el-button round plain>积分商城</el-button>
            </router-link>
          </div>
        </div>
        <!-- 签到 7 天周期进度（第 7 天解锁 +10 奖励） -->
        <div v-if="pointProfile.streakDays > 0" class="uc-points__streak">
          <div class="uc-points__streak-head">
            <span>连续 {{ pointProfile.streakDays }} 天</span>
            <span>本周第 {{ streakInCycle }}/7 天 · 再签 {{ remainToBonus }} 天解锁 +10</span>
          </div>
          <el-progress :percentage="streakPct" :stroke-width="8" :show-text="false" color="#2f6bff" />
        </div>
        <div v-if="pointProfile.logs?.length" class="uc-points__logs">
          <div v-for="log in pointProfile.logs" :key="log.id" class="uc-points__log">
            <span class="uc-points__log-reason">{{ log.reason }}</span>
            <span class="uc-points__log-delta" :class="log.changePoints > 0 ? 'is-plus' : 'is-minus'">
              {{ log.changePoints > 0 ? '+' : '' }}{{ log.changePoints }}
            </span>
            <span class="uc-points__log-time">{{ formatTime(log.createTime) }}</span>
          </div>
        </div>
        <p class="uc-points__tip">每日签到 +5 分，连续 7 天额外 +10；点赞 AI 回复 +2 分；积分可在「积分商城」兑换资料与 VIP 体验卡</p>
      </div>

      <!-- 新手引导任务（留存闭环：完成关键动作得积分） -->
      <div v-if="guideTasks.length" class="uc-tasks">
        <div class="uc-tasks__head">
          <h3 class="uc-tasks__title">🎯 新手任务</h3>
          <span class="uc-tasks__total">全部完成可得 {{ guideTotal }} 积分</span>
        </div>
        <div
          v-for="t in guideTasks"
          :key="t.key"
          class="uc-tasks__item"
          :class="{ 'is-done': t.done, 'is-claimed': t.claimed }"
        >
          <span class="uc-tasks__icon">{{ taskIcon(t.key) }}</span>
          <div class="uc-tasks__info">
            <span class="uc-tasks__name">{{ t.name }} <em>+{{ t.rewardPoints }}</em></span>
            <span class="uc-tasks__desc">{{ t.desc }}</span>
          </div>
          <el-button
            v-if="t.canClaim"
            type="primary"
            size="small"
            round
            :loading="claimingKey === t.key"
            @click="handleClaimTask(t)"
          >领取</el-button>
          <el-tag v-else-if="t.claimed" size="small" type="success" effect="plain">已领取</el-tag>
          <el-tag v-else size="small" type="info" effect="plain">{{ t.done ? '可领取' : '未完成' }}</el-tag>
        </div>
      </div>

      <!-- 学习周报入口（数据资产沉淀） -->
      <router-link to="/weekly-report" class="uc-report">
        <span class="uc-report__icon">📊</span>
        <div class="uc-report__text">
          <span class="uc-report__title">学习周报</span>
          <span class="uc-report__desc">看看这周你聊了什么、学了多少、赚了多少积分</span>
        </div>
        <span class="uc-report__arrow">→</span>
      </router-link>

      <!-- 面试记录入口（历史回看） -->
      <router-link to="/interview-records" class="uc-report">
        <span class="uc-report__icon">🎯</span>
        <div class="uc-report__text">
          <span class="uc-report__title">我的面试</span>
          <span class="uc-report__desc">回看每场面试得分与逐题点评，见证进步</span>
        </div>
        <span class="uc-report__arrow">→</span>
      </router-link>

      <!-- 成就徽章（留存游戏化） -->
      <div v-if="achievements.length" class="uc-achv">
        <h3 class="uc-achv__title">🏅 成就徽章</h3>
        <div class="uc-achv__grid">
          <div
            v-for="a in achievements"
            :key="a.code"
            class="uc-achv__item"
            :class="{ 'is-locked': !a.unlocked }"
            :title="a.desc"
          >
            <span class="uc-achv__icon">{{ a.icon }}</span>
            <span class="uc-achv__name">{{ a.name }}</span>
            <span class="uc-achv__prog">{{ a.unlocked ? '✓' : `${a.progress}/${a.target}` }}</span>
          </div>
        </div>
      </div>

      <!-- 邀请好友（分享裂变） -->
      <div class="uc-invite">
        <h3 class="uc-invite__title">🎁 邀请好友赚积分</h3>
        <p class="uc-invite__desc">
          每邀请 1 位好友注册并完成首次对话，你可得 <b>+{{ inviteInfo.rewardPoints }} 积分</b>
          <span v-if="inviteInfo.invitedCount > 0" class="uc-invite__count">（已成功 {{ inviteInfo.invitedCount }} 人）</span>
        </p>
        <div class="uc-invite__body">
          <img v-if="qrDataUrl" :src="qrDataUrl" alt="邀请二维码" class="uc-invite__qr" />
          <div class="uc-invite__right">
            <el-input :model-value="inviteLink" readonly size="small" class="uc-invite__link" />
            <div class="uc-invite__btns">
              <el-button type="primary" size="small" @click="copyInviteLink">复制链接</el-button>
              <el-button size="small" class="uc-invite__poster-btn" @click="openPoster">📸 保存海报</el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 邀请海报弹窗 -->
      <el-dialog
        v-model="posterVisible"
        title="邀请海报"
        width="min(92vw, 380px)"
        append-to-body
        class="uc-poster-dialog"
      >
        <div class="uc-poster">
          <img v-if="posterDataUrl" :src="posterDataUrl" alt="邀请海报" class="uc-poster__img" />
          <div class="uc-poster__actions">
            <el-button type="primary" @click="downloadPoster">💾 下载海报</el-button>
            <el-button @click="copyInviteLink">🔗 复制链接</el-button>
          </div>
          <p class="uc-poster__tip">长按图片可保存到相册，或下载后分享给好友</p>
        </div>
      </el-dialog>

      <el-divider />

      <h2 class="uc-section-title">修改密码</h2>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="uc-form"
      >
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input
            v-model="form.oldPassword"
            type="password"
            size="large"
            show-password
            placeholder="请输入旧密码"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            size="large"
            show-password
            placeholder="请输入新密码（至少6位）"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            size="large"
            show-password
            placeholder="请再次输入新密码"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="saving"
            class="uc-form__submit"
            @click="handleChangePassword"
          >
            确认修改
          </el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <el-button class="uc-form__logout" @click="handleLogout">退出登录</el-button>
    </div>

    <!-- 背景装饰 -->
    <div class="uc-page__bg" aria-hidden="true">
      <span class="app-orb app-orb--blue uc-orb uc-orb--1" />
      <span class="app-orb app-orb--purple uc-orb uc-orb--2" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import { changePassword, claimGuideTask, getAchievements, getGuideTasks, getInvite, getPoints, getUserMe, signIn, uploadAvatar, type Achievement, type GuideTask, type PointProfile, type UserInfo } from '../api/user';
import QRCode from 'qrcode';
import { useCountUp } from '../composables/useCountUp';
import { useAuthStore } from '../store/authStore';

const router = useRouter();
const authStore = useAuthStore();

const info = reactive<UserInfo>({ id: 0, username: '', createTime: '', avatar: '' });
const formRef = ref<FormInstance>();
const saving = ref(false);
const signing = ref(false);
const pointProfile = reactive<PointProfile>({ points: 0, level: 'FREE', signedToday: false, streakDays: 0, logs: [] });

/* 积分数字滚动 + 签到 7 天周期进度 */
const displayedPoints = useCountUp(computed(() => pointProfile.points));
const streakInCycle = computed(() => {
  const d = pointProfile.streakDays % 7;
  return d === 0 ? 7 : d;
});
const remainToBonus = computed(() => 7 - streakInCycle.value);
const streakPct = computed(() => Math.round((streakInCycle.value / 7) * 100));

/* 新手引导任务 */
const guideTasks = ref<GuideTask[]>([]);
const claimingKey = ref('');
const guideTotal = computed(() => guideTasks.value.reduce((s, t) => s + t.rewardPoints, 0));

function taskIcon(key: string) {
  const map: Record<string, string> = {
    first_chat: '💬',
    first_sign: '📅',
    first_interview: '🎯',
    first_redeem: '🛍️',
  };
  return map[key] ?? '✅';
}

async function loadGuideTasks() {
  try {
    const res = await getGuideTasks();
    guideTasks.value = res.data ?? [];
  } catch {
    // 拦截器已提示
  }
}

async function handleClaimTask(t: GuideTask) {
  claimingKey.value = t.key;
  try {
    const res = await claimGuideTask(t.key);
    ElMessage.success(`奖励 +${res.data ?? t.rewardPoints} 积分已到账`);
    await Promise.all([loadGuideTasks(), loadPoints()]);
  } catch {
    // 拦截器已提示
  } finally {
    claimingKey.value = '';
  }
}

/* 成就徽章 */
const achievements = ref<Achievement[]>([]);

async function loadAchievements() {
  try {
    const res = await getAchievements();
    achievements.value = res.data ?? [];
  } catch {
    // 拦截器已提示
  }
}

/* 分享裂变：邀请好友 */
const inviteInfo = reactive({ inviteCode: 0, invitedCount: 0, rewardPoints: 0 });
const inviteLink = ref('');
const qrDataUrl = ref('');

/** 加载邀请信息 + 生成二维码 */
async function loadInvite() {
  try {
    const res = await getInvite();
    inviteInfo.inviteCode = res.data?.inviteCode ?? 0;
    inviteInfo.invitedCount = res.data?.invitedCount ?? 0;
    inviteInfo.rewardPoints = res.data?.rewardPoints ?? 0;
    if (inviteInfo.inviteCode) {
      inviteLink.value = `${window.location.origin}/register?invite=${inviteInfo.inviteCode}`;
      QRCode.toDataURL(inviteLink.value, { width: 120, margin: 1 }).then((url) => {
        qrDataUrl.value = url;
      });
    }
  } catch {
    // 拦截器已提示
  }
}

/** 复制邀请链接 */
async function copyInviteLink() {
  try {
    await navigator.clipboard.writeText(inviteLink.value);
    ElMessage.success('邀请链接已复制，快去分享给好友吧');
  } catch {
    ElMessage.error('复制失败，请手动复制');
  }
}

/* 邀请海报：canvas 绘制品牌海报（背景渐变 + 文案 + 二维码 + 链接），支持下载/长按保存 */
const posterVisible = ref(false);
const posterDataUrl = ref('');

/** 加载图片（二维码 dataURL → HTMLImageElement） */
function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = reject;
    img.src = src;
  });
}

/** 圆角矩形填充 */
function roundRectPath(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + w, y, x + w, y + h, r);
  ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r);
  ctx.arcTo(x, y, x + w, y, r);
  ctx.closePath();
}

/** 绘制并返回海报 dataURL（600x850，适配手机长按保存） */
async function drawPoster(): Promise<string> {
  const W = 600;
  const H = 850;
  const canvas = document.createElement('canvas');
  canvas.width = W;
  canvas.height = H;
  const ctx = canvas.getContext('2d');
  if (!ctx) return qrDataUrl.value;

  // 背景：品牌蓝渐变
  const g = ctx.createLinearGradient(0, 0, 0, H);
  g.addColorStop(0, '#2f6bff');
  g.addColorStop(1, '#17c3f8');
  ctx.fillStyle = g;
  ctx.fillRect(0, 0, W, H);

  // 装饰圆环（层次感）
  ctx.strokeStyle = 'rgba(255,255,255,0.14)';
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.arc(W / 2, 250, 210, 0, Math.PI * 2);
  ctx.stroke();
  ctx.strokeStyle = 'rgba(255,255,255,0.08)';
  ctx.beginPath();
  ctx.arc(W / 2, 250, 260, 0, Math.PI * 2);
  ctx.stroke();

  // 标题区
  ctx.fillStyle = '#ffffff';
  ctx.textAlign = 'center';
  ctx.font = 'bold 52px "PingFang SC", "Microsoft YaHei", sans-serif';
  ctx.fillText('职规大师', W / 2, 150);
  ctx.font = '24px "PingFang SC", "Microsoft YaHei", sans-serif';
  ctx.fillStyle = 'rgba(255,255,255,0.85)';
  ctx.fillText('AI 职业规划 · 面试模拟 · 八股练习', W / 2, 195);

  // 主文案
  ctx.fillStyle = '#ffffff';
  ctx.font = 'bold 38px "PingFang SC", "Microsoft YaHei", sans-serif';
  ctx.fillText('🎁 邀请好友 双方得积分', W / 2, 320);
  ctx.font = '24px "PingFang SC", "Microsoft YaHei", sans-serif';
  ctx.fillText(`好友注册并完成首次对话，你得 +${inviteInfo.rewardPoints || 50} 积分`, W / 2, 362);

  // 二维码（白色圆角卡片承载）
  const qr = await loadImage(qrDataUrl.value);
  const qrSize = 300;
  const qrX = (W - qrSize) / 2;
  const qrY = 420;
  ctx.fillStyle = '#ffffff';
  roundRectPath(ctx, qrX - 22, qrY - 22, qrSize + 44, qrSize + 44, 26);
  ctx.fill();
  ctx.drawImage(qr, qrX, qrY, qrSize, qrSize);

  // 底部提示 + 链接
  ctx.fillStyle = '#ffffff';
  ctx.font = 'bold 22px "PingFang SC", "Microsoft YaHei", sans-serif';
  ctx.fillText('微信扫一扫 · 立即领取', W / 2, qrY + qrSize + 58);
  ctx.fillStyle = 'rgba(255,255,255,0.78)';
  ctx.font = '17px monospace';
  const linkText = inviteLink.value.replace(/^https?:\/\//, '');
  ctx.fillText(linkText.length > 44 ? linkText.slice(0, 44) + '…' : linkText, W / 2, qrY + qrSize + 92);

  return canvas.toDataURL('image/png');
}

/** 打开海报弹窗 */
async function openPoster() {
  if (!qrDataUrl.value) {
    ElMessage.warning('请稍候，二维码生成中…');
    return;
  }
  posterDataUrl.value = await drawPoster();
  posterVisible.value = true;
}

/** 下载海报 */
function downloadPoster() {
  const a = document.createElement('a');
  a.href = posterDataUrl.value;
  a.download = `职规大师邀请海报_${authStore.username || '好友'}.png`;
  a.click();
}

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const validateConfirm = (_rule: unknown, value: string, callback: (e?: Error) => void) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的新密码不一致'));
  } else {
    callback();
  }
};

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
};

function formatTime(t?: string) {
  if (!t) return '';
  return String(t).slice(0, 19).replace('T', ' ');
}

/** 加载积分画像 */
async function loadPoints() {
  try {
    const res = await getPoints();
    pointProfile.points = res.data?.points ?? 0;
    pointProfile.level = res.data?.level ?? 'FREE';
    pointProfile.vipExpireAt = res.data?.vipExpireAt;
    pointProfile.signedToday = !!res.data?.signedToday;
    pointProfile.streakDays = res.data?.streakDays ?? 0;
    pointProfile.logs = res.data?.logs ?? [];
  } catch {
    // 拦截器已提示
  }
}

/** 每日签到（幂等） */
async function handleSignIn() {
  signing.value = true;
  try {
    const res = await signIn();
    ElMessage.success(`签到成功 +${res.data?.points ?? 0} 分${res.data?.bonus ? '（连续奖励）' : ''}`);
    await loadPoints();
  } catch {
    // 拦截器已提示
  } finally {
    signing.value = false;
  }
}

onMounted(async () => {
  try {
    const res = await getUserMe();
    info.id = res.data?.id ?? 0;
    info.username = res.data?.username ?? '';
    info.createTime = res.data?.createTime ? String(res.data.createTime).slice(0, 19).replace('T', ' ') : '';
    info.avatar = res.data?.avatar ?? '';
  } catch {
    // 401 由 http 拦截器统一处理
  }
  loadPoints();
  loadInvite();
  loadAchievements();
  loadGuideTasks();
});

/** 上传/更换头像（el-upload 自定义请求） */
async function handleUploadAvatar(options: { file: File }) {
  try {
    const res = await uploadAvatar(options.file);
    info.avatar = res.data?.avatar ?? '';
    authStore.setAvatar(res.data?.avatar ?? '');
    ElMessage.success('头像已更新');
  } catch {
    // 错误提示由 http 拦截器处理
  }
}

async function handleChangePassword() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    saving.value = true;
    try {
      await changePassword(form.oldPassword, form.newPassword);
      ElMessage.success('密码修改成功，请重新登录');
      authStore.logout();
      setTimeout(() => router.replace({ name: 'login' }), 600);
    } catch {
      // 错误提示由 http 拦截器处理
    } finally {
      saving.value = false;
    }
  });
}

function handleLogout() {
  authStore.logout();
  router.replace('/');
}
</script>

<style scoped>
.uc-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--app-space-xl);
  position: relative;
  overflow: hidden;
  background: linear-gradient(165deg, #f6f8fb 0%, #eef2f7 50%, #e6ebf2 100%);
  color: var(--app-text);
}

.theme-dark .uc-page {
  background: linear-gradient(165deg, #14171c 0%, #101318 50%, #0d1014 100%);
}

.uc-page__bar {
  position: absolute;
  top: var(--app-space-lg);
  left: var(--app-space-xl);
  z-index: 5;
}

.uc-page__back {
  font-size: 14px;
  color: var(--app-accent-blue);
  text-decoration: none;
}

.uc-page__back:hover {
  text-decoration: underline;
}

.uc-card {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 460px;
  padding: 36px 32px;
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
  animation: app-fade-up 0.5s ease both;
}

.uc-head {
  display: flex;
  align-items: center;
  gap: var(--app-space-lg);
}

.uc-head__avatar {
  background: linear-gradient(135deg, #409eff, #5db2ff);
  font-size: 22px;
  font-weight: 700;
}

.uc-title {
  margin: 0 0 4px;
  font-size: 22px;
  font-weight: 800;
  color: var(--app-text);
}

.uc-sub {
  margin: 0;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.uc-section-title {
  margin: 0 0 var(--app-space-lg);
  font-size: 16px;
  font-weight: 700;
  color: var(--app-text);
}

/* 积分与会员卡片 */
.uc-points {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.08), rgba(255, 180, 64, 0.06));
  border: 1px solid rgba(64, 158, 255, 0.18);
  border-radius: var(--app-radius-md);
  padding: 14px 16px;
  margin-bottom: var(--app-space-lg);
}

.uc-points__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.uc-points__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 新手引导任务卡片 */
.uc-tasks {
  margin-top: 18px;
  padding: 16px;
  background: linear-gradient(135deg, #fff7ec 0%, #fffdf8 100%);
  border: 1px solid #ffe3bd;
  border-radius: var(--app-radius-md);
}

.theme-dark .uc-tasks {
  background: linear-gradient(135deg, #261f12 0%, #1a160d 100%);
  border-color: #4a3a1e;
}

.uc-tasks__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.uc-tasks__title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--app-text);
}

.uc-tasks__total {
  font-size: 12px;
  color: #d48806;
}

.theme-dark .uc-tasks__total {
  color: #e6a23c;
}

.uc-tasks__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  border-radius: 10px;
  transition: background 0.15s ease;
}

.uc-tasks__item + .uc-tasks__item {
  border-top: 1px dashed rgba(47, 107, 255, 0.12);
}

.uc-tasks__item:hover {
  background: rgba(255, 255, 255, 0.6);
}

.theme-dark .uc-tasks__item:hover {
  background: rgba(255, 255, 255, 0.04);
}

.uc-tasks__item.is-done .uc-tasks__name,
.uc-tasks__item.is-claimed .uc-tasks__name {
  color: var(--app-text-secondary);
}

.uc-tasks__icon {
  font-size: 20px;
  flex-shrink: 0;
}

.uc-tasks__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.uc-tasks__name {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text);
}

.uc-tasks__name em {
  font-style: normal;
  color: #2f6bff;
  font-weight: 700;
}

.uc-tasks__desc {
  font-size: 12px;
  color: var(--app-text-secondary);
  line-height: 1.4;
}

.uc-report {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 16px;
  padding: 14px 16px;
  background: linear-gradient(135deg, #eef4ff 0%, #f7f9ff 100%);
  border: 1px solid #dbe6ff;
  border-radius: var(--app-radius-md);
  text-decoration: none;
  color: var(--app-text);
  transition: all 0.18s ease;
}

.theme-dark .uc-report {
  background: linear-gradient(135deg, #1a2233 0%, #121826 100%);
  border-color: #2a3550;
}

.uc-report:hover {
  transform: translateY(-1px);
  box-shadow: var(--app-shadow-md);
}

.uc-report__icon {
  font-size: 20px;
}

.uc-report__text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.uc-report__title {
  font-size: 14px;
  font-weight: 700;
}

.uc-report__desc {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.uc-report__arrow {
  color: var(--app-primary);
  font-weight: 700;
}

.uc-points__left {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
}

.uc-points__label {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.uc-points__value {
  font-size: 26px;
  font-weight: 800;
  color: var(--app-accent-blue);
}

.uc-points__vip-expire {
  font-size: 12px;
  color: #b88230;
}

.uc-points__logs {
  margin-top: 12px;
  max-height: 180px;
  overflow-y: auto;
  border-top: 1px dashed var(--app-border);
  padding-top: 8px;
}

.uc-points__log {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  font-size: 12px;
}

.uc-points__log-reason {
  flex: 1;
  color: var(--app-text);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.uc-points__log-delta {
  font-weight: 700;
}

.uc-points__log-delta.is-plus {
  color: #67c23a;
}

.uc-points__log-delta.is-minus {
  color: var(--app-accent-red);
}

.uc-points__log-time {
  color: var(--app-text-secondary);
  flex-shrink: 0;
}

.uc-points__tip {
  margin: 10px 0 0;
  font-size: 11px;
  color: var(--app-text-secondary);
}

/* 签到 7 天周期进度 */
.uc-points__streak {
  margin-top: 12px;
}

.uc-points__streak-head {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--app-text-secondary);
  margin-bottom: 6px;
  gap: 8px;
  flex-wrap: wrap;
}

/* 成就徽章墙 */
.uc-achv {
  background: linear-gradient(135deg, rgba(47, 107, 255, 0.06), rgba(245, 158, 11, 0.05));
  border: 1px solid rgba(47, 107, 255, 0.14);
  border-radius: var(--app-radius-md);
  padding: 14px 16px;
  margin-bottom: var(--app-space-lg);
}

.uc-achv__title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 700;
  color: var(--app-text);
}

.uc-achv__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(92px, 1fr));
  gap: 10px;
}

.uc-achv__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 6px;
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  box-shadow: var(--app-shadow-sm);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.uc-achv__item:hover {
  transform: translateY(-2px);
  box-shadow: var(--app-shadow-md);
}

.uc-achv__item.is-locked {
  opacity: 0.55;
  filter: grayscale(0.5);
}

.uc-achv__icon {
  font-size: 22px;
}

.uc-achv__name {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text);
  text-align: center;
}

.uc-achv__prog {
  font-size: 11px;
  color: var(--app-text-secondary);
}

.uc-achv__item:not(.is-locked) .uc-achv__prog {
  color: var(--app-accent-green);
  font-weight: 700;
}

/* 邀请好友卡片 */
.uc-invite {
  background: linear-gradient(135deg, rgba(255, 180, 64, 0.08), rgba(64, 158, 255, 0.06));
  border: 1px solid rgba(255, 180, 64, 0.2);
  border-radius: var(--app-radius-md);
  padding: 14px 16px;
  margin-bottom: var(--app-space-lg);
}

.uc-invite__title {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 700;
  color: var(--app-text);
}

.uc-invite__desc {
  margin: 0 0 12px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.uc-invite__desc b {
  color: #e6a23c;
}

.uc-invite__count {
  color: #67c23a;
}

.uc-invite__body {
  display: flex;
  align-items: center;
  gap: 12px;
}

.uc-invite__qr {
  width: 96px;
  height: 96px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: #fff;
  flex-shrink: 0;
}

.uc-invite__right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.uc-invite__link {
  width: 100%;
}

.uc-invite__btns {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.uc-invite__poster-btn {
  flex-shrink: 0;
}

/* 邀请海报弹窗 */
.uc-poster {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.uc-poster__img {
  width: 100%;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(47, 107, 255, 0.25);
  user-select: none;
  -webkit-user-drag: none;
}

.uc-poster__actions {
  display: flex;
  gap: 10px;
  width: 100%;
}

.uc-poster__actions .el-button {
  flex: 1;
  border-radius: 9999px;
}

.uc-poster__tip {
  margin: 0;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.uc-form :deep(.el-input__wrapper) {
  border-radius: 10px;
}

.uc-form__submit {
  width: 100%;
  border-radius: 9999px;
  font-weight: 600;
  background: #409eff;
  border: none;
  box-shadow: 0 4px 18px rgba(64, 158, 255, 0.4);
}

.uc-form__submit:hover {
  box-shadow: 0 6px 22px rgba(64, 158, 255, 0.5);
}

.uc-form__logout {
  width: 100%;
  border-radius: 9999px;
  color: var(--app-accent-red);
  border-color: rgba(255, 92, 92, 0.4);
}

.uc-form__logout:hover {
  color: var(--app-accent-red);
  border-color: var(--app-accent-red);
  background: rgba(255, 92, 92, 0.06);
}

.uc-page__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.uc-orb--1 {
  width: 360px;
  height: 360px;
  top: -100px;
  right: -80px;
}

.uc-orb--2 {
  width: 300px;
  height: 300px;
  bottom: -60px;
  left: -80px;
  animation-delay: 2s;
}

@media (max-width: 767px) {
  .uc-page {
    padding: var(--app-space-md);
  }

  .uc-card {
    padding: 28px 22px;
  }
}
</style>
