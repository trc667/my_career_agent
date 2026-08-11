<template>
  <div class="auth-page">
    <div class="auth-page__bar">
      <router-link to="/" class="auth-page__back">← 返回首页</router-link>
    </div>

    <div class="auth-card">
      <img src="/logo.jpg" alt="职规大师 Logo" class="auth-card__logo" />
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
          <router-link to="/agreement" target="_blank" class="auth-agreement__link">《用户协议与隐私政策》</router-link>
        </p>
      </el-form>
    </div>

    <div class="auth-page__status">
      <span class="auth-page__status-dot" />
      <span>AUTH MODULE · v1.0</span>
    </div>

    <!-- 背景装饰（仿 afgprogrammer mYQQJV：SVG 渐变 blob + 代码符号） -->
    <AuthDecor />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { User, Lock } from '@element-plus/icons-vue';
import type { FormInstance, FormRules } from 'element-plus';
import { useAuthStore } from '../store/authStore';
import { ElMessage } from 'element-plus';
import AuthDecor from '../components/AuthDecor.vue';

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

.auth-agreement {
  margin: 10px 0 0;
  font-size: 12px;
  color: #999;
  text-align: center;
}

.auth-agreement__link {
  color: #2f6bff;
  text-decoration: none;
}

.theme-dark .auth-agreement__link {
  color: #6ea8ff;
}

.auth-agreement__link:hover,
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
    padding: 30px 24px;
  }
}
</style>
