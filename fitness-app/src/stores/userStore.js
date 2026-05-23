import { ref, computed } from 'vue';
import { defineStore } from 'pinia';

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '');

  const isLoggedIn = computed(() => !!token.value);

  const setToken = (newToken) => {
    token.value = newToken;
    localStorage.setItem('token', newToken);
  };

  const clearToken = () => {
    token.value = '';
    localStorage.removeItem('token');
  };

  return { token, isLoggedIn, setToken, clearToken };
});
