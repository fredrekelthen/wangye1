<template>
  <div class="discuss-section">
    <div class="section-header">
      <h3 class="section-title">讨论区</h3>
      <div class="section-actions">
        <a-button type="primary" @click="showCreateModal = true">发起讨论</a-button>
      </div>
    </div>

    <div class="filter-bar">
      <a-input 
        v-model:value="searchKeyword" 
        placeholder="搜索讨论..." 
        style="width: 300px"
        allowClear
      >
        <template #prefix>
          <span>🔍</span>
        </template>
      </a-input>
      <a-select v-model:value="filterType" style="width: 150px" placeholder="讨论类型">
        <a-select-option value="all">全部</a-select-option>
        <a-select-option value="question">问题</a-select-option>
        <a-select-option value="share">分享</a-select-option>
        <a-select-option value="answer">答疑</a-select-option>
      </a-select>
    </div>

    <div class="discussions-list">
      <div 
        v-for="discussion in filteredDiscussions" 
        :key="discussion.id" 
        class="discussion-item"
        :class="{ expanded: expandedDiscussion === discussion.id }"
      >
        <div class="discussion-main">
          <div class="discussion-avatar">
            {{ discussion.avatar }}
          </div>
          <div class="discussion-content">
            <div class="discussion-header">
              <span class="author">{{ discussion.author }}</span>
              <span class="time">{{ discussion.time }}</span>
              <a-tag v-if="discussion.isExample" color="green" class="type-tag">样例</a-tag>
              <a-tag :color="getTypeColor(discussion.type)" class="type-tag">
                {{ getTypeLabel(discussion.type) }}
              </a-tag>
            </div>
            <h4 class="discussion-title">{{ discussion.title }}</h4>
            <p class="discussion-text">{{ discussion.content }}</p>
            <div class="discussion-actions">
              <span class="action-item" @click="toggleLike(discussion.id)">
                <span class="icon">{{ discussion.liked ? '❤️' : '🤍' }}</span>
                <span class="count">{{ discussion.likes }}</span>
              </span>
              <span class="action-item">
                <span class="icon">💬</span>
                <span class="count">{{ discussion.replies.length }}</span>
              </span>
              <span class="action-item">
                <span class="icon">👁️</span>
                <span class="count">{{ discussion.views }}</span>
              </span>
              <a-button type="link" size="small" @click="toggleExpand(discussion.id)">
                {{ expandedDiscussion === discussion.id ? '收起' : '查看回复' }}
              </a-button>
            </div>
          </div>
        </div>

        <!-- 回复列表 -->
        <div v-if="expandedDiscussion === discussion.id" class="replies-section">
          <div class="replies-header">
            <span class="replies-count">回复 ({{ discussion.replies.length }})</span>
          </div>
          
          <div class="replies-list">
            <div 
              v-for="reply in discussion.replies" 
              :key="reply.id" 
              class="reply-item"
            >
              <div class="reply-avatar">{{ reply.avatar }}</div>
              <div class="reply-content">
                <div class="reply-header">
                  <span class="reply-author">{{ reply.author }}</span>
                  <span class="reply-time">{{ reply.time }}</span>
                </div>
                <p class="reply-text">{{ reply.content }}</p>
                <div class="reply-actions">
                  <span class="reply-action" @click="toggleReplyLike(discussion.id, reply.id)">
                    {{ reply.liked ? '❤️' : '🤍' }} {{ reply.likes }}
                  </span>
                  <span class="reply-action" @click="replyToReply(discussion.id, reply)">
                    ↩️ 回复
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- 回复输入框 -->
          <div class="reply-input-area">
            <a-textarea 
              v-model:value="replyContent[discussion.id]" 
              :rows="3" 
              placeholder="写下你的回复..."
              @pressEnter="submitReply(discussion.id)"
            />
            <div class="reply-actions-bar">
              <a-button type="primary" @click="submitReply(discussion.id)">发送回复</a-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="pagination">
      <a-pagination 
        v-model:current="currentPage" 
        :total="totalDiscussions" 
        :page-size="pageSize"
        show-quick-jumper
        show-size-changer
      />
    </div>

    <!-- 发起讨论弹窗 -->
    <a-modal 
      v-model:open="showCreateModal" 
      title="发起讨论" 
      :footer="null"
      width="600px"
    >
      <a-form 
        :model="newDiscussion" 
        layout="vertical"
        @finish="submitDiscussion"
      >
        <a-form-item label="讨论类型" name="type" required>
          <a-radio-group v-model:value="newDiscussion.type">
            <a-radio value="question">问题</a-radio>
            <a-radio value="share">分享</a-radio>
            <a-radio value="answer">答疑</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="标题" name="title" required>
          <a-input v-model:value="newDiscussion.title" placeholder="请输入讨论标题" />
        </a-form-item>
        <a-form-item label="内容" name="content" required>
          <a-textarea 
            v-model:value="newDiscussion.content" 
            :rows="6" 
            placeholder="请输入讨论内容"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">发布</a-button>
            <a-button @click="showCreateModal = false">取消</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { message } from 'ant-design-vue'

interface Reply {
  id: number;
  avatar: string;
  author: string;
  time: string;
  content: string;
  likes: number;
  liked: boolean;
}

interface Discussion {
  id: number;
  avatar: string;
  author: string;
  time: string;
  type: string;
  title: string;
  content: string;
  likes: number;
  replies: Reply[];
  views: number;
  liked: boolean;
  isExample?: boolean;
}

const searchKeyword = ref('')
const filterType = ref('all')
const currentPage = ref(1)
const pageSize = ref(10)
const showCreateModal = ref(false)
const expandedDiscussion = ref<number | null>(null)

const replyContent = reactive<Record<number, string>>({})

const newDiscussion = ref({ type: 'question', title: '', content: '' })

const discussions = ref<Discussion[]>([
  { 
    id: 1, 
    avatar: '👨‍🎓', 
    author: '张同学', 
    time: '2小时前', 
    type: 'question', 
    title: '关于电磁场叠加原理的理解', 
    content: '在学习电磁场叠加原理时，我有一个疑问：多个电场叠加时，每个电场对其他电场有没有影响？比如两个正电荷产生的电场，它们之间会相互干扰吗？', 
    likes: 12, 
    replies: [
      { id: 101, avatar: '👨‍🏫', author: '王老师', time: '1小时前', content: '同学你好！电磁场叠加原理是线性的，多个电场叠加时只是矢量相加，并不会相互干扰。每个电荷独立产生自己的电场，空间中某点的总场强就是各个电荷在该点产生场强的矢量和。', likes: 8, liked: false },
      { id: 102, avatar: '👩‍🎓', author: '李同学', time: '30分钟前', content: '补充一下，这个原理的前提是介质是线性的。如果是非线性介质，确实会有相互作用，但我们课程中讨论的都是线性介质情况。', likes: 5, liked: false }
    ], 
    views: 156, 
    liked: false,
    isExample: true
  },
  { 
    id: 2, 
    avatar: '👩‍🎓', 
    author: '李同学', 
    time: '5小时前', 
    type: 'share', 
    title: '分享：电磁场可视化工具', 
    content: '我发现了一个很棒的电磁场可视化工具，可以直观地展示电场线和等势面。推荐给大家！', 
    likes: 34, 
    replies: [
      { id: 201, avatar: '👨‍💻', author: '王同学', time: '4小时前', content: '太棒了！这个工具在哪里可以找到？', likes: 3, liked: false },
      { id: 202, avatar: '👩‍🎓', author: '李同学', time: '4小时前', content: '在GitHub上搜索em-field-visualizer就能找到，是开源项目。', likes: 4, liked: false }
    ], 
    views: 289, 
    liked: true,
    isExample: true
  },
  { 
    id: 3, 
    avatar: '👨‍🏫', 
    author: '王老师', 
    time: '1天前', 
    type: 'answer', 
    title: '答疑：麦克斯韦方程组的应用范围', 
    content: '麦克斯韦方程组是电磁学的基本方程，适用于所有宏观电磁现象。但在微观领域（如原子尺度），需要用量子电动力学来描述。', 
    likes: 89, 
    replies: [
      { id: 301, avatar: '👨‍🎓', author: '张同学', time: '12小时前', content: '老师，那在等离子体物理中麦克斯韦方程组还适用吗？', likes: 6, liked: false },
      { id: 302, avatar: '👨‍🏫', author: '王老师', time: '10小时前', content: '适用的，等离子体虽然是带电粒子集合，但在宏观尺度上仍然可以用麦克斯韦方程组来描述其电磁行为。', likes: 12, liked: false }
    ], 
    views: 456, 
    liked: false,
    isExample: true
  }
])

const filteredDiscussions = computed(()=> discussions.value.filter(d=> (searchKeyword.value? (d.title.includes(searchKeyword.value)||d.content.includes(searchKeyword.value)) : true) && (filterType.value==='all' ? true : d.type===filterType.value) ))
const totalDiscussions = computed(()=> discussions.value.length)

const typeColors:any = { question:'orange', share:'blue', answer:'green' }
const typeLabels:any = { question:'问题', share:'分享', answer:'答疑' }
const getTypeColor = (t:string)=> typeColors[t] || 'default'
const getTypeLabel = (t:string)=> typeLabels[t] || t

const toggleLike = (id:number)=>{ 
  const d = discussions.value.find(x=>x.id===id); 
  if(!d) return; 
  d.liked ? (d.likes--, d.liked=false) : (d.likes++, d.liked=true, message.success('已点赞')) 
}

const toggleExpand = (id: number) => {
  expandedDiscussion.value = expandedDiscussion.value === id ? null : id;
}

const submitReply = (discussionId: number) => {
  const content = replyContent[discussionId];
  if (!content || !content.trim()) {
    message.warning('请输入回复内容');
    return;
  }
  
  const discussion = discussions.value.find(d => d.id === discussionId);
  if (!discussion) return;
  
  discussion.replies.push({
    id: Date.now(),
    avatar: '👨‍🎓',
    author: '我',
    time: '刚刚',
    content: content.trim(),
    likes: 0,
    liked: false
  });
  
  replyContent[discussionId] = '';
  message.success('回复成功');
}

const toggleReplyLike = (discussionId: number, replyId: number) => {
  const discussion = discussions.value.find(d => d.id === discussionId);
  if (!discussion) return;
  
  const reply = discussion.replies.find(r => r.id === replyId);
  if (!reply) return;
  
  reply.liked ? reply.likes-- : reply.likes++;
  reply.liked = !reply.liked;
}

const replyToReply = (discussionId: number, reply: Reply) => {
  replyContent[discussionId] = `@${reply.author} `;
}

const submitDiscussion = ()=> {
  if (!newDiscussion.value.title || !newDiscussion.value.content) {
    message.warning('请填写完整信息');
    return;
  }
  
  discussions.value.unshift({
    id: Date.now(),
    avatar: '👨‍🎓',
    author: '我',
    time: '刚刚',
    type: newDiscussion.value.type,
    title: newDiscussion.value.title,
    content: newDiscussion.value.content,
    likes: 0,
    replies: [],
    views: 0,
    liked: false
  });
  
  message.success('讨论发布成功！');
  showCreateModal.value = false;
  newDiscussion.value = { type: 'question', title: '', content: '' };
}
</script>

<style scoped>
.discuss-section {
  background: #fff;
  border: 1px solid #e6e6e6;
  border-radius: 4px;
  padding: 20px;
  min-height: 420px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
}

.section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.discussions-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.discussion-item {
  background: #fafafa;
  border-radius: 8px;
  transition: all 0.3s;
}

.discussion-main {
  display: flex;
  gap: 12px;
  padding: 16px;
}

.discussion-item:hover .discussion-main {
  background: #f5f5f5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.discussion-avatar {
  width: 48px;
  height: 48px;
  min-width: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
}

.discussion-content {
  flex: 1;
}

.discussion-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.author {
  font-weight: 600;
  color: #333;
}

.time {
  color: #999;
  font-size: 12px;
}

.type-tag {
  margin-left: auto;
}

.discussion-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #222;
}

.discussion-text {
  margin: 0 0 12px 0;
  color: #666;
  line-height: 1.6;
}

.discussion-actions {
  display: flex;
  gap: 20px;
  align-items: center;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #999;
  font-size: 14px;
  transition: color 0.3s;
}

.action-item:hover {
  color: #667eea;
}

.icon {
  font-size: 16px;
}

.count {
  font-size: 14px;
}

/* 回复区域 */
.replies-section {
  background: #fff;
  border-top: 1px solid #eee;
  padding: 16px;
}

.replies-header {
  margin-bottom: 12px;
}

.replies-count {
  font-weight: 600;
  color: #333;
}

.replies-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reply-item {
  display: flex;
  gap: 10px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
}

.reply-avatar {
  width: 36px;
  height: 36px;
  min-width: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
}

.reply-content {
  flex: 1;
}

.reply-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}

.reply-author {
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.reply-time {
  color: #999;
  font-size: 12px;
}

.reply-text {
  margin: 0;
  color: #555;
  font-size: 14px;
  line-height: 1.5;
}

.reply-actions {
  display: flex;
  gap: 16px;
  margin-top: 8px;
}

.reply-action {
  color: #999;
  font-size: 12px;
  cursor: pointer;
  transition: color 0.3s;
}

.reply-action:hover {
  color: #667eea;
}

.reply-input-area {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #ddd;
}

.reply-actions-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.pagination {
  margin-top: 20px;
  text-align: center;
}
</style>