<template>
  <div class="admin-page">
    <div class="admin-page__bar">
      <router-link to="/" class="admin-page__back">← 返回首页</router-link>
      <h1 class="admin-page__title">管理后台</h1>
    </div>

    <main class="admin-page__body">
      <el-tabs v-model="activeTab" class="admin-tabs">
        <!-- 运营看板 -->
        <el-tab-pane label="运营看板" name="stats">
          <div class="stats-grid">
            <div class="stats-card">
              <span class="stats-card__label">注册用户</span>
              <span class="stats-card__value app-num">{{ stats.users?.total ?? 0 }}</span>
              <span class="stats-card__sub">本周新增 {{ stats.users?.newWeek ?? 0 }}</span>
            </div>
            <div class="stats-card">
              <span class="stats-card__label">今日活跃</span>
              <span class="stats-card__value app-num">{{ stats.activeToday ?? 0 }}</span>
              <span class="stats-card__sub">有对话或签到的用户</span>
            </div>
            <div class="stats-card">
              <span class="stats-card__label">VIP 用户</span>
              <span class="stats-card__value app-num">{{ stats.users?.vip ?? 0 }}</span>
              <span class="stats-card__sub">付费转化基础盘</span>
            </div>
            <div class="stats-card">
              <span class="stats-card__label">累计对话</span>
              <span class="stats-card__value app-num">{{ stats.conversations?.total ?? 0 }}</span>
              <span class="stats-card__sub">本周 {{ stats.conversations?.week ?? 0 }} 次</span>
            </div>
            <div class="stats-card">
              <span class="stats-card__label">本周签到</span>
              <span class="stats-card__value app-num">{{ stats.weekSignDays ?? 0 }}</span>
              <span class="stats-card__sub">人次 · 八股打卡 {{ stats.weekCheckinDays ?? 0 }}</span>
            </div>
            <div class="stats-card">
              <span class="stats-card__label">本周积分</span>
              <span class="stats-card__value app-num">+{{ stats.points?.earned ?? 0 }}</span>
              <span class="stats-card__sub">发放 {{ stats.points?.earned ?? 0 }} / 消耗 {{ stats.points?.spent ?? 0 }}</span>
            </div>
            <div class="stats-card">
              <span class="stats-card__label">商城兑换</span>
              <span class="stats-card__value app-num">{{ stats.redeems?.count ?? 0 }}</span>
              <span class="stats-card__sub">累计消耗 {{ stats.redeems?.points ?? 0 }} 积分</span>
            </div>
            <div class="stats-card">
              <span class="stats-card__label">面试模拟</span>
              <span class="stats-card__value app-num">{{ stats.interviews?.total ?? 0 }}</span>
              <span class="stats-card__sub">本周 {{ stats.interviews?.week ?? 0 }} 场（VIP 卖点）</span>
            </div>
            <!-- 转化漏斗 -->
            <div class="stats-card stats-card--wide">
              <span class="stats-card__label">用户转化漏斗（相对注册数）</span>
              <div class="funnel">
                <div v-for="(s, i) in stats.funnel" :key="i" class="funnel__row">
                  <span class="funnel__stage" :class="{ 'is-key': i === stats.funnel.length - 1 }">{{ s.stage }}</span>
                  <el-progress
                    :percentage="Math.min(100, s.rate)"
                    :stroke-width="10"
                    :show-text="false"
                    :color="funnelColor(i, s.rate)"
                  />
                  <span class="funnel__count app-num">{{ s.count }}</span>
                  <span class="funnel__rate">{{ s.rate }}%</span>
                </div>
              </div>
            </div>
            <div class="stats-card stats-card--wide">
              <span class="stats-card__label">本周积分消耗去向</span>
              <div v-if="stats.spendTop?.length" class="stats-top">
                <div v-for="(s, i) in stats.spendTop" :key="i" class="stats-top__row">
                  <span class="stats-top__reason">{{ s.reason }}</span>
                  <el-progress :percentage="Math.round((s.points / (stats.points?.spent || 1)) * 100)" :stroke-width="6" color="#2f6bff" />
                  <span class="stats-top__points app-num">{{ s.points }}</span>
                </div>
              </div>
              <span v-else class="stats-card__sub">本周暂无积分消耗</span>
            </div>
          </div>
        </el-tab-pane>

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

        <!-- AI 设置 -->
        <el-tab-pane label="AI 设置" name="ai">
          <div class="admin-ai">
            <div class="admin-ai__preview">
              <el-avatar :size="72" :src="aiAvatar || undefined">{{ aiAvatar ? '' : 'AI' }}</el-avatar>
              <p class="admin-ai__tip">该头像将展示给所有用户（AI 助手头像），上传后全局生效</p>
            </div>
            <el-upload
              :show-file-list="false"
              accept="image/jpeg,image/png,image/webp,image/gif"
              :http-request="handleUploadAiAvatar"
            >
              <el-button type="primary">上传 / 更换 AI 头像</el-button>
            </el-upload>
          </div>
        </el-tab-pane>

        <!-- 错误日志 -->
        <el-tab-pane label="错误日志" name="errors">
          <div class="admin-toolbar admin-errors__toolbar">
            <el-select v-model="errorSource" placeholder="全部来源" clearable size="small" style="width: 120px" @change="loadErrorLogs">
              <el-option label="后端" value="backend" />
              <el-option label="前端" value="frontend" />
            </el-select>
            <el-select v-model="errorLevel" placeholder="全部级别" clearable size="small" style="width: 120px" @change="loadErrorLogs">
              <el-option label="ERROR" value="ERROR" />
              <el-option label="WARN" value="WARN" />
            </el-select>
            <el-button size="small" type="danger" plain @click="handleClearErrorLogs">清空日志</el-button>
          </div>
          <el-table :data="errorLogs" stripe style="width: 100%">
            <el-table-column type="expand">
              <template #default="{ row }">
                <pre class="admin-errors__stack">{{ row.stackTrace || '（无堆栈）' }}</pre>
              </template>
            </el-table-column>
            <el-table-column label="时间" min-width="150">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="来源" width="80">
              <template #default="{ row }">
                <el-tag :type="row.source === 'backend' ? 'danger' : 'warning'" size="small">{{ row.source }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="级别" width="80">
              <template #default="{ row }">
                <el-tag :type="row.level === 'ERROR' ? 'danger' : 'info'" size="small">{{ row.level }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="错误摘要" min-width="220" show-overflow-tooltip />
            <el-table-column prop="uri" label="接口" min-width="140" show-overflow-tooltip />
            <el-table-column prop="username" label="用户" width="90" />
          </el-table>
          <div v-if="errorLogs.length === 0" class="admin-empty">暂无错误日志</div>
        </el-tab-pane>

        <!-- 知识库管理 -->
        <el-tab-pane label="知识库管理" name="knowledge">
          <div class="admin-toolbar admin-errors__toolbar">
            <el-input
              v-model="kbKeyword"
              placeholder="搜索知识内容"
              clearable
              size="small"
              style="width: 200px"
              @change="handleKbSearch"
              @clear="handleKbSearch"
            />
            <el-select v-model="kbCategory" placeholder="全部分类" clearable size="small" style="width: 150px" @change="handleKbSearch">
              <el-option v-for="c in kbCategories" :key="c.category" :label="`${c.category}(${c.count})`" :value="c.category" />
            </el-select>
            <el-button size="small" type="primary" @click="openKbDialog()">新增知识</el-button>
            <el-button size="small" @click="handleRebuildKnowledge">重建索引</el-button>
            <el-tag v-if="kbRebuild.rebuilding" type="warning" size="small">索引重建中，约 1-2 分钟...</el-tag>
            <el-tag
              v-else-if="kbRebuild.info"
              :type="kbRebuild.status === 'success' ? 'success' : kbRebuild.status === 'failed' ? 'danger' : 'info'"
              size="small"
            >{{ kbRebuild.info }}</el-tag>
          </div>
          <el-table :data="kbList" stripe style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="category" label="分类" width="100">
              <template #default="{ row }"><el-tag size="small">{{ row.category }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-switch :model-value="row.enabled === 1" size="small" @change="(v: boolean) => handleToggleKnowledge(row, v)" />
              </template>
            </el-table-column>
            <el-table-column label="更新时间" min-width="150">
              <template #default="{ row }">{{ formatTime(row.updateTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small" @click="openKbDialog(row)">编辑</el-button>
                <el-button size="small" type="danger" plain @click="handleDeleteKnowledge(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="kbTotal > 0"
            background
            layout="prev, pager, next, total"
            :total="kbTotal"
            :page-size="kbPageSize"
            :current-page="kbPage + 1"
            style="margin-top: 12px; justify-content: flex-end"
            @current-change="(p: number) => { kbPage = p - 1; loadKnowledge(); }"
          />
          <div v-if="kbList.length === 0" class="admin-empty">暂无知识条目</div>
        </el-tab-pane>
      </el-tabs>
    </main>

    <!-- 发布/编辑公告对话框 -->
    <el-dialog v-model="annDialogVisible" :title="editingAnn ? '编辑公告' : '发布公告'" width="min(460px, 92vw)">
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

    <!-- 新增/编辑知识条目对话框 -->
    <el-dialog v-model="kbDialogVisible" :title="editingKb ? '编辑知识' : '新增知识'" width="min(560px, 94vw)">
      <el-form :model="kbForm" label-position="top">
        <el-form-item label="分类（留空自动识别）">
          <el-input v-model="kbForm.category" maxlength="32" placeholder="如：后端 / 算法 / 面试，留空自动分类" />
        </el-form-item>
        <el-form-item label="知识内容">
          <el-input
            v-model="kbForm.content"
            type="textarea"
            :rows="8"
            maxlength="4000"
            show-word-limit
            placeholder="一段求职/面试知识点，RAG 检索与八股练习共用"
          />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="kbForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="kbDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!kbForm.content.trim()" @click="handleSaveKnowledge">保存</el-button>
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
  clearAdminErrorLogs,
  createAnnouncement,
  createKnowledge,
  deleteAnnouncement,
  deleteFeedback,
  deleteKnowledge,
  getAdminAnnouncements,
  getAdminErrorLogs,
  getAdminFeedbacks,
  getAdminKnowledge,
  getAdminStats,
  type AdminStats,

  getAdminUsers,
  getKnowledgeCategories,
  getKnowledgeRebuildStatus,
  rebuildKnowledge,
  toggleKnowledge,
  updateAnnouncement,
  updateKnowledge,
  uploadAiAvatar,
  type AdminErrorLog,
  type AdminFeedback,
  type AdminUser,
  type Knowledge,
  type KnowledgeCategoryStat,
} from '../api/admin';
import { getAiAvatar } from '../api/user';
import type { Notice } from '../api/notice';

const activeTab = ref('announcements');
// 初始化为默认对象避免 null 访问崩溃（接口返回前首帧渲染即安全）
const stats = ref<AdminStats>({
  users: { total: 0, newWeek: 0, vip: 0 },
  activeToday: 0,
  conversations: { total: 0, week: 0 },
  weekSignDays: 0,
  weekCheckinDays: 0,
  points: { earned: 0, spent: 0 },
  redeems: { count: 0, points: 0 },
  spendTop: [],
  interviews: { total: 0, week: 0 },
  funnel: [],
});
const announcements = ref<Notice[]>([]);
const feedbacks = ref<AdminFeedback[]>([]);
const users = ref<AdminUser[]>([]);
const errorLogs = ref<AdminErrorLog[]>([]);
const errorSource = ref('');
const errorLevel = ref('');

const annDialogVisible = ref(false);
const editingAnn = ref<Notice | null>(null);
const annForm = reactive({ title: '', content: '' });
const aiAvatar = ref('');

/* 知识库管理状态 */
const kbList = ref<Knowledge[]>([]);
const kbTotal = ref(0);
const kbPage = ref(0);
const kbPageSize = ref(10);
const kbKeyword = ref('');
const kbCategory = ref('');
const kbCategories = ref<KnowledgeCategoryStat[]>([]);
const kbDialogVisible = ref(false);
const editingKb = ref<Knowledge | null>(null);
const kbForm = reactive({ category: '', content: '', enabled: true });
const kbRebuild = reactive({ rebuilding: false, status: 'idle', info: '' });

function formatTime(t?: string) {
  if (!t) return '';
  return String(t).slice(0, 19).replace('T', ' ');
}

/** 漏斗进度条颜色：最后阶段(VIP)高亮，其余按转化率降序渐变 */
function funnelColor(index: number, rate: number) {
  if (index === 4) return '#16a34a';
  if (rate >= 50) return '#2f6bff';
  if (rate >= 20) return '#7b5bff';
  return '#94a3b8';
}

async function loadStats() {
  try {
    const res = await getAdminStats();
    if (res.data) stats.value = res.data;
  } catch {
    // 拦截器已提示
  }
}

async function loadAll() {
  try {
    const [a, f, u] = await Promise.all([getAdminAnnouncements(), getAdminFeedbacks(), getAdminUsers()]);
    announcements.value = a.data ?? [];
    feedbacks.value = f.data ?? [];
    users.value = u.data ?? [];
    const ai = await getAiAvatar();
    aiAvatar.value = ai.data?.avatar ?? '';
  } catch {
    // 403/401 由拦截器处理
  }
}

/** 加载错误日志（可按来源/级别过滤） */
async function loadErrorLogs() {
  try {
    const res = await getAdminErrorLogs({
      source: errorSource.value || undefined,
      level: errorLevel.value || undefined,
    });
    errorLogs.value = res.data ?? [];
  } catch {
    // 拦截器已提示
  }
}

/** 清空全部错误日志 */
async function handleClearErrorLogs() {
  try {
    await ElMessageBox.confirm('确定清空全部错误日志吗？', '提示', { type: 'warning' });
    await clearAdminErrorLogs();
    ElMessage.success('已清空');
    await loadErrorLogs();
  } catch {
    // 取消或失败
  }
}

/** 上传/更换 AI 头像（覆盖固定 key，全局生效） */
async function handleUploadAiAvatar(options: { file: File }) {
  try {
    const res = await uploadAiAvatar(options.file);
    aiAvatar.value = res.data?.avatar ?? '';
    ElMessage.success('AI 头像已更新，所有用户可见');
  } catch {
    // 错误提示由 http 拦截器处理
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

/* ===== 知识库管理 ===== */

/** 加载知识库分页列表（搜索/分类/分页） */
async function loadKnowledge() {
  try {
    const res = await getAdminKnowledge({
      category: kbCategory.value || undefined,
      keyword: kbKeyword.value || undefined,
      page: kbPage.value,
      size: kbPageSize.value,
    });
    kbList.value = res.data?.list ?? [];
    kbTotal.value = res.data?.total ?? 0;
  } catch {
    // 拦截器已提示
  }
}

/** 加载分类统计 + 重建状态 */
async function loadKbMeta() {
  try {
    const [c, s] = await Promise.all([getKnowledgeCategories(), getKnowledgeRebuildStatus()]);
    kbCategories.value = c.data ?? [];
    Object.assign(kbRebuild, s.data ?? { rebuilding: false, status: 'idle', info: '' });
  } catch {
    // 拦截器已提示
  }
}

/** 搜索/筛选变化时回到第一页 */
function handleKbSearch() {
  kbPage.value = 0;
  loadKnowledge();
}

/** 打开新增/编辑对话框 */
function openKbDialog(k?: Knowledge) {
  editingKb.value = k ?? null;
  kbForm.category = k?.category ?? '';
  kbForm.content = k?.content ?? '';
  kbForm.enabled = k ? k.enabled === 1 : true;
  kbDialogVisible.value = true;
}

/** 保存知识（新增/编辑），保存后触发异步重建 */
async function handleSaveKnowledge() {
  const payload = {
    content: kbForm.content.trim(),
    category: kbForm.category.trim() || undefined,
    enabled: kbForm.enabled,
  };
  try {
    if (editingKb.value) {
      await updateKnowledge(editingKb.value.id, payload);
      ElMessage.success('知识已更新，正在重建索引');
    } else {
      await createKnowledge(payload);
      ElMessage.success('知识已新增，正在重建索引');
    }
    kbDialogVisible.value = false;
    await loadKnowledge();
    await loadKbMeta();
  } catch {
    // 拦截器已提示
  }
}

/** 启停知识段（停用则不进检索与八股列表） */
async function handleToggleKnowledge(row: Knowledge, enabled: boolean) {
  try {
    await toggleKnowledge(row.id, enabled);
    ElMessage.success(enabled ? '已启用，正在重建索引' : '已停用，正在重建索引');
    await loadKnowledge();
    await loadKbMeta();
  } catch {
    // 拦截器已提示
  }
}

/** 删除知识段 */
async function handleDeleteKnowledge(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该知识段吗？删除后立即从检索与八股中移除。', '提示', { type: 'warning' });
    await deleteKnowledge(id);
    ElMessage.success('知识已删除，正在重建索引');
    await loadKnowledge();
    await loadKbMeta();
  } catch {
    // 取消或失败
  }
}

/** 手动触发全量索引重建，并轮询状态 */
async function handleRebuildKnowledge() {
  try {
    await ElMessageBox.confirm('确定全量重建索引吗？耗时约 1-2 分钟，期间检索仍用旧索引。', '提示', { type: 'warning' });
    await rebuildKnowledge();
    ElMessage.success('重建已启动');
    kbRebuild.rebuilding = true;
    kbRebuild.status = 'running';
    kbRebuild.info = '重建中，约 1-2 分钟...';
    pollRebuildStatus();
  } catch {
    // 取消或失败
  }
}

/** 轮询重建状态（重建中每 5s 查一次） */
function pollRebuildStatus() {
  const timer = window.setInterval(async () => {
    try {
      const res = await getKnowledgeRebuildStatus();
      Object.assign(kbRebuild, res.data ?? {});
      if (!kbRebuild.rebuilding) {
        window.clearInterval(timer);
        if (kbRebuild.status === 'success') ElMessage.success('索引重建完成');
      }
    } catch {
      window.clearInterval(timer);
    }
  }, 5000);
}

onMounted(() => {
  loadAll();
  loadErrorLogs();
  loadKnowledge();
  loadKbMeta();
  loadStats();
});
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

/* 运营看板 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: var(--app-space-md);
}

.stats-card {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-md);
  box-shadow: var(--app-shadow-sm);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stats-card--wide {
  grid-column: 1 / -1;
}

.stats-card__label {
  font-size: 12px;
  color: var(--app-text-secondary);
  font-weight: 600;
}

.stats-card__value {
  font-size: 30px;
  font-weight: 800;
  color: var(--app-primary);
  line-height: 1.1;
}

.stats-card__sub {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.stats-top {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 4px;
}

.stats-top__row {
  display: grid;
  grid-template-columns: 200px 1fr 50px;
  align-items: center;
  gap: 12px;
}

.stats-top__reason {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stats-top__points {
  font-size: 14px;
  font-weight: 700;
  text-align: right;
}

/* 转化漏斗 */
.funnel {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 4px;
}

.funnel__row {
  display: grid;
  grid-template-columns: 110px 1fr 50px 56px;
  align-items: center;
  gap: 12px;
}

.funnel__stage {
  font-size: 13px;
  font-weight: 600;
}

.funnel__stage.is-key {
  color: #16a34a;
}

.funnel__count {
  font-size: 14px;
  font-weight: 800;
  text-align: right;
}

.funnel__rate {
  font-size: 12px;
  color: var(--app-text-secondary);
  text-align: right;
}

/* 移动端：漏斗行紧凑化，避免横向溢出 */
@media (max-width: 560px) {
  .funnel__row {
    grid-template-columns: 84px 1fr 32px 46px;
    gap: 8px;
  }

  .funnel__stage {
    font-size: 12px;
  }
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

/* 错误日志面板 */
.admin-errors__toolbar {
  display: flex;
  align-items: center;
  gap: var(--app-space-sm);
  flex-wrap: wrap;
}

.admin-errors__stack {
  margin: 0;
  padding: var(--app-space-md);
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  color: var(--app-text-secondary);
}

.theme-dark .admin-errors__stack {
  background: rgba(255, 255, 255, 0.06);
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

  /* 表格在手机上保持横向滚动，不被挤压变形 */
  .admin-tabs .el-table {
    width: 100%;
  }

  .admin-tabs .el-table__body-wrapper {
    overflow-x: auto;
  }

  /* 卡片头部：标题与时间在小屏允许换行 */
  .admin-ann__head,
  .admin-fb__head {
    flex-wrap: wrap;
    gap: 4px 10px;
  }

  .admin-ann__actions,
  .admin-fb__actions {
    text-align: left;
  }
}
</style>
