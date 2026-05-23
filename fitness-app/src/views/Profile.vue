<template>
  <div class="page">
    <van-nav-bar title="个人中心" />

    <van-cell-group inset style="margin-top: 12px">
      <van-cell title="修改密码" is-link @click="showPasswordDialog = true" />
      <van-cell title="提交反馈" is-link @click="showFeedbackDialog = true" />
    </van-cell-group>

    <div style="margin: 40px 16px">
      <van-button round block type="danger" @click="handleLogout">退出登录</van-button>
    </div>

    <!-- 修改密码弹窗 -->
    <van-dialog v-model:show="showPasswordDialog" title="修改密码" show-cancel-button
      @confirm="handleChangePassword" :confirm-button-loading="passwordLoading">
      <van-field v-model="passwordForm.oldPassword" type="password" label="旧密码"
        placeholder="输入旧密码" />
      <van-field v-model="passwordForm.newPassword" type="password" label="新密码"
        placeholder="6-20 位" :rules="[{ validator: (v) => !v || v.length >= 6, message: '至少 6 位' }]" />
    </van-dialog>

    <!-- 反馈弹窗 -->
    <van-dialog v-model:show="showFeedbackDialog" title="提交反馈" show-cancel-button
      @confirm="handleSubmitFeedback" :confirm-button-loading="feedbackLoading">
      <van-field v-model="feedbackContent" type="textarea" rows="4"
        placeholder="请描述你的问题或建议..." maxlength="500" show-word-limit />
    </van-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { showConfirmDialog, showToast } from 'vant';
import { useRouter } from 'vue-router';
import { logout as logoutApi } from '@/api/user';
import { submitFeedback } from '@/api/feedback';
import { useUserStore } from '@/stores/userStore';
import request from '@/api/request';

const router = useRouter();
const userStore = useUserStore();

// 退出登录
const handleLogout = async () => {
  try {
    await showConfirmDialog({ title: '提示', message: '确定退出登录？' });
    await logoutApi().catch(() => {});
    userStore.clearToken();
    showToast('已退出');
    router.push('/login');
  } catch { /* 取消 */ }
};

// 修改密码
const showPasswordDialog = ref(false);
const passwordLoading = ref(false);
const passwordForm = reactive({ oldPassword: '', newPassword: '' });

const handleChangePassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    showToast('请填写完整');
    return false;
  }
  if (passwordForm.newPassword.length < 6) {
    showToast('新密码至少 6 位');
    return false;
  }
  passwordLoading.value = true;
  try {
    const res = await request.put('/user/password', passwordForm);
    if (res.code === 200) {
      showToast('修改成功');
      passwordForm.oldPassword = '';
      passwordForm.newPassword = '';
      return true;
    }
    showToast(res.message || '修改失败');
    return false;
  } catch (err) {
    showToast(err.response?.data?.message || '修改失败');
    return false;
  } finally {
    passwordLoading.value = false;
  }
};

// 反馈
const showFeedbackDialog = ref(false);
const feedbackLoading = ref(false);
const feedbackContent = ref('');

const handleSubmitFeedback = async () => {
  if (!feedbackContent.value.trim()) {
    showToast('请输入反馈内容');
    return false;
  }
  feedbackLoading.value = true;
  try {
    const res = await submitFeedback(feedbackContent.value);
    if (res.code === 200) {
      showToast('感谢你的反馈！');
      feedbackContent.value = '';
      return true;
    }
    showToast(res.message || '提交失败');
    return false;
  } catch {
    showToast('提交失败');
    return false;
  } finally {
    feedbackLoading.value = false;
  }
};
</script>
