<template>
  <div class="feedback-page">
    <div class="feedback-page__bar">
      <router-link to="/" class="feedback-page__back">← 返回首页</router-link>
    </div>

    <div class="feedback-card">
      <div class="feedback-card__logo">💬</div>
      <h1 class="feedback-title">意见反馈</h1>
      <p class="feedback-desc">你的每一个建议，都可能是下一个 Commit 的 Message</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="feedback-form"
      >
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" size="large" placeholder="你的昵称（默认使用登录用户名）" />
        </el-form-item>
        <el-form-item label="联系方式（选填）" prop="contact">
          <el-input v-model="form.contact" size="large" placeholder="邮箱 / 微信号，便于我们联系你" />
        </el-form-item>
        <el-form-item label="反馈内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            maxlength="2000"
            show-word-limit
            placeholder="想吐槽什么、想要什么功能、发现什么 Bug，都欢迎告诉我们…"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="feedback-submit"
            @click="handleSubmit"
          >
            提交反馈
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 背景装饰 -->
    <div class="feedback-page__bg" aria-hidden="true">
      <span class="app-orb app-orb--blue feedback-orb feedback-orb--1" />
      <span class="app-orb app-orb--orange feedback-orb feedback-orb--2" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import { postFeedback } from '../api/user';
import { useAuthStore } from '../store/authStore';

const authStore = useAuthStore();
const formRef = ref<FormInstance>();
const loading = ref(false);

const form = reactive({
  nickname: '',
  contact: '',
  content: '',
});

const rules: FormRules = {
  content: [{ required: true, message: '请填写反馈内容', trigger: 'blur' }],
};

onMounted(() => {
  if (authStore.username) form.nickname = authStore.username;
});

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      await postFeedback({ contact: form.contact.trim(), content: form.content.trim() });
      ElMessage.success('反馈提交成功，感谢您的建议！');
      form.contact = '';
      form.content = '';
    } catch {
      ElMessage.error('提交失败，请稍后重试');
    } finally {
      loading.value = false;
    }
  });
}
</script>

<style scoped>
.feedback-page {
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

.theme-dark .feedback-page {
  background: linear-gradient(165deg, #14171c 0%, #101318 50%, #0d1014 100%);
}

.feedback-page__bar {
  position: absolute;
  top: var(--app-space-lg);
  left: var(--app-space-xl);
  z-index: 5;
}

.feedback-page__back {
  font-size: 14px;
  color: var(--app-accent-blue);
  text-decoration: none;
}

.feedback-page__back:hover {
  text-decoration: underline;
}

.feedback-card {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 520px;
  padding: 36px 32px;
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
  animation: app-fade-up 0.5s ease both;
}

.feedback-card__logo {
  font-size: 40px;
  text-align: center;
  margin-bottom: var(--app-space-md);
}

.feedback-title {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 800;
  text-align: center;
  color: var(--app-text);
}

.feedback-desc {
  margin: 0 0 var(--app-space-xl);
  font-size: 13px;
  text-align: center;
  color: var(--app-text-secondary);
}

.feedback-form :deep(.el-input__wrapper),
.feedback-form :deep(.el-textarea__inner) {
  border-radius: 10px;
}

.feedback-submit {
  width: 100%;
  border-radius: 9999px;
  font-weight: 600;
  background: #409eff;
  border: none;
  box-shadow: 0 4px 18px rgba(64, 158, 255, 0.4);
}

.feedback-submit:hover {
  box-shadow: 0 6px 22px rgba(64, 158, 255, 0.5);
}

.feedback-page__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.feedback-orb--1 {
  width: 360px;
  height: 360px;
  top: -100px;
  right: -80px;
}

.feedback-orb--2 {
  width: 300px;
  height: 300px;
  bottom: -60px;
  left: -80px;
  animation-delay: 2s;
}

@media (max-width: 767px) {
  .feedback-page {
    padding: var(--app-space-md);
  }

  .feedback-card {
    padding: 28px 22px;
  }
}
</style>
