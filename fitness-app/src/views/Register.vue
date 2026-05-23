<template>
  <div class="page">
    <h2 class="title">注册</h2>
    <van-form @submit="handleRegister">
      <van-cell-group inset>
        <van-field
          v-model="username"
          name="username"
          label="用户名"
          placeholder="4-20 位字母数字"
          :rules="[
            { required: true, message: '请输入用户名' },
            { pattern: /^[a-zA-Z0-9_]{4,20}$/, message: '4-20 位字母数字或下划线' },
          ]"
        />
        <van-field
          v-model="password"
          type="password"
          name="password"
          label="密码"
          placeholder="6-20 位"
          :rules="[{ required: true, message: '请输入密码' }, { validator: (v) => v.length >= 6, message: '密码至少 6 位' }]"
        />
        <van-field
          v-model="confirmPassword"
          type="password"
          name="confirmPassword"
          label="确认密码"
          placeholder="再次输入密码"
          :rules="[{ required: true, message: '请确认密码' }, { validator: (v) => v === password, message: '两次密码不一致' }]"
        />
      </van-cell-group>
      <div style="margin: 16px">
        <van-button round block type="primary" native-type="submit" :loading="loading">
          注册
        </van-button>
      </div>
    </van-form>
    <div style="text-align: center">
      <van-button plain round block to="/login" @click="$router.push('/login')">
        返回登录
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { showToast } from 'vant';
import { register } from '@/api/user';
import { useUserStore } from '@/stores/userStore';

const router = useRouter();
const userStore = useUserStore();
const username = ref('');
const password = ref('');
const confirmPassword = ref('');
const loading = ref(false);

const handleRegister = async () => {
  loading.value = true;
  try {
    const res = await register({ username: username.value, password: password.value });
    if (res.code === 200) {
      userStore.setToken(res.data.token);
      showToast('注册成功');
      router.push('/workout');
    } else {
      showToast(res.message || '注册失败');
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
