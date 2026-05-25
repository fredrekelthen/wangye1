<template>
  <div class="ai-chat">
    <a-card title="AI学习助手" style="width: 100%; height: 600px; display: flex; flex-direction: column">
      <template #extra>
        <a-space>
          <a-button size="small" @click="clearHistory">清空对话</a-button>
          <a-button size="small" @click="exportHistory">导出对话</a-button>
        </a-space>
      </template>

      <div class="chat-container" ref="chatContainer">
        <div v-if="messages.length === 0" class="welcome-message">
          <a-empty description="开始与AI学习助手对话吧！">
            <template #description>
              <p>你可以问我关于项目的任何问题：</p>
              <ul>
                <li>项目结构和路由</li>
                <li>组件的功能和用法</li>
                <li>数据库结构和数据</li>
                <li>代码逻辑和业务逻辑</li>
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
              :style="{ backgroundColor: msg.role === 'user' ? '#1890ff' : '#52c41a' }"
              class="message-avatar"
            >
              {{ msg.role === 'user' ? '我' : 'AI' }}
            </a-avatar>
            <div class="message-content">
              <div class="message-text">{{ msg.content }}</div>
              <div class="message-time">{{ msg.time }}</div>
            </div>
          </div>
        </div>

        <div v-if="loading" class="loading-indicator">
          <a-spin tip="AI正在思考..." />
        </div>
      </div>

      <div class="input-area">
        <a-space direction="vertical" style="width: 100%">
          <a-select
            v-model:value="contextType"
            style="width: 200px"
            placeholder="选择上下文"
          >
            <a-select-option value="general">通用问题</a-select-option>
            <a-select-option value="frontend">前端问题</a-select-option>
            <a-select-option value="backend">后端问题</a-select-option>
            <a-select-option value="database">数据库问题</a-select-option>
          </a-select>
          
          <a-textarea
            v-model:value="inputText"
            placeholder="输入你的问题..."
            :rows="3"
            @pressEnter="handleEnter"
          />
          
          <a-space>
            <a-button type="primary" :loading="loading" @click="sendMessage">
              发送
            </a-button>
            <a-button @click="inputText = ''">清空</a-button>
            <a-button @click="useSuggestion">
              使用建议
            </a-button>
          </a-space>
        </a-space>
      </div>

      <div class="suggestions" v-if="showSuggestions">
        <a-tag
          v-for="suggestion in suggestions"
          :key="suggestion"
          color="blue"
          class="suggestion-tag"
          @click="useSuggestionItem(suggestion)"
        >
          {{ suggestion }}
        </a-tag>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue';
import { message } from 'ant-design-vue';
import { agentApi } from '../api/agentApi';

const messages = ref<Array<{role: string; content: string; time: string}>>([]);
const inputText = ref('');
const loading = ref(false);
const chatContainer = ref<any>(null);
const contextType = ref('general');
const showSuggestions = ref(true);

const suggestions = [
  '介绍一下项目的整体架构',
  '首页有哪些主要功能？',
  '用户登录的流程是怎样的？',
  '数据库有哪些集合？',
  '如何新增一个课程章节？',
];

const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight;
    }
  });
};

const sendMessage = async () => {
  if (!inputText.value.trim()) {
    message.warning('请输入问题');
    return;
  }

  const userMessage = {
    role: 'user',
    content: inputText.value,
    time: new Date().toLocaleTimeString(),
  };
  messages.value.push(userMessage);
  const question = inputText.value;
  inputText.value = '';
  showSuggestions.value = false;
  loading.value = true;
  scrollToBottom();

  try {
    const context = {
      type: contextType.value,
      projectPath: {
        frontend: '../qianduan',
        backend: '../backend',
      },
    };
    
    const response = await agentApi.chat(question, context);
    
    const aiMessage = {
      role: 'assistant',
      content: response.data || '抱歉，我无法回答这个问题。',
      time: new Date().toLocaleTimeString(),
    };
    messages.value.push(aiMessage);
    scrollToBottom();
  } catch (error) {
    message.error('发送失败，请重试');
    messages.value.push({
      role: 'assistant',
      content: '抱歉，服务出现问题，请稍后重试。',
      time: new Date().toLocaleTimeString(),
    });
  } finally {
    loading.value = false;
  }
};

const handleEnter = (e: KeyboardEvent) => {
  if (!e.shiftKey) {
    e.preventDefault();
    sendMessage();
  }
};

const clearHistory = () => {
  messages.value = [];
  showSuggestions.value = true;
  message.success('对话已清空');
};

const exportHistory = () => {
  const history = messages.value
    .map((msg) => `${msg.role === 'user' ? '我' : 'AI'}: ${msg.content}`)
    .join('\n\n');
  
  const blob = new Blob([history], { type: 'text/plain' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `chat-history-${Date.now()}.txt`;
  a.click();
  URL.revokeObjectURL(url);
  message.success('对话已导出');
};

const useSuggestion = () => {
  showSuggestions.value = !showSuggestions.value;
};

const useSuggestionItem = (suggestion: string) => {
  inputText.value = suggestion;
  showSuggestions.value = false;
  sendMessage();
};
</script>

<style scoped>
.chat-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 16px;
  min-height: 300px;
  max-height: 400px;
}

.welcome-message {
  display: flex;
  align-items: center;
  justify-content: center;
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
  max-width: 80%;
}

.message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message.assistant {
  align-self: flex-start;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message.user .message-content {
  align-items: flex-end;
}

.message-text {
  padding: 12px;
  border-radius: 8px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  word-wrap: break-word;
}

.message.user .message-text {
  background: #1890ff;
  color: white;
}

.message-time {
  font-size: 12px;
  color: #888;
}

.loading-indicator {
  display: flex;
  justify-content: center;
  padding: 16px;
}

.input-area {
  margin-top: 16px;
}

.suggestions {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.suggestion-tag {
  cursor: pointer;
  padding: 8px 16px;
  font-size: 14px;
}

.suggestion-tag:hover {
  opacity: 0.8;
}
</style>
