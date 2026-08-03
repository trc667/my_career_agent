<template>
  <div class="admin-page">
    <div class="admin-page__bar">
      <router-link to="/" class="admin-page__back">← 返回首页</router-link>
      <h1 class="admin-page__title">管理后台</h1>
    </div>

    <main class="admin-page__body">
      <el-tabs v-model="activeTab" class="admin-tabs">
        <!-- 公告管理 -->
        <el-tab-pane label="公告管理" name="announcements">
          <div class="admin-toolbar">
            <el-button type="primary" @click="openAnnounceDialog()">发布公告</el-button>
          </div>
          <div v-if="announcements.length === 0" class="admin-empty">暂无公告</div>
          <div v-for="a in announcements" :key="a.id" class="admin-ann">
            <div class="admin-ann__head">
              <span class="admin-ann__title">{{ a.title }}</span>
              <span class="admin-ann__time">{{ formatTime(a.createTime) }}</span>
            </div>
            <div class="admin-ann__content">{{ a.content }}</div>
            <div class="admin-ann__actions">
              <el-button size="small" @click="openAnnounceDialog(a)">编辑</el-button>
              <el-button size="small" type="danger" plain @click="handleDeleteAnnouncement(a.id)">删除</el-button>
            </div>
          </div>
        </el-tab-pane>

        <!-- 意见反馈 -->
        <el-tab-pane label="意见反馈" name="feedbacks">
          <div v-if="feedbacks.length === 0" class="admin-empty">暂无反馈</div>
          <div v-for="f in feedbacks" :key="f.id" class="admin-fb">
            <div class="admin-fb__head">
              <span class="admin-fb__user">👤 {{ f.username }}</span>
              <span v-if="f.contact" class="admin-fb__contact">{{ f.contact }}</span>
              <span class="admin-fb__time">{{ formatTime(f.createTime) }}</span>
            </div>
            <div class="admin-fb__content">{{ f.content }}</div>
            <div class="admin-fb__actions">
              <el-button size="small" type="danger" plain @click="handleDeleteFeedback(f.id)">删除</el-button>
            </div>
          </div>
        </el-tab-pane>

        <!-- 用户列表 -->
        <el-tab-pane label="用户列表" name="users">
          <el-table :data="users" stripe style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="username" label="用户名" min-width="120" />
            <el-table-column prop="email" label="邮箱" min-width="180" />
            <el-table-column label="角色" width="100">
              <template #default="{ row }">
                <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">
                  {{ row.role }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="注册时间" min-width="170">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </main>

    <!-- 发布/编辑公告对话框 -->
    <el-dialog v-model="annDialogVisible" :title="editingAnn ? '编辑公告' : '发布公告'" width="460px">
      <el-form :model="annForm" label-position="top">
        <el-form-item label="标题">
          <el-input v-model="annForm.title" maxlength="128" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="annForm.content" type="textarea" :rows="6" maxlength="4000" show-word-limit placeholder="请输入公告内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="annDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!annForm.title.trim() || !annForm.content.trim()" @click="handleSaveAnnouncement">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 背景装饰 -->
    <div class="admin-page__bg" aria-hidden="true">
      <span class="app-orb app-orb--blue admin-orb admin-orb--1" />
      <span class="app-orb app-orb--purple admin-orb admin-orb--2" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  createAnnouncement,
  deleteAnnouncement,
  deleteFeedback,
  getAdminAnnouncements,
  getAdminFeedbacks,
  getAdminUsers,
  updateAnnouncement,
  type AdminFeedback,
  type AdminUser,
} from '../api/admin';
import type { Notice } from '../api/notice';

const activeTab = ref('announcements');
const announcements = ref<Notice[]>([]);
const feedbacks = ref<AdminFeedback[]>([]);
const users = ref<AdminUser[]>([]);

const annDialogVisible = ref(false);
const editingAnn = ref<Notice | null>(null);
const annForm = reactive({ title: '', content: '' });

function formatTime(t?: string) {
  if (!t) return '';
  return String(t).slice(0, 19).replace('T', ' ');
}

async function loadAll() {
  try {
    const [a, f, u] = await Promise.all([getAdminAnnouncements(), getAdminFeedbacks(), getAdminUsers()]);
    announcements.value = a.data ?? [];
    feedbacks.value = f.data ?? [];
    users.value = u.data ?? [];
  } catch {
    // 403/401 由拦截器处理
  }
}

function openAnnounceDialog(ann?: Notice) {
  editingAnn.value = ann ?? null;
  annForm.title = ann?.title ?? '';
  annForm.content = ann?.content ?? '';
  annDialogVisible.value = true;
}

async function handleSaveAnnouncement() {
  const payload = { title: annForm.title.trim(), content: annForm.content.trim() };
  try {
    if (editingAnn.value) {
      await updateAnnouncement(editingAnn.value.id, payload);
      ElMessage.success('公告已更新');
    } else {
      await createAnnouncement(payload);
      ElMessage.success('公告已发布');
    }
    annDialogVisible.value = false;
    await loadAll();
  } catch {
    // 拦截器已提示
  }
}

async function handleDeleteAnnouncement(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该公告吗？', '提示', { type: 'warning' });
    await deleteAnnouncement(id);
    ElMessage.success('公告已删除');
    await loadAll();
  } catch {
    // 取消或失败
  }
}

async function handleDeleteFeedback(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该反馈吗？', '提示', { type: 'warning' });
    await deleteFeedback(id);
    ElMessage.success('反馈已删除');
    await loadAll();
  } catch {
    // 取消或失败
  }
}

onMounted(loadAll);
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: linear-gradient(165deg, #f6f8fb 0%, #eef2f7 50%, #e6ebf2 100%);
  color: var(--app-text);
  padding: 0 var(--app-space-xl) 60px;
}

.theme-dark .admin-page {
  background: linear-gradient(165deg, #14171c 0%, #101318 50%, #0d1014 100%);
}

.admin-page__bar {
  position: relative;
  z-index: 2;
  max-width: var(--app-content-max);
  width: 100%;
  margin: 0 auto;
  padding: var(--app-space-lg) 0;
  display: flex;
  align-items: center;
  gap: var(--app-space-lg);
}

.admin-page__back {
  font-size: 14px;
  color: var(--app-accent-blue);
  text-decoration: none;
}

.admin-page__back:hover {
  text-decoration: underline;
}

.admin-page__title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 1px;
}

.admin-page__body {
  position: relative;
  z-index: 2;
  max-width: var(--app-content-max);
  width: 100%;
  margin: 0 auto;
}

.admin-toolbar {
  margin-bottom: var(--app-space-md);
}

.admin-empty {
  text-align: center;
  padding: 60px 0;
  color: var(--app-text-secondary);
  font-size: 14px;
}

.admin-ann,
.admin-fb {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-md);
  padding: 14px 18px;
  margin-bottom: var(--app-space-md);
}

.admin-ann__head,
.admin-fb__head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.admin-ann__title {
  font-weight: 700;
  font-size: 15px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-fb__user {
  font-weight: 600;
  font-size: 14px;
}

.admin-fb__contact {
  font-size: 12px;
  color: var(--app-accent-blue);
}

.admin-ann__time,
.admin-fb__time {
  font-size: 12px;
  color: var(--app-text-secondary);
  flex-shrink: 0;
}

.admin-ann__content,
.admin-fb__content {
  font-size: 13px;
  line-height: 1.7;
  color: var(--app-text-secondary);
  white-space: pre-wrap;
  margin-bottom: 8px;
}

.admin-ann__actions,
.admin-fb__actions {
  text-align: right;
}

.admin-page__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.admin-orb--1 {
  width: 380px;
  height: 380px;
  top: -120px;
  right: -100px;
}

.admin-orb--2 {
  width: 320px;
  height: 320px;
  bottom: -80px;
  left: -100px;
  animation-delay: 2s;
}

@media (max-width: 767px) {
  .admin-page {
    padding: 0 var(--app-space-md) 40px;
  }
}
</style>
