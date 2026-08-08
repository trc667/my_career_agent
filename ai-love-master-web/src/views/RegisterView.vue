<template>
  <div class="auth-page">
    <div class="auth-page__bar">
      <router-link to="/" class="auth-page__back">← 返回首页</router-link>
    </div>

    <div class="auth-card">
      <div class="auth-card__logo">AI</div>
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
            <router-link to="/agreement" target="_blank" class="register-agreement__link">《用户协议》</router-link>
            和
            <router-link to="/agreement" target="_blank" class="register-agreement__link">《隐私政策》</router-link>
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
    <div class="auth-page__bg" aria-hidden="true">
      <span class="app-orb app-orb--blue auth-orb auth-orb--1" />
      <span class="app-orb app-orb--orange auth-orb auth-orb--2" />
      <span class="app-orb app-orb--purple auth-orb auth-orb--3" />
    </div>
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
.auth-page {
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

.theme-dark .auth-page {
  background: linear-gradient(165deg, #14171c 0%, #101318 50%, #0d1014 100%);
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
  max-width: 400px;
  padding: 36px 32px;
  background: var(--app-card);
  border: 2px solid var(--app-border);
  border-radius: var(--app-radius-md);
  box-shadow: 0 4px 0 var(--app-border), 0 12px 40px rgba(0, 0, 0, 0.08);
  text-align: center;
  animation: app-fade-up 0.5s ease both;
}

.auth-card__logo {
  width: 48px;
  height: 48px;
  margin: 0 auto var(--app-space-md);
  border-radius: 4px;
  background: linear-gradient(135deg, #409eff, #5db2ff);
  color: #fff;
  font-weight: 800;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 3px 3px 0 rgba(64, 158, 255, 0.55);
}

.auth-title {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 800;
  color: var(--app-text);
}

.auth-desc {
  margin: 0 0 var(--app-space-xl);
  font-size: 13px;
  color: var(--app-text-secondary);
}

.auth-form {
  text-align: left;
}

.auth-form :deep(.el-input__wrapper) {
  border-radius: 10px;
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
  border-radius: 10px;
  min-width: 120px;
  font-weight: 500;
}

.auth-submit {
  width: 100%;
  border-radius: 3px;
  font-weight: 600;
  background: #409eff;
  border: none;
  box-shadow: 0 4px 0 #2f7fd6;
  transition: transform 0.08s ease, box-shadow 0.08s ease, filter 0.2s ease;
}

.auth-submit:hover {
  box-shadow: 0 6px 0 #2f7fd6;
  transform: translateY(-1px);
  filter: brightness(1.05);
}

.auth-submit:active {
  box-shadow: 0 0 0 #2f7fd6;
  transform: translateY(4px);
}

.auth-switch {
  margin: var(--app-space-lg) 0 0;
  font-size: 14px;
  color: var(--app-text-secondary);
  text-align: center;
}

.auth-switch__link {
  color: var(--app-accent-blue);
  text-decoration: none;
  margin-left: 4px;
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
  color: var(--app-text-secondary);
}

.register-agreement__link {
  color: var(--app-accent-blue);
  text-decoration: none;
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
  color: var(--app-text-secondary);
  font-family: 'JetBrains Mono', 'Consolas', monospace;
}

.auth-page__status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--app-accent-blue);
  animation: app-pulse 1.5s ease-in-out infinite;
}

/* 背景装饰 */
.auth-page__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.auth-orb--1 {
  width: 360px;
  height: 360px;
  top: -100px;
  right: -80px;
}

.auth-orb--2 {
  width: 300px;
  height: 300px;
  bottom: -60px;
  left: -80px;
  animation-delay: 2s;
}

.auth-orb--3 {
  width: 200px;
  height: 200px;
  top: 30%;
  left: 12%;
  animation-delay: 4s;
  opacity: 0.8;
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
