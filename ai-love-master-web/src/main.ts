import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';

import App from './App.vue';
import router from './router';
import { setUnauthorizedHandler } from './api/http';
import { useAuthStore } from './store/authStore';
import './styles/global.css';
import './styles/chat-layout.css';
import './styles/pixel.css';
// 像素字体（Press Start 2P，拉丁/数字）
import '@fontsource/press-start-2p/400.css';

const app = createApp(App);
const pinia = createPinia();
app.use(pinia);
app.use(router);
app.use(ElementPlus);

pinia.state.value = pinia.state.value ?? {};
setUnauthorizedHandler(() => {
  useAuthStore(pinia).logout();
});

app.mount('#app');
