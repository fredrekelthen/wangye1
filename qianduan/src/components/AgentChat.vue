<template>
  <a-modal
    v-model:open="modalVisible"
    title="🤖 AI学习助手"
    :width="700"
    :footer="null"
    @cancel="handleCancel"
  >
    <div class="agent-chat-container">
      <div class="chat-messages" ref="messagesRef">
        <div v-if="messages.length === 0" class="welcome-message">
          <a-empty description="你好！我是你的AI学习助手">
            <template #description>
              <p>你可以问我：</p>
              <ul>
                <li>• 打开首页/课程页面/用户中心</li>
                <li v-if="isAdmin">• 打开资料管理/用户管理</li>
              </ul>
            </template>
          </a-empty>
        </div>
        
        <div v-else class="message-list">
          <div
            v-for="(msg, index) in messages"
            :key="index"
            :class="['message', msg.role]"
          >
            <a-avatar
              :style="{ backgroundColor: msg.role === 'user' ? '#00B894' : '#1890ff' }"
              class="avatar"
            >
              {{ msg.role === 'user' ? '我' : 'AI' }}
            </a-avatar>
            <div class="content">
              <div class="text">{{ msg.content }}</div>
              <div class="time">{{ msg.time }}</div>
            </div>
          </div>
        </div>

        <div v-if="loading" class="loading-indicator">
          <a-spin size="small" tip="思考中..." />
        </div>
      </div>

      <div class="input-area">
        <a-textarea
          v-model:value="inputText"
          placeholder="输入你的问题..."
          :rows="3"
          @pressEnter="handleEnter"
          :maxlength="2000"
        />
        <div class="input-actions">
          <a-space>
            <a-button @click="clearMessages">清空</a-button>
            <a-button 
              type="primary" 
              :loading="loading" 
              @click="sendMessage"
              :disabled="!inputText.trim()"
            >
              发送
            </a-button>
          </a-space>
          <span class="hint">按 Enter 发送</span>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import { ref, nextTick, watch, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { agentApi, AgentChatResponse } from '@/api/agent'
import { useUserLogin } from '@/store/useUesrLogin'

const router = useRouter()
const userStore = useUserLogin()

interface Message {
  role: 'user' | 'assistant'
  content: string
  time: string
}

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
}>()

const modalVisible = ref(props.open)
const messages = ref<Message[]>([])
const inputText = ref('')
const loading = ref(false)
const messagesRef = ref<HTMLElement>()

const STORAGE_KEY = 'agent-chat-history'

// 检查是否是管理员
const isAdmin = computed(() => {
  const user = userStore.getRawUser()
  if (!user) return false
  return user.userRole === 'admin' || user.userRole === 'ADMIN' || user.role === 'ADMIN'
})

// 检查是否需要管理员权限的页面
const isAdminPage = (page: string) => {
  return page === '/admin/material-manage' || page === '/admin/user-manage'
}

// 从localStorage加载对话历史
const loadMessagesFromStorage = () => {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) {
      messages.value = JSON.parse(saved)
    }
  } catch (e) {
    console.error('加载对话历史失败:', e)
  }
}

// 保存对话历史到localStorage
const saveMessagesToStorage = () => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(messages.value))
  } catch (e) {
    console.error('保存对话历史失败:', e)
  }
}

onMounted(() => {
  loadMessagesFromStorage()
})

watch(() => props.open, (newVal) => {
  modalVisible.value = newVal
  if (newVal) {
    loadMessagesFromStorage()
  }
})

watch(modalVisible, (newVal) => {
  emit('update:open', newVal)
})

watch(messages, () => {
  saveMessagesToStorage()
}, { deep: true })

const handleCancel = () => {
  modalVisible.value = false
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const navigateToPage = (page: string) => {
  // 检查是否是管理员页面且当前用户不是管理员
  if (isAdminPage(page) && !isAdmin.value) {
    const aiMessage: Message = {
      role: 'assistant',
      content: '抱歉，您没有权限访问资料管理和用户管理页面，请使用管理员账号登录。',
      time: new Date().toLocaleTimeString(),
    }
    messages.value.push(aiMessage)
    scrollToBottom()
    return
  }

  const pageNames: Record<string, string> = {
    '/': '首页',
    '/course': '课程页面',
    '/user-center': '用户中心',
    '/admin/material-manage': '资料管理',
    '/admin/user-manage': '用户管理',
  }
  
  const pageName = pageNames[page] || page
  message.success(`正在跳转到${pageName}...`)
  
  setTimeout(() => {
    router.push(page)
  }, 500)
}

const sendMessage = async () => {
  if (!inputText.value.trim()) {
    return
  }

  const userMessage: Message = {
    role: 'user',
    content: inputText.value,
    time: new Date().toLocaleTimeString(),
  }
  
  messages.value.push(userMessage)
  const question = inputText.value
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const response: AgentChatResponse = await agentApi.chat(question, {
      type: 'frontend',
      currentPage: location.pathname,
    })

    let aiContent = '抱歉，服务暂时不可用'
    
    if (response && response.data) {
      aiContent = response.data
    }

    const aiMessage: Message = {
      role: 'assistant',
      content: aiContent,
      time: new Date().toLocaleTimeString(),
    }
    
    messages.value.push(aiMessage)
    scrollToBottom()

    if (response.action === 'navigate' && response.page) {
      navigateToPage(response.page)
    }
  } catch (error: any) {
    const errorMsg = error.message || '请求失败，请确保Agent后端已启动'
    const aiMessage: Message = {
      role: 'assistant',
      content: errorMsg,
      time: new Date().toLocaleTimeString(),
    }
    messages.value.push(aiMessage)
    scrollToBottom()
    
    if (errorMsg.includes('API Key')) {
      message.warning('请先配置Agent的API Key')
    }
  } finally {
    loading.value = false
  }
}

const handleEnter = (e: KeyboardEvent) => {
  if (!e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

const clearMessages = () => {
  messages.value = []
  localStorage.removeItem(STORAGE_KEY)
  message.success('对话已清空')
}
</script>

<style scoped>
.agent-chat-container {
  display: flex;
  flex-direction: column;
  height: 600px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 16px;
}

.welcome-message {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message {
  display: flex;
  gap: 12px;
  max-width: 85%;
}

.message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message.assistant {
  align-self: flex-start;
}

.avatar {
  flex-shrink: 0;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message.user .content {
  align-items: flex-end;
}

.text {
  padding: 12px 16px;
  border-radius: 12px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  word-wrap: break-word;
  line-height: 1.6;
}

.message.user .text {
  background: linear-gradient(135deg, #00B894 0%, #55E6C1 100%);
  color: white;
}

.time {
  font-size: 12px;
  color: #999;
}

.loading-indicator {
  display: flex;
  justify-content: center;
  padding: 16px;
}

.input-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hint {
  font-size: 12px;
  color: #999;
}
</style>
