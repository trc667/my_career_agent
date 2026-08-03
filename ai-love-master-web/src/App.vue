<template>
  <div class="app-root" :class="`theme-${theme}`">
    <main class="app-main">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    <AppFooter v-if="showFooter" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useLoveMasterStore } from './store/loveMasterStore';
import AppFooter from './components/AppFooter.vue';

const store = useLoveMasterStore();
const { theme } = storeToRefs(store);
const route = useRoute();

/** 聊天页隐藏底部版权，避免遮挡输入区和发送按钮 */
const showFooter = computed(() => {
  const name = route.name as string;
  return name !== 'career-master' && name !== 'super-agent';
});
</script>

<style scoped>
.app-root {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.app-main {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
</style>
