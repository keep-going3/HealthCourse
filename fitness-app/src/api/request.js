import axios from 'axios';
import { showToast } from 'vant';

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
});

let __isRefreshing = false;
let __refreshSubscribers = [];

const onRefreshed = (newToken) => {
  __refreshSubscribers.forEach((cb) => cb(newToken));
  __refreshSubscribers = [];
};

const refreshTokenRequest = async () => {
  const res = await axios.post(
    '/api/user/refresh-token',
    {},
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } }
  );
  const newToken = res.data.data.token;
  localStorage.setItem('token', newToken);
  return newToken;
};

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

request.interceptors.response.use(
  (res) => res.data,
  async (error) => {
    const { config, response } = error;
    if (!response || response.status !== 401 || config.__isRetry) {
      return Promise.reject(error);
    }

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
      __refreshSubscribers = [];
      localStorage.removeItem('token');
      showToast('登录已过期，请重新登录');
      window.location.hash = '#/login';
      return Promise.reject(error);
    } finally {
      __isRefreshing = false;
    }
  }
);

export default request;
