import { createApp } from 'vue';
import { createPinia } from 'pinia';
import 'element-plus/dist/index.css';

import App from './App.vue';
import router from './router';
import { setUnauthorizedHandler } from './api/http';
import { useAuthStore } from './store/authStore';
import { initErrorMonitor } from './utils/errorMonitor';
import './styles/global.css';
import './styles/chat-layout.css';
import './styles/pixel.css';

const app = createApp(App);
const pinia = createPinia();
app.use(pinia);
app.use(router);

// 前端错误监控：全局捕获 error / unhandledrejection 自动上报
initErrorMonitor();

setUnauthorizedHandler(() => {
  useAuthStore(pinia).logout();
});

app.mount('#app');
