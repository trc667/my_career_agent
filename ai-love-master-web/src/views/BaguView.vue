<template>
  <div class="bagu-page">
    <div class="bagu-page__bar">
      <router-link to="/" class="bagu-page__back">← 返回首页</router-link>
      <h1 class="bagu-page__title">AI 八股练习场</h1>
    </div>

    <main class="bagu-page__body">
      <!-- 分类筛选 -->
      <div class="bagu-filters">
        <el-radio-group v-model="category" @change="handleFilterChange">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button v-for="c in categories" :key="c.category" :value="c.category">
            {{ c.category }}({{ c.count }})
          </el-radio-button>
        </el-radio-group>
      </div>

      <!-- 搜索 + 随机 -->
      <div class="bagu-toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索八股关键词…（如：锁 / Kafka / 索引）"
          clearable
          class="bagu-toolbar__search"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
        <el-button type="warning" class="bagu-toolbar__random" @click="handleRandom">🎲 随机一题</el-button>
        <router-link to="/bagu-practice" class="bagu-toolbar__practice">📖 学习记录</router-link>
      </div>

      <!-- 列表 -->
      <div v-if="loading" class="bagu-loading">
        <el-skeleton :rows="6" animated />
      </div>
      <el-empty v-else-if="list.length === 0" description="暂无匹配的八股，换个关键词试试" />
      <div v-else class="bagu-list">
        <el-card v-for="(item, i) in list" :key="i" class="bagu-card" shadow="hover">
          <div class="bagu-card__head">
            <el-tag size="small" :type="tagType(item.category)">{{ item.category }}</el-tag>
            <span class="bagu-card__no">#{{ page * size + i + 1 }}</span>
          </div>
          <p class="bagu-card__content" :class="{ 'bagu-card__content--expanded': expanded[i] }">
            {{ item.content }}
          </p>
          <div class="bagu-card__actions">
            <el-button size="small" link type="primary" @click="toggleExpand(i)">
              {{ expanded[i] ? '收起' : '展开全文' }}
            </el-button>
            <el-button size="small" link type="success" @click="handleExplain(item)">🤖 AI 深入讲解</el-button>
          </div>
        </el-card>
      </div>

      <!-- 分页 -->
      <el-pagination
        v-if="total > size"
        class="bagu-page__pager"
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page + 1"
        @current-change="handlePageChange"
      />
    </main>

    <!-- 随机一题弹窗 -->
    <el-dialog v-model="randomVisible" title="🎲 随机一题" width="min(560px, 92vw)">
      <template v-if="randomItem">
        <el-tag size="small" :type="tagType(randomItem.category)">{{ randomItem.category }}</el-tag>
        <p class="bagu-random__content">{{ randomItem.content }}</p>
      </template>
      <template #footer>
        <el-button @click="handleRandom">再抽一题</el-button>
        <el-button type="danger" plain :loading="wrongLoading" @click="handleAddWrong(randomItem)">不会 / 答错，加入错题本</el-button>
        <el-button type="primary" :loading="explainLoading" @click="handleExplain(randomItem)">🤖 AI 讲解</el-button>
      </template>
    </el-dialog>

    <!-- AI 讲解弹窗 -->
    <el-dialog v-model="explainVisible" title="🤖 AI 深入讲解" width="min(680px, 92vw)">
      <div v-if="explainLoading" class="bagu-explain__loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>AI 思考中，请稍候…</span>
      </div>
      <div v-else class="bagu-explain__content" v-html="renderMarkdown(explainText)"></div>
    </el-dialog>

    <!-- 背景装饰 -->
    <div class="bagu-page__bg" aria-hidden="true">
      <span class="app-orb app-orb--blue bagu-orb bagu-orb--1" />
      <span class="app-orb app-orb--purple bagu-orb bagu-orb--2" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Loading } from '@element-plus/icons-vue';
import { getBaguCategories, getBaguList, getBaguRandom, addBaguWrong, type BaguCategory, type BaguEntry } from '../api/bagu';
import { postChatRag } from '../api/chat';
import { renderMarkdown } from '../utils/markdown';

const categories = ref<BaguCategory[]>([]);
const category = ref('');
const keyword = ref('');
const list = ref<BaguEntry[]>([]);
const total = ref(0);
const page = ref(0);
const size = ref(10);
const loading = ref(false);
const expanded = ref<Record<number, boolean>>({});

const randomVisible = ref(false);
const randomItem = ref<BaguEntry | null>(null);
const wrongLoading = ref(false);
const explainVisible = ref(false);
const explainLoading = ref(false);
const explainText = ref('');

onMounted(() => {
  loadCategories();
  loadList();
});

async function loadCategories() {
  try {
    const res = await getBaguCategories();
    categories.value = res.data ?? [];
  } catch {
    // 401 由拦截器处理
  }
}

async function loadList() {
  loading.value = true;
  expanded.value = {};
  try {
    const res = await getBaguList({
      category: category.value || undefined,
      keyword: keyword.value?.trim() || undefined,
      page: page.value,
      size: size.value,
    });
    list.value = res.data?.list ?? [];
    total.value = res.data?.total ?? 0;
  } catch {
    // 401 由拦截器处理
  } finally {
    loading.value = false;
  }
}

function handleFilterChange() {
  page.value = 0;
  loadList();
}

function handleSearch() {
  page.value = 0;
  loadList();
}

function handlePageChange(p: number) {
  page.value = p - 1;
  loadList();
}

function toggleExpand(i: number) {
  expanded.value = { ...expanded.value, [i]: !expanded.value[i] };
}

async function handleRandom() {
  try {
    const res = await getBaguRandom(category.value || undefined);
    randomItem.value = res.data ?? null;
    randomVisible.value = true;
  } catch {
    // 拦截器提示
  }
}

/** 加入错题本（不会/答错时） */
async function handleAddWrong(item: BaguEntry | null) {
  if (!item) return;
  wrongLoading.value = true;
  try {
    await addBaguWrong({
      questionId: item.id,
      category: item.category,
      content: item.content,
    });
    ElMessage.success('已加入错题本，可去「学习记录」查看');
  } catch {
    // 拦截器已提示
  } finally {
    wrongLoading.value = false;
  }
}

/** AI 深入讲解：复用对话接口（chatWithRag） */
async function handleExplain(item: BaguEntry | null) {
  if (!item) return;
  explainVisible.value = true;
  explainLoading.value = true;
  explainText.value = '';
  try {
    const res = await postChatRag({
      message: `请深入讲解以下计算机八股知识点，结合面试场景与追问展开，控制在 300 字内：\n${item.content}`,
    });
    explainText.value = res.data?.reply ?? 'AI 暂时无法回答，请稍后重试';
  } catch {
    explainText.value = 'AI 讲解失败，请检查登录状态后重试';
  } finally {
    explainLoading.value = false;
  }
}

/** 分类 → el-tag 颜色映射 */
function tagType(cat: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    后端: 'primary',
    前端: 'success',
    算法: 'warning',
    面试: 'danger',
    校招流程: 'info',
  };
  return map[cat] ?? 'info';
}
</script>

<style scoped>
.bagu-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
  position: relative;
  overflow: hidden;
  background: linear-gradient(165deg, #f6f8fb 0%, #eef2f7 50%, #e6ebf2 100%);
  color: var(--app-text);
}

.theme-dark .bagu-page {
  background: linear-gradient(165deg, #14171c 0%, #101318 50%, #0d1014 100%);
}

.bagu-page__bar {
  position: absolute;
  top: 20px;
  left: 24px;
  z-index: 5;
  display: flex;
  align-items: baseline;
  gap: 16px;
}

.bagu-page__back {
  font-size: 14px;
  color: var(--app-accent-blue);
  text-decoration: none;
}

.bagu-page__back:hover {
  text-decoration: underline;
}

.bagu-page__title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: var(--app-text);
}

.bagu-page__body {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 860px;
  margin-top: 56px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.bagu-filters {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  padding: 14px 16px;
}

.bagu-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.bagu-toolbar__search {
  flex: 1;
}

.bagu-toolbar__random {
  border-radius: 9999px;
  font-weight: 600;
  white-space: nowrap;
}

.bagu-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bagu-card {
  border-radius: var(--app-radius-lg);
  border: 1px solid var(--app-border);
}

.bagu-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.bagu-card__no {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.bagu-card__content {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--app-text);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.bagu-card__content--expanded {
  -webkit-line-clamp: unset;
  overflow: visible;
}

.bagu-card__actions {
  margin-top: 10px;
  display: flex;
  gap: 4px;
}

.bagu-page__pager {
  justify-content: center;
}

.bagu-random__content {
  margin: 12px 0 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--app-text);
}

.bagu-explain__loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--app-text-secondary);
  padding: 24px 0;
}

.bagu-explain__content {
  font-size: 14px;
  line-height: 1.7;
  color: var(--app-text);
}

.bagu-explain__content :deep(p) {
  margin: 6px 0;
}

.bagu-page__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.bagu-orb--1 {
  width: 360px;
  height: 360px;
  top: -100px;
  right: -80px;
}

.bagu-orb--2 {
  width: 300px;
  height: 300px;
  bottom: -60px;
  left: -80px;
  animation-delay: 2s;
}

@media (max-width: 767px) {
  .bagu-page {
    padding: 16px;
  }

  .bagu-page__body {
    margin-top: 72px;
  }

  .bagu-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
