<template>
  <a-modal
    v-model:open="modalVisible"
    title="🤖 电磁场智能学习助手"
    :width="900"
    :footer="null"
    @cancel="handleCancel"
  >
    <div class="agent-chat-layout">
      <!-- ====== 左侧：会话列表 ====== -->
      <div class="sidebar">
        <a-button type="primary" block @click="newChat" :loading="creatingConv">
          ＋ 新对话
        </a-button>

        <div class="conversation-list" v-if="conversations.length > 0">
          <div
            v-for="conv in conversations"
            :key="conv.id"
            :class="['conv-item', { active: conv.id === currentConversationId }]"
            @click="selectConversation(conv.id)"
          >
            <div class="conv-main">
              <div class="conv-title">{{ conv.title }}</div>
              <div class="conv-preview">{{ conv.lastMessagePreview }}</div>
            </div>
            <div class="conv-actions">
              <a-popconfirm
                title="删除此对话？不可恢复"
                @confirm="deleteConversation(conv.id)"
                ok-text="删除"
                cancel-text="取消"
              >
                <a-button type="link" size="small" danger @click.stop>删</a-button>
              </a-popconfirm>
            </div>
          </div>
        </div>
        <a-empty v-else description="暂无对话" :image-style="{ height: '40px' }" style="margin-top: 40px;" />
      </div>

      <!-- ====== 右侧：对话区 ====== -->
      <div class="chat-area">
        <div class="chat-messages" ref="messagesRef">
          <div v-if="messages.length === 0" class="welcome-message">
            <a-empty description="你好！我是你的电磁场智能学习助手">
              <template #description>
                <p>你可以问我：</p>
                <ul>
                  <li>• 打开首页/课程页面/用户中心</li>
                  <li v-if="isAdmin">• 打开资料管理/用户管理</li>
                  <li>• 任何学习相关的问题</li>
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
                <div v-if="msg.role === 'assistant' && msg.citations && msg.citations.length > 0" class="citations">
                  <a-tag
                    v-for="(cite, ci) in msg.citations"
                    :key="ci"
                    color="blue"
                    style="margin-top: 6px; margin-right: 6px;"
                  >
                    来源：{{ cite.materialTitle || '资料' }}
                  </a-tag>
                </div>
                <div v-if="msg.role === 'assistant'" class="msg-actions">
                  <a-button type="link" size="small" @click="copyMessage(msg.content)">复制</a-button>
                </div>
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
              <a-button @click="clearMessages">清空当前</a-button>
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
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import { ref, nextTick, watch, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { agentApi, AgentChatResponse, Citation } from '@/api/agent'
import { conversationApi, ConversationSummary, ChatMessageVO } from '@/api/conversation'
import { useUserLogin } from '@/store/useUesrLogin'

const router = useRouter()
const userStore = useUserLogin()

interface Message {
  role: 'user' | 'assistant'
  content: string
  time: string
  citations?: Citation[]
}

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ (e: 'update:open', value: boolean): void }>()

const modalVisible = ref(props.open)
const messages = ref<Message[]>([])
const inputText = ref('')
const loading = ref(false)
const messagesRef = ref<HTMLElement>()

// 会话状态
const conversations = ref<ConversationSummary[]>([])
const currentConversationId = ref<string | null>(null)
const creatingConv = ref(false)

const isAdmin = computed(() => {
  const user = userStore.getRawUser()
  if (!user) return false
  return user.userRole === 'admin' || user.userRole === 'ADMIN' || user.role === 'ADMIN'
})

const isAdminPage = (page: string) => page === '/admin/material-manage' || page === '/admin/user-manage'

// ==================== 会话管理 ====================

/** 加载会话列表 */
const loadConversations = async () => {
  try {
    const res = await conversationApi.list()
    conversations.value = res.data || []
  } catch (e: any) {
    console.error('加载会话列表失败:', e)
  }
}

/** 新建对话 */
const newChat = async () => {
  creatingConv.value = true
  try {
    const res = await conversationApi.newConversation()
    const id = res.data.conversationId
    currentConversationId.value = id
    messages.value = []
    await loadConversations()
  } catch (e: any) {
    message.error('创建会话失败：' + (e.message || '未知错误'))
  } finally {
    creatingConv.value = false
  }
}

/** 选择一个会话，加载其历史消息 */
const selectConversation = async (convId: string) => {
  currentConversationId.value = convId
  loading.value = true
  try {
    const res = await conversationApi.getMessages(convId)
    const serverMessages: ChatMessageVO[] = res.data || []
    messages.value = serverMessages.map((m) => ({
      role: m.role as 'user' | 'assistant',
      content: m.content,
      time: m.createTime ? new Date(m.createTime).toLocaleTimeString() : '',
      citations: m.citations || [],
    }))
  } catch (e: any) {
    message.error('加载历史失败：' + (e.message || '未知错误'))
    messages.value = []
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

/** 删除会话 */
const deleteConversation = async (convId: string) => {
  try {
    await conversationApi.remove(convId)
    if (currentConversationId.value === convId) {
      currentConversationId.value = null
      messages.value = []
    }
    await loadConversations()
    message.success('已删除')
  } catch (e: any) {
    message.error('删除失败：' + (e.message || '未知错误'))
  }
}

// ==================== 消息发送 ====================

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const navigateToPage = (page: string) => {
  if (isAdminPage(page) && !isAdmin.value) {
    messages.value.push({
      role: 'assistant',
      content: '抱歉，您没有权限访问资料管理和用户管理页面，请使用管理员账号登录。',
      time: new Date().toLocaleTimeString(),
    })
    scrollToBottom()
    return
  }
  const pageNames: Record<string, string> = {
    '/': '首页', '/course': '课程页面', '/user-center': '用户中心',
    '/admin/material-manage': '资料管理', '/admin/user-manage': '用户管理',
  }
  message.success(`正在跳转到${pageNames[page] || page}...`)
  setTimeout(() => router.push(page), 500)
}

const sendMessage = async () => {
  if (!inputText.value.trim()) return

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
    // 如果当前没有会话，自动新建一个
    if (!currentConversationId.value) {
      const res = await conversationApi.newConversation()
      currentConversationId.value = res.data.conversationId
    }

    // 调用新的会话接口——带多轮记忆
    const convRes = await conversationApi.sendMessage(currentConversationId.value, question)
    const data = convRes.data

    const aiMessage: Message = {
      role: 'assistant',
      content: data.answer,
      time: new Date().toLocaleTimeString(),
      citations: data.citations || [],
    }
    messages.value.push(aiMessage)
    scrollToBottom()

    // 刷新左侧会话列表（标题/预览更新）
    loadConversations()
  } catch (error: any) {
    // 降级：回退到传统 /api/agent/chat（无会话记忆，但仍能回答）
    try {
      const fallback: AgentChatResponse = await agentApi.chat(question, {
        type: 'frontend',
        currentPage: location.pathname,
      })
      const aiMessage: Message = {
        role: 'assistant',
        content: fallback?.data || '抱歉，服务暂时不可用',
        time: new Date().toLocaleTimeString(),
        citations: fallback?.citations || [],
      }
      messages.value.push(aiMessage)
      scrollToBottom()
      if (fallback?.action === 'navigate' && fallback?.page) {
        navigateToPage(fallback.page)
      }
    } catch (fallbackErr: any) {
      const errMsg = fallbackErr?.message || '请求失败'
      messages.value.push({
        role: 'assistant',
        content: errMsg,
        time: new Date().toLocaleTimeString(),
        citations: [],
      })
      scrollToBottom()
      if (errMsg.includes('API Key')) {
        message.warning('请先配置API Key')
      }
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
}

const copyMessage = async (content: string) => {
  try {
    await navigator.clipboard.writeText(content)
    message.success('已复制到剪贴板')
  } catch (e) {
    // 降级：textarea 方式复制
    const ta = document.createElement('textarea')
    ta.value = content
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    message.success('已复制到剪贴板')
  }
}

const handleCancel = () => {
  modalVisible.value = false
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadConversations()
})

watch(() => props.open, (newVal) => {
  modalVisible.value = newVal
  if (newVal) {
    loadConversations()
  }
})

watch(modalVisible, (newVal) => {
  emit('update:open', newVal)
})
</script>

<style scoped>
/* ====== 布局 ====== */
.agent-chat-layout {
  display: flex;
  gap: 0;
  height: 600px;
  border-radius: 8px;
  overflow: hidden;
}

/* ====== 左侧边栏 ====== */
.sidebar {
  width: 220px;
  min-width: 220px;
  background: #fafafa;
  border-right: 1px solid #f0f0f0;
  padding: 12px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.conversation-list {
  margin-top: 12px;
  flex: 1;
  overflow-y: auto;
}

.conv-item {
  padding: 10px 12px;
  margin-bottom: 4px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
  border: 1px solid transparent;
}

.conv-item:hover {
  background: #f0f0f0;
}

.conv-item.active {
  background: #e6f7ff;
  border-color: #1890ff;
}

.conv-title {
  font-weight: 500;
  font-size: 13px;
  color: #262626;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-preview {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-actions {
  margin-top: 4px;
  text-align: right;
}

/* ====== 右侧对话区 ====== */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f7fa;
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
  white-space: pre-wrap;
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

/* ====== 输入区 ====== */
.input-area {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: white;
  border-top: 1px solid #f0f0f0;
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

.msg-actions {
  margin-top: 2px;
}

/* 移动端适配：上限宽度 + 隐藏侧边栏 */
:deep(.ant-modal) { max-width: 96vw; }

@media (max-width: 767px) {
  .sidebar { display: none; }
  .agent-chat-layout { height: 70vh; }
}
</style>
