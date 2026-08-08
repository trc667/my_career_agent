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
            <span class="uc-points__value">{{ pointProfile.points }}</span>
            <el-tag :type="pointProfile.level === 'VIP' ? 'warning' : 'info'" size="small" effect="dark">
              {{ pointProfile.level === 'VIP' ? 'VIP 会员' : '免费用户' }}
            </el-tag>
            <span v-if="pointProfile.level === 'VIP' && pointProfile.vipExpireAt" class="uc-points__vip-expire">
              到期 {{ formatTime(pointProfile.vipExpireAt) }}
            </span>
          </div>
          <el-button type="primary" round :disabled="pointProfile.signedToday" :loading="signing" @click="handleSignIn">
            {{ pointProfile.signedToday ? `已签到 · 连续 ${pointProfile.streakDays} 天` : '每日签到' }}
          </el-button>
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
        <p class="uc-points__tip">每日签到 +5 分，连续 7 天额外 +10；点赞 AI 回复 +2 分</p>
      </div>

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
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import {
  changePassword,
  getPoints,
  getUserMe,
  signIn,
  uploadAvatar,
  type PointProfile,
  type UserInfo,
} from '../api/user';
import { useAuthStore } from '../store/authStore';

const router = useRouter();
const authStore = useAuthStore();

const info = reactive<UserInfo>({ id: 0, username: '', createTime: '', avatar: '' });
const formRef = ref<FormInstance>();
const saving = ref(false);
const signing = ref(false);
const pointProfile = reactive<PointProfile>({ points: 0, level: 'FREE', signedToday: false, streakDays: 0, logs: [] });

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
