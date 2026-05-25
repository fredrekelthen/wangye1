import axios from 'axios';
import { message } from 'ant-design-vue';

const apiClient = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('agent_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

apiClient.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    const errorMsg = error.response?.data?.message || '请求失败，请重试';
    message.error(errorMsg);
    return Promise.reject(error);
  }
);

export const agentApi = {
  // 查询项目结构
  getProjectStructure: () => apiClient.get('/agent/project/structure'),

  // 获取路由信息
  getRoutes: () => apiClient.get('/agent/routes'),

  // 获取组件列表
  getComponents: () => apiClient.get('/agent/components'),

  // 分析组件
  analyzeComponent: (componentPath: string) => 
    apiClient.post('/agent/component/analyze', { path: componentPath }),

  // 查询数据库
  queryDatabase: (query: string) => 
    apiClient.post('/agent/database/query', { query }),

  // 获取数据库表结构
  getDatabaseSchema: () => apiClient.get('/agent/database/schema'),

  // 获取集合列表
  getCollections: () => apiClient.get('/agent/database/collections'),

  // AI对话
  chat: (prompt: string, context?: any) => 
    apiClient.post('/agent/chat', { prompt, context }),

  // 导航到页面
  navigateToPage: (pagePath: string) => 
    apiClient.post('/agent/navigate', { page: pagePath }),

  // 打开组件
  openComponent: (componentPath: string) => 
    apiClient.post('/agent/component/open', { path: componentPath }),
};

export default apiClient;
