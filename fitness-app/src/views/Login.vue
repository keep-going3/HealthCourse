<template>
  <div class="page">
    <h2 class="title">登录</h2>
    <van-form @submit="handleLogin">
      <van-cell-group inset>
        <van-field
          v-model="username"
          name="username"
          label="用户名"
          placeholder="4-20 位"
          :rules="[{ required: true, message: '请输入用户名' }]"
        />
        <van-field
          v-model="password"
          type="password"
          name="password"
          label="密码"
          placeholder="6-20 位"
          :rules="[{ required: true, message: '请输入密码' }]"
        />
      </van-cell-group>
      <div style="margin: 16px">
        <van-button round block type="primary" native-type="submit" :loading="loading">
          登录
        </van-button>
      </div>
    </van-form>
    <div style="text-align: center">
      <van-button plain block round to="/register" @click="$router.push('/register')">
        注册
      </van-button>
    </div>
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
  loading.value = true;
  try {
    const res = await login({ username: username.value, password: password.value });
    if (res.code === 200) {
      userStore.setToken(res.data.token);
      showToast('登录成功');
      router.push('/workout');
    } else {
      showToast(res.message || '登录失败');
    }
  } catch (err) {
    showToast(err.response?.data?.message || '网络错误，请重试');
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.title { text-align: center; padding: 40px 0 24px; font-size: 24px; }
</style>
