import axios from 'axios'

const client = axios.create({
  baseURL: 'http://localhost:8080/api/conversation',
  timeout: 60000,
  headers: { 'Content-Type': 'application/json' },
})

client.interceptors.response.use(
  (res) => res.data,
  (error) => {
    const msg = error.response?.data?.message || error.message || '请求失败'
    return Promise.reject(new Error(msg))
  }
)

export interface ConversationSummary {
  id: string
  title: string
  messageCount: number
  lastMessagePreview: string
  createTime: string
  updateTime: string
}

export interface ChatMessageVO {
  id: string
  conversationId: string
  role: 'user' | 'assistant'
  content: string
  citations?: any[]
  sequence: number
  createTime: string
}

export interface SendMessageResult {
  conversationId: string
  answer: string
  citations: any[]
  sequence: number
}

export const conversationApi = {
  /** 新建空会话 */
  newConversation: (): Promise<{ code: number; data: { conversationId: string } }> =>
    client.post('/new'),

  /** 在某会话内发送消息 */
  sendMessage: (conversationId: string, content: string): Promise<{ code: number; data: SendMessageResult }> =>
    client.post(`/${conversationId}/message`, { content }),

  /** 获取用户会话列表 */
  list: (): Promise<{ code: number; data: ConversationSummary[] }> =>
    client.get('/list'),

  /** 回看某会话全部消息 */
  getMessages: (conversationId: string): Promise<{ code: number; data: ChatMessageVO[] }> =>
    client.get(`/${conversationId}/messages`),

  /** 重命名会话 */
  rename: (conversationId: string, title: string): Promise<{ code: number }> =>
    client.put(`/${conversationId}/title`, { title }),

  /** 删除会话 */
  remove: (conversationId: string): Promise<{ code: number }> =>
    client.delete(`/${conversationId}`),
}
