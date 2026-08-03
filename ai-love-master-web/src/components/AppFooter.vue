<template>
  <footer class="app-footer">
    <div class="app-footer__inner">
      <div class="app-footer__copyright">
        © {{ year }} AI 应用中心 版权所有
      </div>
      <div class="app-footer__links">
        <router-link to="/feedback" class="app-footer__link app-footer__link--clickable">意见反馈</router-link>
        <span class="app-footer__link app-footer__link--clickable" @click="showUserAgreement">用户协议</span>
        <span class="app-footer__link app-footer__link--clickable" @click="showDisclaimer">隐私政策</span>
      </div>
      <span class="app-footer__link app-footer__link--clickable" @click="showWechat">联系我们</span>
      <span class="app-footer__author">作者：richard谭</span>
    </div>
    <el-dialog
      v-model="showWechatDialog"
      title="联系我们"
      width="320px"
      :show-close="true"
    >
      <p class="app-footer__wechat-text">微信号：<strong>trc030228</strong></p>
      <el-button type="primary" @click="copyWechatId">复制微信号</el-button>
    </el-dialog>
    <el-dialog
      v-model="showUserAgreementDialog"
      title="用户协议"
      width="320px"
      :show-close="true"
    >
      <p class="app-footer__wechat-text">trc牛逼</p>
    </el-dialog>
    <el-dialog
      v-model="showDisclaimerDialog"
      title="隐私政策"
      width="360px"
      :show-close="true"
    >
      <p class="app-footer__disclaimer">内容为AI生成仅供参考</p>
    </el-dialog>
  </footer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';

const year = computed(() => new Date().getFullYear());
const showWechatDialog = ref(false);
const showDisclaimerDialog = ref(false);
const showUserAgreementDialog = ref(false);
const WECHAT_ID = 'trc030228';

function showUserAgreement() {
  showUserAgreementDialog.value = true;
}

function showDisclaimer() {
  showDisclaimerDialog.value = true;
}

function showWechat() {
  showWechatDialog.value = true;
}

async function copyWechatId() {
  try {
    await navigator.clipboard.writeText(WECHAT_ID);
    ElMessage.success('微信号已复制到剪贴板');
  } catch {
    ElMessage.warning('复制失败，请手动复制：' + WECHAT_ID);
  }
}
</script>

<style scoped>
.app-footer {
  padding: var(--app-space-md) var(--app-space-lg);
  border-top: 1px solid var(--app-border);
  background: var(--app-card);
}

.app-footer__inner {
  max-width: var(--app-content-max, 720px);
  margin: 0 auto;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: var(--app-space-md);
}

.app-footer__copyright {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  opacity: 0.9;
}

.app-footer__links {
  display: flex;
  gap: var(--app-space-lg);
}

.app-footer__link {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-decoration: none;
  opacity: 0.9;
}

.app-footer__link:hover {
  color: var(--el-color-primary);
}

.app-footer__link--clickable {
  cursor: pointer;
}

.app-footer__author {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  opacity: 0.9;
}

@media (max-width: 767px) {
  .app-footer {
    padding: var(--app-space-md);
  }

  .app-footer__inner {
    flex-direction: column;
    gap: var(--app-space-sm);
  }
}
</style>
