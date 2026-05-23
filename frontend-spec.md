# 健身训练记录 App - 前端开发最终版

## 一、技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | ^3.4 | 核心框架 |
| Vant 4 | ^4.9 | UI 组件库 |
| Pinia | ^2.1 | 状态管理 |
| Vue Router | ^4.2 | 路由跳转 |
| Axios | ^1.6 | HTTP 请求 |
| Vite | ^5.0 | 构建工具 |

## 二、项目结构

```
src/
├── api/
│   ├── request.js      # axios 实例 + 拦截器
│   ├── user.js         # 用户接口
│   ├── workout.js      # 训练接口
│   ├── body.js         # 身体数据接口
│   ├── agent.js        # AI 接口（含 SSE）
│   └── feedback.js     # 反馈接口
├── stores/
│   └── userStore.js    # 用户状态
├── views/
│   ├── Login.vue
│   ├── Register.vue
│   ├── WorkoutRecord.vue
│   ├── WorkoutHistory.vue
│   ├── AIAssistant.vue
│   ├── BodyData.vue
│   └── Profile.vue
├── router/
│   └── index.js
└── App.vue
```

## 三、接口总览

### 3.1 用户模块

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /user/register | 注册 | ✗ |
| POST | /user/login | 登录 | ✗ |
| POST | /user/refresh-token | 刷新 Token | ✓ |
| PUT | /user/password | 修改密码 | ✓ |
| POST | /user/logout | 退出 | ✓ |

### 3.2 训练记录模块

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /exercise/library | 动作库列表 | ✓ |
| GET | /exercise/library/group | 按肌群分组 | ✓ |
| POST | /exercise/custom | 添加自定义动作 | ✓ |
| PUT | /exercise/custom/{id} | 编辑自定义动作 | ✓ |
| DELETE | /exercise/custom/{id} | 删除自定义动作 | ✓ |
| POST | /workout/save | 新增训练 | ✓ |
| PUT | /workout/update | 覆盖当天训练 | ✓ |
| GET | /workout/detail?date= | 训练详情 | ✓ |
| GET | /workout/calendar?year=&month= | 训练日历 | ✓ |
| DELETE | /workout/{sessionId} | 删除训练 | ✓ |

### 3.3 身体数据模块

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /body/info | 获取身体信息 | ✓ |
| PUT | /body/info | 更新身体信息 | ✓ |
| POST | /body/weight | 记录体重 | ✓ |
| GET | /body/weight/list?months= | 体重列表 | ✓ |
| PUT | /body/weight/{recordId} | 修改体重记录 | ✓ |
| DELETE | /body/weight/{recordId} | 删除体重记录 | ✓ |

### 3.4 AI 助手模块

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /agent/chat/stream | 流式对话 (SSE) | ✓ |
| POST | /agent/parse-workout | 自然语言解析 | ✓ |

### 3.5 反馈模块

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /feedback | 提交反馈 | ✓ |

## 四、统一规范

### 4.1 基础信息

```javascript
BaseURL: 'http://localhost:8080/api'
数据格式: JSON
字符编码: UTF-8
时区: 北京时间 yyyy-MM-dd
认证方式: Authorization: Bearer <token>
```

### 4.2 统一响应格式

```javascript
// 成功
{ "code": 200, "message": "success", "data": {} }

// 错误码
200 成功 | 400 参数错误 | 401 未登录 | 403 无权限
404 不存在 | 409 冲突 | 500 服务器错误
```

### 4.3 Token 刷新机制

```
流程：请求 → 收到401 → 调用刷新接口 → 重试原请求 → 失败则跳登录

刷新接口响应：
{ "code": 200, "data": { "token": "新token", "expiresIn": 604800 } }
```

### 4.4 SSE 事件格式

```
data: {"type":"start","messageId":"xxx"}     // 开始
data: {"type":"chunk","content":"文本"}       // 内容片段
data: {"type":"error","message":"错误信息"}   // 错误
data: {"type":"end","messageId":"xxx"}       // 结束
```

## 五、核心代码示例

### 5.1 Axios 封装（request.js）

```javascript
import axios from 'axios';
import router from '@/router';

const request = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000,
});

// 刷新 Token 状态管理
let __isRefreshing = false;
let __refreshSubscribers = [];

const onRefreshed = (newToken) => {
  __refreshSubscribers.forEach((cb) => cb(newToken));
  __refreshSubscribers = [];
};

const refreshTokenRequest = async () => {
  const res = await axios.post('http://localhost:8080/api/user/refresh-token', {}, {
    headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
  });
  const newToken = res.data.data.token;
  localStorage.setItem('token', newToken);
  return newToken;
};

// 请求拦截器：自动添加 token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 响应拦截器：401 处理（防无限循环 + 并发等待）
request.interceptors.response.use(
  (res) => res.data,
  async (error) => {
    const { config } = error;
    // 不是 401 或已是重试请求，直接拒绝
    if (error.response?.status !== 401 || config.__isRetry) {
      return Promise.reject(error);
    }

    // 正在刷新中，将请求加入等待队列
    if (__isRefreshing) {
      return new Promise((resolve) => {
        __refreshSubscribers.push((newToken) => {
          config.headers.Authorization = `Bearer ${newToken}`;
          config.__isRetry = true;
          resolve(request(config));
        });
      });
    }

    __isRefreshing = true;
    config.__isRetry = true;

    try {
      const newToken = await refreshTokenRequest();
      onRefreshed(newToken);
      config.headers.Authorization = `Bearer ${newToken}`;
      return request(config);
    } catch {
      // 刷新失败，跳登录
      __refreshSubscribers = [];
      localStorage.removeItem('token');
      router.push('/login');
      return Promise.reject(error);
    } finally {
      __isRefreshing = false;
    }
  }
);

export default request;
```

### 5.2 Token 刷新逻辑

```javascript
// stores/userStore.js
import { ref } from 'vue';
import { defineStore } from 'pinia';

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '');

  const setToken = (newToken) => {
    token.value = newToken;
    localStorage.setItem('token', newToken);
  };

  const clearToken = () => {
    token.value = '';
    localStorage.removeItem('token');
  };

  return { token, setToken, clearToken };
});
```

### 5.3 登录页（Login.vue）

```vue
<template>
  <div class="login">
    <van-field v-model="username" label="用户名" placeholder="4-20位" />
    <van-field v-model="password" type="password" label="密码" placeholder="6-20位" />
    <van-button type="primary" block :loading="loading" @click="handleLogin">登录</van-button>
    <van-button plain block @click="$router.push('/register')">注册</van-button>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { showToast } from 'vant';
import { login } from '@/api/user';
import { useUserStore } from '@/stores/userStore';

const router = useRouter();
const userStore = useUserStore();
const username = ref('');
const password = ref('');

const loading = ref(false);

const handleLogin = async () => {
  if (!username.value || !password.value) {
    showToast('请填写完整');
    return;
  }
  if (loading.value) return;
  loading.value = true;
  try {
    const res = await login({ username: username.value, password: password.value });
    if (res.code === 200) {
      userStore.setToken(res.data.token);
      showToast('登录成功');
      router.push('/');
    }
  } catch {
    showToast('登录失败，请检查网络或用户名密码');
  } finally {
    loading.value = false;
  }
};
</script>
```

### 5.4 保存训练记录（关键：新增/覆盖）

```javascript
// api/workout.js
import request from './request';

// 新增
export const saveWorkout = (data) => request.post('/workout/save', data);

// 覆盖当天
export const updateWorkout = (data) => request.put('/workout/update', data);
```

```vue
<!-- 组件中使用 -->
<script setup>
import { saveWorkout, updateWorkout } from '@/api/workout';
import { showConfirmDialog, showToast } from 'vant';

const submitWorkout = async (formData) => {
  try {
    await saveWorkout(formData);
    showToast('保存成功');
  } catch (err) {
    if (err.response?.data?.code === 409) {
      // 当天已有记录，询问是否覆盖
      const confirm = await showConfirmDialog({
        title: '提示',
        message: '当天已有训练记录，是否覆盖？',
      });
      if (confirm) {
        await updateWorkout(formData);
        showToast('覆盖成功');
      }
    }
  }
};
</script>
```

### 5.5 SSE 流式对话（AIAssistant.vue）

```vue
<template>
  <div class="ai-chat">
    <div class="messages">
      <div v-for="msg in messages" :key="msg.id">
        <div v-if="msg.role === 'user'">我：{{ msg.content }}</div>
        <div v-else>AI：{{ msg.content }}</div>
      </div>
      <div v-if="isStreaming"><span>AI：{{ currentChunk }}▊</span></div>
    </div>
    <van-field v-model="inputMsg" placeholder="问AI..." />
    <van-button type="primary" @click="sendMessage">发送</van-button>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { showToast } from 'vant';
import { useUserStore } from '@/stores/userStore';

const userStore = useUserStore();
const messages = ref([]);
const inputMsg = ref('');
const isStreaming = ref(false);
const currentChunk = ref('');

const sendMessage = async () => {
  if (!inputMsg.value.trim()) return;

  // 添加用户消息
  messages.value.push({ role: 'user', content: inputMsg.value, id: Date.now() });
  const userMsg = inputMsg.value;
  inputMsg.value = '';

  // 准备 AI 消息占位
  const aiMsgId = Date.now() + 1;
  messages.value.push({ role: 'assistant', content: '', id: aiMsgId });
  currentChunk.value = '';
  isStreaming.value = true;

  const response = await fetch('http://localhost:8080/api/agent/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${userStore.token}`,
    },
    body: JSON.stringify({ message: userMsg }),
  });

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n');
    buffer = lines.pop();

    for (const line of lines) {
      if (line.startsWith('data: ')) {
        const data = JSON.parse(line.slice(6));
        if (data.type === 'chunk') {
          currentChunk.value += data.content;
          messages.value[messages.value.length - 1].content = currentChunk.value;
        } else if (data.type === 'ping') {
          // 心跳保持，不做处理
        } else if (data.type === 'error') {
          showToast(data.message || 'AI 服务异常');
          isStreaming.value = false;
          return;
        } else if (data.type === 'end') {
          isStreaming.value = false;
        }
      }
    }
  }
};
</script>
```

### 5.6 路由守卫（router/index.js）

```javascript
import { createRouter, createWebHistory } from 'vue-router';

const routes = [
  { path: '/login', component: () => import('@/views/Login.vue') },
  { path: '/register', component: () => import('@/views/Register.vue') },
  { path: '/', component: () => import('@/views/WorkoutRecord.vue'), meta: { requiresAuth: true } },
  // ... 其他需要登录的路由
];

const router = createRouter({ history: createWebHistory(), routes });

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token');
  if (to.meta.requiresAuth && !token) {
    next('/login');
  } else {
    next();
  }
});

export default router;
```

## 六、全局约定速查表

| 项目 | 约定 |
|------|------|
| Token 存储 | localStorage |
| Token 刷新 | 收到 401 自动调用 /user/refresh-token，重试原请求 |
| 日期格式 | yyyy-MM-dd（北京时间） |
| 训练覆盖 | 当天有记录时用 PUT /workout/update |
| SSE 重连 | 断连后前端重新请求，消息内 messageId 用于后续业务去重 |
| 缓存策略 | 响应头带 ETag，前端可用 If-None-Match |
| 退出登录 | 清除本地 token，跳登录页 |
