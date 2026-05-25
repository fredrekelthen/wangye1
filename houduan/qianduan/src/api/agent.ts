import axios from 'axios'
import { message } from 'ant-design-vue'

const agentApiClient = axios.create({
  baseURL: 'http://localhost:8081/api/agent',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

agentApiClient.interceptors.request.use(
  (config) => {
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

agentApiClient.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    const errorMsg = error.response?.data?.message || error.message || '请求失败'
    if (!errorMsg.includes('API Key')) {
      message.error(errorMsg)
    }
    return Promise.reject(error)
  }
)

export interface AgentChatRequest {
  prompt: string
  context?: any
}

export const agentApi = {
  chat: (prompt: string, context?: any) => 
    agentApiClient.post('/chat', { prompt, context }),
  
  getProjectStructure: () => 
    agentApiClient.get('/project/structure'),
  
  getRoutes: () => 
    agentApiClient.get('/routes'),
  
  getDatabaseSchema: () => 
    agentApiClient.get('/database/schema'),
  
  queryDatabase: (query: string) => 
    agentApiClient.post('/database/query', { query }),
  
  navigateToPage: (page: string) => 
    agentApiClient.post('/navigate', { page }),
}

export default agentApiClient
