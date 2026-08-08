<template>
  <div class="auth-page">
    <div class="auth-page__bar">
      <router-link to="/" class="auth-page__back">← 返回首页</router-link>
    </div>

    <div class="auth-card">
      <div class="auth-card__logo">AI</div>
      <h1 class="auth-title">登录</h1>
      <p class="auth-desc">登录后使用职规大师、超级智能体等功能</p>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="auth-form"
        @submit.prevent="handleLogin"
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
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            autocomplete="current-password"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="auth-submit"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
        <p class="auth-switch">
          还没有账号？
          <router-link to="/register" class="auth-switch__link">立即注册</router-link>
        </p>
        <p class="auth-agreement">
          登录即代表同意
          <router-link to="/agreement" target="_blank" class="auth-agreement__link">《用户协议》</router-link>
          与
          <router-link to="/agreement#privacy" target="_blank" class="auth-agreement__link">《隐私政策》</router-link>
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
import { ref, reactive } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { User, Lock } from '@element-plus/icons-vue';
import type { FormInstance, FormRules } from 'element-plus';
import { useAuthStore } from '../store/authStore';
import { ElMessage } from 'element-plus';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const formRef = ref<FormInstance>();
const loading = ref(false);
const form = reactive({
  username: '',
  password: '',
});

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

async function handleLogin() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      await authStore.login(form);
      ElMessage.success('登录成功，正在跳转…');
      const redirect = (route.query.redirect as string) || '/';
      setTimeout(() => router.replace(redirect), 400);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : '登录失败，请稍后重试';
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

.auth-agreement {
  margin: var(--app-space-sm) 0 0;
  font-size: 12px;
  color: var(--app-text-secondary);
  text-align: center;
}

.auth-agreement__link {
  color: var(--app-accent-blue);
  text-decoration: none;
}

.auth-agreement__link:hover {
  text-decoration: underline;
}

.auth-switch__link:hover {
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
