import axios from 'axios';
import { ElMessage } from 'element-plus';
import router from '../router';

const http = axios.create({
  // 本地 dev 由 .env.development 指定；生产部署留空走同域（Netlify _redirects 代理 /api）
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 20000,
});

let onUnauthorized: (() => void) | null = null;

/** 注册 401 回调，由 main.ts 设置，避免循环依赖 */
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler;
}

http.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('love_master_token');
    if (token) {
      config.headers = config.headers ?? {};
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

http.interceptors.response.use(
  (resp) => resp.data,
  (error) => {
    const status = error?.response?.status;
    const url = String(error?.config?.url ?? '');
    const isAuthEndpoint = /api\/auth\/(login|register)/.test(url);

    // 登录/注册接口失败（400/401）：用户名或密码错误
    if (isAuthEndpoint && (status === 400 || status === 401)) {
      const backendMsg = error?.response?.data?.message ?? error?.response?.data?.msg;
      ElMessage.error(backendMsg || '用户名或密码错误');
      return Promise.reject(error);
    }

    // 其他接口 401：登录过期
    if (status === 401) {
      onUnauthorized?.();
      router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } });
      ElMessage.warning('登录已过期，请重新登录');
      return Promise.reject(error);
    }

    const msg =
      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      error?.message ||
      '请求失败，请稍后重试';
    ElMessage.error(msg);
    return Promise.reject(error);
  },
);

export default http;

