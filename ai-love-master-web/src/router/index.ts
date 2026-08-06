import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../store/authStore';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: () => import('../views/HomePage.vue'), meta: { title: '应用中心' } },
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { title: '登录' } },
    { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue'), meta: { title: '注册' } },
    { path: '/career-master', name: 'career-master', component: () => import('../views/LoveMasterView.vue'), meta: { title: 'AI 职规大师', requiresAuth: true } },
    { path: '/resume-review', name: 'resume-review', component: () => import('../views/ResumeReviewView.vue'), meta: { title: 'AI 简历评分', requiresAuth: true } },
    { path: '/super-agent', name: 'super-agent', component: () => import('../views/SuperAgentView.vue'), meta: { title: 'AI 超级智能体', requiresAuth: true } },
    { path: '/feedback', name: 'feedback', component: () => import('../views/FeedbackView.vue'), meta: { title: '意见反馈', requiresAuth: true } },
    { path: '/user-center', name: 'user-center', component: () => import('../views/UserCenterView.vue'), meta: { title: '个人中心', requiresAuth: true } },
    { path: '/navigate', name: 'navigate', component: () => import('../views/NavigateView.vue'), meta: { title: '网站导航' } },
    { path: '/agreement', name: 'agreement', component: () => import('../views/AgreementView.vue'), meta: { title: '用户协议' } },
    { path: '/notice', name: 'notice', component: () => import('../views/NoticeView.vue'), meta: { title: '公告中心' } },
    { path: '/admin', name: 'admin', component: () => import('../views/AdminView.vue'), meta: { title: '管理后台', requiresAuth: true, requiresAdmin: true } },
    { path: '/bagu', name: 'bagu', component: () => import('../views/BaguView.vue'), meta: { title: 'AI 八股练习场', requiresAuth: true } },
  ],
});

router.beforeEach((to, _from, next) => {
  const auth = useAuthStore();
  const needAuth = to.matched.some((r) => r.meta?.requiresAuth);
  const needAdmin = to.matched.some((r) => r.meta?.requiresAdmin);
  const isAuthPage = to.name === 'login' || to.name === 'register';
  if (needAuth && !auth.isAuthenticated()) {
    next({ name: 'login', query: { redirect: to.fullPath } });
  } else if (needAdmin && !auth.isAdmin()) {
    next({ name: 'home' });
  } else if (isAuthPage && auth.isAuthenticated()) {
    next({ name: 'home' });
  } else {
    next();
  }
});

router.afterEach((to) => {
  document.title = (to.meta.title as string) || 'AI 应用';
});

export default router;
