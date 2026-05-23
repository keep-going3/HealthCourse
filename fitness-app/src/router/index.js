import { createRouter, createWebHashHistory } from 'vue-router';

const routes = [
  {
    path: '/login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/register',
    component: () => import('@/views/Register.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    redirect: '/workout',
  },
  {
    path: '/workout',
    component: () => import('@/views/WorkoutRecord.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/history',
    component: () => import('@/views/WorkoutHistory.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/body',
    component: () => import('@/views/BodyData.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/ai',
    component: () => import('@/views/AIAssistant.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/profile',
    component: () => import('@/views/Profile.vue'),
    meta: { requiresAuth: true },
  },
];

const router = createRouter({
  history: createWebHashHistory(),
  routes,
});

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token');
  if (to.meta.requiresAuth && !token) {
    next('/login');
  } else if (to.path === '/login' && token) {
    next('/');
  } else {
    next();
  }
});

export default router;
