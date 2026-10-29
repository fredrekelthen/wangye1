import axios from 'axios'
import { message } from 'ant-design-vue'

const emCodeApiClient = axios.create({
  baseURL: 'http://localhost:8080/api/multi-agent',
  timeout: 60000,
  headers: { 'Content-Type': 'application/json' },
})

emCodeApiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const errorMsg =
      error.response?.data?.error || error.response?.data?.message || error.message || '请求失败'
    message.error(errorMsg)
    return Promise.reject(error)
  }
)

export interface EmCodeResult {
  generated: boolean
  language?: string
  domain?: string
  description?: string
  dependencies?: string[]
  scriptName?: string
  code?: string
  executionHint?: string
  message?: string
}

export interface EmCodeResponse {
  success: boolean
  data: EmCodeResult
  executionTimeMs: number
}

export const emCodeApi = {
  generate: (requirement: string): Promise<EmCodeResponse> =>
    emCodeApiClient.post('/em-code', { requirement, userId: 'web-user' }),

  detect: (text: string): Promise<any> =>
    emCodeApiClient.post('/intent/detect', { text }),
}

export default emCodeApiClient
