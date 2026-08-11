<template>
  <div class="auth-page">
    <div class="auth-page__bar">
      <router-link to="/" class="auth-page__back">← 返回首页</router-link>
    </div>

    <div class="auth-card">
      <img src="/logo.jpg" alt="职规大师 Logo" class="auth-card__logo" />
      <h1 class="auth-title">注册</h1>
      <p class="auth-desc">注册账号后登录使用 AI 功能</p>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="auth-form"
        @submit.prevent="handleRegister"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
            autocomplete="username"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码（至少6位）"
            size="large"
            :prefix-icon="Lock"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="form.email"
            placeholder="请输入邮箱（用于接收验证码）"
            size="large"
            :prefix-icon="Message"
            autocomplete="email"
          />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div class="register-code">
            <el-input
              v-model="form.code"
              placeholder="6位验证码"
              size="large"
              :prefix-icon="Key"
              maxlength="6"
            />
            <el-button
              size="large"
              class="register-code__btn"
              :disabled="countdown > 0 || !isEmailValid"
              @click="handleSendCode"
            >
              {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item prop="agreed" class="register-agreement">
          <el-checkbox v-model="form.agreed">
            我已阅读并同意
            <router-link to="/agreement" target="_blank" class="register-agreement__link">《用户协议与隐私政策》</router-link>
          </el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="auth-submit"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>
        <p class="auth-switch">
          已有账号？
          <router-link to="/login" class="auth-switch__link">立即登录</router-link>
        </p>
      </el-form>
    </div>

    <div class="auth-page__status">
      <span class="auth-page__status-dot" />
      <span>AUTH MODULE · v1.0</span>
    </div>

    <!-- 背景装饰 -->
    <AuthDecor />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onBeforeUnmount } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { User, Lock, Message, Key } from '@element-plus/icons-vue';
import type { FormInstance, FormRules } from 'element-plus';
import { useAuthStore } from '../store/authStore';
import { ElMessage } from 'element-plus';
import { sendEmailCode } from '../api/auth';
import AuthDecor from '../components/AuthDecor.vue';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const formRef = ref<FormInstance>();
const loading = ref(false);
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  code: '',
  agreed: false,
  inviteCode: '',
});

// 分享裂变：通过邀请链接（/register?invite=xxx）进入时自动预填邀请码
if (route.query.invite) {
  form.inviteCode = String(route.query.invite);
}

/** 邮箱格式校验（简单正则） */
const EMAIL_RE = /^[\w.-]+@[\w-]+(\.[\w-]+)+$/;
const isEmailValid = computed(() => EMAIL_RE.test(form.email.trim()));

/** 验证码发送倒计时（秒） */
const countdown = ref(0);
let countdownTimer: ReturnType<typeof setInterval> | null = null;

async function handleSendCode() {
  if (!isEmailValid.value) {
    ElMessage.warning('请先输入正确的邮箱地址');
    return;
  }
  try {
    await sendEmailCode(form.email.trim());
    ElMessage.success('验证码已发送，请查收邮件');
    countdown.value = 60;
    if (countdownTimer) clearInterval(countdownTimer);
    countdownTimer = setInterval(() => {
      countdown.value -= 1;
      if (countdown.value <= 0 && countdownTimer) {
        clearInterval(countdownTimer);
        countdownTimer = null;
      }
    }, 1000);
  } catch {
    // 错误提示由 http 拦截器处理（如：发送过于频繁）
  }
}

onBeforeUnmount(() => {
  if (countdownTimer) clearInterval(countdownTimer);
});

const validateConfirm = (_rule: unknown, value: string, callback: (e?: Error) => void) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
};

const validateAgreed = (_rule: unknown, value: boolean, callback: (e?: Error) => void) => {
  if (!value) {
    callback(new Error('请先阅读并同意用户协议与隐私政策'));
  } else {
    callback();
  }
};

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: EMAIL_RE, message: '邮箱格式不正确', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为6位数字', trigger: 'blur' },
  ],
  agreed: [{ validator: validateAgreed, trigger: 'change' }],
};

async function handleRegister() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      await authStore.register({
        username: form.username,
        password: form.password,
        email: form.email.trim(),
        code: form.code.trim(),
        agreed: form.agreed,
        inviteCode: form.inviteCode.trim() || undefined,
      });
      ElMessage.success('注册成功，已自动登录，正在跳转…');
      const redirect = (route.query.redirect as string) || '/';
      setTimeout(() => router.replace(redirect), 400);
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : '注册失败，请稍后重试';
      ElMessage.error(msg);
    } finally {
      loading.value = false;
    }
  });
}
</script>

<style scoped>
/* ===== 仿 afgprogrammer mYQQJV 登录页：浅紫蓝底 + SVG blob + 白卡片 + 下划线输入框 + 渐变胶囊按钮 ===== */
.auth-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--app-space-xl);
  position: relative;
  overflow: hidden;
  background: linear-gradient(160deg, #e9edfa 0%, #dde3f7 55%, #d3daf3 100%);
  color: var(--app-text);
}

.theme-dark .auth-page {
  background: linear-gradient(160deg, #14171c 0%, #101318 50%, #0d1014 100%);
}

.auth-page__bar {
  position: absolute;
  top: var(--app-space-lg);
  left: var(--app-space-xl);
  z-index: 5;
}

.auth-page__back {
  font-size: 14px;
  color: var(--app-accent-blue);
  text-decoration: none;
}

.auth-page__back:hover {
  text-decoration: underline;
}

.auth-card {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 380px;
  padding: 40px;
  background: #fff;
  border: none;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(47, 107, 255, 0.16);
  text-align: center;
  animation: app-fade-up 0.5s ease both;
}

.theme-dark .auth-card {
  background: #1b2230;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.45);
}

.auth-card__logo {
  width: 76px;
  height: 76px;
  margin: 0 auto 28px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
  box-shadow: 0 10px 24px rgba(47, 107, 255, 0.38);
}

.auth-title {
  margin: 0 0 6px;
  font-size: 30px;
  font-weight: 500;
  color: #2f6bff;
}

.theme-dark .auth-title {
  color: #6ea8ff;
}

.auth-desc {
  margin: 0 0 32px;
  font-size: 15px;
  font-weight: 300;
  color: #2f6bff;
  opacity: 0.68;
}

.theme-dark .auth-desc {
  color: #8fa8d8;
}

.auth-form {
  text-align: left;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 8px;
}

.auth-form :deep(.el-form-item__label) {
  color: #555;
  font-weight: 400;
  font-size: 14px;
}

.theme-dark .auth-form :deep(.el-form-item__label) {
  color: #c6cede;
}

/* 下划线输入框：无边框，仅底部细线，聚焦变主色 */
.auth-form :deep(.el-input__wrapper) {
  background: transparent;
  box-shadow: none;
  border-radius: 0;
  border-bottom: 2px solid rgba(153, 153, 153, 0.35);
  padding: 0 2px;
  transition: border-color 0.25s ease;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  border-bottom-color: #2f6bff;
}

.auth-form :deep(.el-input__inner) {
  height: 50px;
  font-size: 17px;
  color: #333;
}

.auth-form :deep(.el-input__inner::placeholder) {
  color: #999;
  font-size: 17px;
  font-weight: 300;
}

.auth-form :deep(.el-input__prefix) {
  color: #999;
}

.auth-form :deep(.el-input__wrapper.is-focus .el-input__prefix) {
  color: #2f6bff;
}

.theme-dark .auth-form :deep(.el-input__wrapper) {
  border-bottom-color: rgba(255, 255, 255, 0.22);
}

.theme-dark .auth-form :deep(.el-input__inner) {
  color: #e8ecf4;
}

.theme-dark .auth-form :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.35);
}

.theme-dark .auth-form :deep(.el-input__prefix) {
  color: #8fa8d8;
}

/* 验证码行：输入框 + 获取按钮 */
.register-code {
  display: flex;
  gap: 10px;
  width: 100%;
}

.register-code :deep(.el-input) {
  flex: 1;
}

.register-code__btn {
  flex-shrink: 0;
  border-radius: 50px;
  min-width: 110px;
  font-weight: 400;
  background: linear-gradient(to right, #2f6bff, #17c3f8);
  border: none;
  color: #fff;
}

/* 渐变胶囊按钮（居中，非通栏） */
.auth-form :deep(.el-form-item__content) {
  justify-content: center;
  padding-top: 14px;
}

.auth-submit {
  width: 120px;
  height: 42px;
  margin: 0 auto;
  border-radius: 50px;
  font-weight: 400;
  font-size: 16px;
  letter-spacing: 0.06em;
  background: linear-gradient(to right, #2f6bff, #17c3f8);
  border: none;
  box-shadow: 0 8px 20px rgba(47, 107, 255, 0.35);
  transition: transform 0.15s ease, box-shadow 0.15s ease, filter 0.2s ease;
}

.auth-submit:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(47, 107, 255, 0.42);
  filter: brightness(1.05);
}

.auth-submit:active {
  transform: translateY(1px);
  box-shadow: 0 4px 12px rgba(47, 107, 255, 0.28);
}

.auth-switch {
  margin: 26px 0 0;
  font-size: 14px;
  color: #777;
  text-align: center;
}

.theme-dark .auth-switch {
  color: #9aa8bf;
}

.auth-switch__link {
  color: #2f6bff;
  text-decoration: none;
  margin-left: 4px;
}

.theme-dark .auth-switch__link {
  color: #6ea8ff;
}

.auth-switch__link:hover {
  text-decoration: underline;
}

/* 协议勾选 */
.register-agreement {
  margin-bottom: var(--app-space-sm);
}

.register-agreement :deep(.el-checkbox__label) {
  font-size: 13px;
  color: #777;
}

.theme-dark .register-agreement :deep(.el-checkbox__label) {
  color: #9aa8bf;
}

.register-agreement__link {
  color: #2f6bff;
  text-decoration: none;
}

.theme-dark .register-agreement__link {
  color: #6ea8ff;
}

.register-agreement__link:hover {
  text-decoration: underline;
}

.auth-page__status {
  position: relative;
  z-index: 2;
  margin-top: var(--app-space-xl);
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  letter-spacing: 0.15em;
  color: #7b87a8;
  font-family: 'JetBrains Mono', 'Consolas', monospace;
}

.theme-dark .auth-page__status {
  color: #6b7690;
}

.auth-page__status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--app-accent-blue);
  animation: app-pulse 1.5s ease-in-out infinite;
}

@media (max-width: 767px) {
  .auth-page {
    padding: var(--app-space-md);
  }

  .auth-card {
    padding: 28px 22px;
  }
}
</style>