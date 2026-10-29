import axios, { AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import { message } from 'ant-design-vue';

const myAxios: AxiosInstance = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000, // 建议适当延长超时时间
  withCredentials: true,
});

// 请求拦截器 - 添加 JWT token 认证信息
myAxios.interceptors.request.use(
  function (config: InternalAxiosRequestConfig) {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
);

// 响应拦截器 - 统一错误处理
myAxios.interceptors.response.use(
  function(response) {
    const { data } = response;
    // 未登录（后端业务码 40100/40101）统一跳转登录，并携带回跳地址
    if (data && (data.code === 40100 || data.code === 40101)) {
      if (
        !response.request.responseURL.includes('user/current') &&
        !window.location.href.includes('/user/login')
      ) {
        window.location.href = `/user/login?redirect=${encodeURIComponent(window.location.href)}`;
      }
    } else if (data && typeof data.code === 'number' && data.code !== 0) {
      // 其它业务错误统一 toast 提示（避免静默失败）
      message.error(data.description || data.message || '操作失败');
    }
    return response;
  },

  function(error) {
    // 网络层 HTTP 401 同样按未登录处理，其余错误保留给调用方处理
    if (error && error.response && error.response.status === 401) {
      if (!window.location.href.includes('/user/login')) {
        window.location.href = `/user/login?redirect=${encodeURIComponent(window.location.href)}`;
      }
    } else if (error && error.response) {
      const body = error.response.data;
      message.error((body && (body.description || body.message)) || '请求失败，请稍后重试');
    } else {
      message.error('网络异常，请检查连接后重试');
    }
    return Promise.reject(error);
  }
);

export default myAxios;