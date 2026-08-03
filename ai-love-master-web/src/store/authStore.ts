import { defineStore } from 'pinia';
import { ref } from 'vue';
import { login as apiLogin, register as apiRegister } from '../api/auth';
import type { LoginRequest, RegisterRequest } from '../api/auth';

const TOKEN_KEY = 'love_master_token';
const USERNAME_KEY = 'love_master_username';
const ROLE_KEY = 'love_master_role';

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY));
  const username = ref<string | null>(localStorage.getItem(USERNAME_KEY));
  const role = ref<string | null>(localStorage.getItem(ROLE_KEY));

  const isAuthenticated = () => !!token.value;
  const isAdmin = () => role.value === 'ADMIN';

  function setAuth(t: string, name?: string, r?: string) {
    token.value = t;
    username.value = name ?? null;
    role.value = r ?? null;
    localStorage.setItem(TOKEN_KEY, t);
    if (name) localStorage.setItem(USERNAME_KEY, name);
    if (r) localStorage.setItem(ROLE_KEY, r);
  }

  function clearAuth() {
    token.value = null;
    username.value = null;
    role.value = null;
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USERNAME_KEY);
    localStorage.removeItem(ROLE_KEY);
  }

  async function login(data: LoginRequest) {
    const res = await apiLogin(data);
    const dataRes = res as unknown as {
      code?: number;
      message?: string;
      data?: Record<string, unknown> | null;
      token?: string;
      accessToken?: string;
    };
    // 先判断业务失败：code !== 200 时显示后端 message（如「用户名或密码错误」）
    if (dataRes?.code !== 200 && dataRes?.code !== 0) {
      throw new Error(dataRes?.message ?? '登录失败');
    }
    const payload = dataRes?.data as Record<string, unknown> | undefined;
    const tokenStr = (
      payload?.token ??
      payload?.accessToken ??
      payload?.access_token ??
      dataRes?.token ??
      dataRes?.accessToken
    ) as string | undefined;
    if (!tokenStr) throw new Error('登录失败：未返回 token');
    const roleStr = (payload?.role as string | undefined) ?? (dataRes as { role?: string }).role;
    setAuth(tokenStr, (payload?.username as string) ?? data.username, roleStr);
    return dataRes;
  }

  async function register(data: RegisterRequest) {
    await apiRegister(data);
    return login({ username: data.username, password: data.password });
  }

  function logout() {
    clearAuth();
  }

  return {
    token,
    username,
    role,
    isAuthenticated,
    isAdmin,
    login,
    register,
    logout,
  };
});
