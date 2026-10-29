<template>
  <a-modal
    v-model:open="modalVisible"
    title="🤖 电磁场智能学习助手"
    :width="700"
    :footer="null"
    @cancel="handleCancel"
  >
    <div class="agent-chat-container">
      <div class="chat-messages" ref="messagesRef">
        <div v-if="messages.length === 0" class="welcome-message">
          <a-empty description="你好！我是你的电磁场智能学习助手">
            <template #description>
              <p>你可以问我：</p>
              <ul>
                <li>• 项目结构和功能</li>
                <li>• 组件代码分析</li>
                <li>• 数据库结构</li>
                <li>• 业务逻辑说明</li>
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
import { ref, nextTick, watch } from 'vue'
import { message } from 'ant-design-vue'
import { agentApi } from '@/api/agent'

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

watch(() => props.open, (newVal) => {
  modalVisible.value = newVal
})

watch(modalVisible, (newVal) => {
  emit('update:open', newVal)
})

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
    const response = await agentApi.chat(question, {
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
