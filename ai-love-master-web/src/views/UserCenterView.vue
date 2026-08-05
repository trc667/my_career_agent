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
import { changePassword, getUserMe, uploadAvatar, type UserInfo } from '../api/user';
import { useAuthStore } from '../store/authStore';

const router = useRouter();
const authStore = useAuthStore();

const info = reactive<UserInfo>({ id: 0, username: '', createTime: '', avatar: '' });
const formRef = ref<FormInstance>();
const saving = ref(false);

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
