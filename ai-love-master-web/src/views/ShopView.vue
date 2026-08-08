<template>
  <div class="shop-page">
    <div class="shop-page__bar">
      <router-link to="/user-center" class="shop-page__back">← 返回个人中心</router-link>
      <h1 class="shop-page__title">积分商城</h1>
      <span class="shop-page__points">我的积分 <b class="app-num">{{ points }}</b></span>
    </div>

    <!-- 商品列表（加载骨架） -->
    <div v-if="!loaded" class="shop-grid">
      <el-skeleton v-for="n in 4" :key="n" animated class="shop-card shop-card--sk">
        <template #template>
          <div class="shop-sk__head">
            <el-skeleton-item variant="text" style="width: 70px; height: 18px" />
            <el-skeleton-item variant="text" style="width: 44px; height: 22px" />
          </div>
          <el-skeleton-item variant="h3" style="width: 60%; height: 20px" />
          <el-skeleton-item variant="text" style="width: 100%; height: 14px" />
          <el-skeleton-item variant="text" style="width: 85%; height: 14px" />
        </template>
      </el-skeleton>
    </div>

    <!-- 商品列表 -->
    <div v-else class="shop-grid">
      <div v-for="item in items" :key="item.id" class="shop-card pixel-hover">
        <div class="shop-card__head">
          <span class="shop-card__type" :class="item.type === 'VIP_CARD' ? 'is-vip' : 'is-content'">
            {{ item.type === 'VIP_CARD' ? '👑 VIP 权益' : '📄 求职资料' }}
          </span>
          <span class="shop-card__points app-num">{{ item.points }}</span>
        </div>
        <h3 class="shop-card__name">{{ item.name }}</h3>
        <p class="shop-card__desc">{{ item.description }}</p>
        <div class="shop-card__actions">
          <el-button
            type="primary"
            round
            :disabled="points < item.points || redeemingId === item.id"
            :loading="redeemingId === item.id"
            @click="handleRedeem(item)"
          >
            {{ points < item.points ? `还差 ${item.points - points} 分` : '立即兑换' }}
          </el-button>
        </div>
      </div>
    </div>

    <p v-if="!items.length" class="shop-empty">暂无上架商品，敬请期待</p>

    <!-- 兑换记录 -->
    <div v-if="records.length" class="shop-records">
      <h3 class="shop-records__title">🛍️ 兑换记录</h3>
      <div v-for="r in records" :key="r.id" class="shop-record">
        <div class="shop-record__main">
          <span class="shop-record__name">{{ r.itemName }}</span>
          <span class="shop-record__time">{{ formatTime(r.createTime) }}</span>
        </div>
        <span class="shop-record__cost app-num">-{{ r.points }}</span>
        <el-button v-if="r.payload" size="small" link type="primary" @click="showPayload(r)">查看内容</el-button>
      </div>
    </div>

    <!-- 兑换成功 / 内容弹窗 -->
    <el-dialog v-model="showDialog" :title="dialogTitle" width="min(480px, 92vw)" align-center>
      <div class="shop-dialog__payload">{{ dialogPayload }}</div>
      <template #footer>
        <el-button type="primary" round @click="showDialog = false">知道了</el-button>
      </template>
    </el-dialog>

    <!-- 背景装饰 -->
    <div class="shop-page__bg" aria-hidden="true">
      <span class="app-orb app-orb--blue shop-orb shop-orb--1" />
      <span class="app-orb app-orb--orange shop-orb shop-orb--2" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getRedeemRecords, getShopItems, redeemItem, type RedeemRecord, type ShopItem } from '../api/shop';
import { getPoints } from '../api/user';

const items = ref<ShopItem[]>([]);
const records = ref<RedeemRecord[]>([]);
const points = ref(0);
const redeemingId = ref(0);
const loaded = ref(false);

const showDialog = ref(false);
const dialogTitle = ref('');
const dialogPayload = ref('');

onMounted(() => {
  loadItems();
  loadPoints();
});

async function loadItems() {
  try {
    const res = await getShopItems();
    items.value = res.data ?? [];
    loadRecords();
  } catch {
    // 拦截器已提示
  } finally {
    loaded.value = true;
  }
}

async function loadRecords() {
  try {
    const res = await getRedeemRecords();
    records.value = res.data ?? [];
  } catch {
    // 拦截器已提示
  }
}

async function loadPoints() {
  try {
    const res = await getPoints();
    points.value = res.data?.points ?? 0;
  } catch {
    // 拦截器已提示
  }
}

async function handleRedeem(item: ShopItem) {
  redeemingId.value = item.id;
  try {
    const res = await redeemItem(item.id);
    points.value = res.data?.pointsLeft ?? points.value;
    if (res.data?.type === 'VIP_CARD') {
      dialogTitle.value = '🎉 兑换成功：' + res.data.itemName;
      dialogPayload.value = res.data.payload + '\n\n剩余积分：' + res.data.pointsLeft;
    } else {
      dialogTitle.value = '📄 ' + res.data?.itemName;
      dialogPayload.value = res.data?.payload ?? '';
    }
    showDialog.value = true;
    loadRecords();
    ElMessage.success('兑换成功，已扣除 ' + res.data?.cost + ' 积分');
  } catch {
    // 拦截器已提示
  } finally {
    redeemingId.value = 0;
  }
}

function showPayload(r: RedeemRecord) {
  dialogTitle.value = '🛍️ ' + r.itemName;
  dialogPayload.value = r.payload;
  showDialog.value = true;
}

function formatTime(t?: string) {
  if (!t) return '';
  return t.replace('T', ' ').slice(0, 16);
}
</script>

<style scoped>
.shop-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: linear-gradient(180deg, #f8fbfe 0%, #f0f4fa 55%, #eaf0f8 100%);
  color: var(--app-text);
  padding: 0 var(--app-space-xl) 60px;
}

.theme-dark .shop-page {
  background: linear-gradient(180deg, #10141c 0%, #0d1118 55%, #0a0e14 100%);
}

.shop-page__bar {
  position: relative;
  z-index: 2;
  max-width: 860px;
  width: 100%;
  margin: 0 auto;
  padding: var(--app-space-lg) 0;
  display: flex;
  align-items: center;
  gap: var(--app-space-lg);
  flex-wrap: wrap;
}

.shop-page__back {
  font-size: 14px;
  color: var(--app-primary);
  text-decoration: none;
}

.shop-page__title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  flex: 1;
}

.shop-page__points {
  font-size: 13px;
  color: var(--app-text-secondary);
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 9999px;
  padding: 4px 14px;
  box-shadow: var(--app-shadow-sm);
}

.shop-page__points b {
  color: var(--app-primary);
}

.shop-grid {
  position: relative;
  z-index: 2;
  max-width: 860px;
  width: 100%;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: var(--app-space-lg);
  animation: app-fade-up 0.5s ease both;
}

.shop-card {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  box-shadow: var(--app-shadow-md);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: all 0.18s ease;
}

.shop-card--sk {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.shop-sk__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.shop-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.shop-card__type {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 9999px;
}

.shop-card__type.is-vip {
  color: #b45309;
  background: #fef3c7;
}

.shop-card__type.is-content {
  color: var(--app-primary);
  background: var(--app-primary-soft);
}

.shop-card__points {
  font-size: 20px;
  font-weight: 800;
  color: var(--app-primary);
}

.shop-card__points::after {
  content: ' 分';
  font-size: 12px;
  color: var(--app-text-secondary);
}

.shop-card__name {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.shop-card__desc {
  margin: 0;
  font-size: 13px;
  color: var(--app-text-secondary);
  line-height: 1.6;
  flex: 1;
}

.shop-card__actions {
  text-align: right;
}

.shop-empty {
  position: relative;
  z-index: 2;
  text-align: center;
  color: var(--app-text-secondary);
  padding: 60px 0;
}

.shop-records {
  position: relative;
  z-index: 2;
  max-width: 860px;
  width: 100%;
  margin: 32px auto 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.shop-records__title {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 700;
}

.shop-record {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-md);
  padding: 12px 16px;
  box-shadow: var(--app-shadow-sm);
}

.shop-record__main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.shop-record__name {
  font-size: 14px;
  font-weight: 600;
}

.shop-record__time {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.shop-record__cost {
  font-size: 16px;
  font-weight: 800;
  color: #ef4444;
}

.shop-dialog__payload {
  white-space: pre-wrap;
  font-size: 14px;
  line-height: 1.8;
  color: var(--app-text);
}

.shop-page__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.shop-orb--1 {
  width: 380px;
  height: 380px;
  top: -120px;
  right: -100px;
}

.shop-orb--2 {
  width: 320px;
  height: 320px;
  bottom: -80px;
  left: -100px;
  animation-delay: 2s;
}

@media (max-width: 767px) {
  .shop-page {
    padding: 0 var(--app-space-md) 40px;
  }
}
</style>
