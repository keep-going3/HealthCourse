<template>
  <div id="app-container">
    <router-view />

    <!-- 底部导航：仅在登录后显示 -->
    <van-tabbar v-if="userStore.isLoggedIn" route active-color="#1989fa" placeholder>
      <van-tabbar-item icon="records" to="/workout">训练</van-tabbar-item>
      <van-tabbar-item icon="calendar-o" to="/history">历史</van-tabbar-item>
      <van-tabbar-item icon="chart-trending-o" to="/body">身体</van-tabbar-item>
      <van-tabbar-item icon="chat-o" to="/ai">AI</van-tabbar-item>
      <van-tabbar-item icon="contact" to="/profile">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { useUserStore } from '@/stores/userStore';

const userStore = useUserStore();
</script>

<style>
#app-container { padding-bottom: 50px; }
</style>
