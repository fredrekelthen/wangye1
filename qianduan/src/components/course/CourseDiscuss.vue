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
      >
        <div class="discussion-avatar">
          {{ discussion.avatar }}
        </div>
        <div class="discussion-content">
          <div class="discussion-header">
            <span class="author">{{ discussion.author }}</span>
            <span class="time">{{ discussion.time }}</span>
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
              <span class="count">{{ discussion.replies }}</span>
            </span>
            <span class="action-item">
              <span class="icon">👁️</span>
              <span class="count">{{ discussion.views }}</span>
            </span>
            <a-button type="link" size="small" @click="viewDetail(discussion.id)">
              查看详情
            </a-button>
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
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'

const searchKeyword = ref('')
const filterType = ref('all')
const currentPage = ref(1)
const pageSize = ref(10)
const showCreateModal = ref(false)

const newDiscussion = ref({ type: 'question', title: '', content: '' })

const discussions = ref([
  { id:1, avatar:'👨‍🎓', author:'张同学', time:'2小时前', type:'question', title:'关于电磁场叠加原理的理解', content:'在学习电磁场叠加原理时，我有一个疑问：多个电场叠加时，每个电场对其他电场有没有影响？', likes:12, replies:8, views:156, liked:false },
  { id:2, avatar:'👩‍🎓', author:'李同学', time:'5小时前', type:'share', title:'分享：电磁场可视化工具', content:'我发现了一个很棒的电磁场可视化工具...', likes:34, replies:15, views:289, liked:true },
  { id:3, avatar:'👨‍🏫', author:'王老师', time:'1天前', type:'answer', title:'答疑：麦克斯韦方程组的应用范围', content:'麦克斯韦方程组是电磁学的基本方程...', likes:89, replies:23, views:456, liked:false }
])

const filteredDiscussions = computed(()=> discussions.value.filter(d=> (searchKeyword.value? (d.title.includes(searchKeyword.value)||d.content.includes(searchKeyword.value)) : true) && (filterType.value==='all' ? true : d.type===filterType.value) ))
const totalDiscussions = computed(()=> filteredDiscussions.value.length)

const typeColors:any = { question:'orange', share:'blue', answer:'green' }
const typeLabels:any = { question:'问题', share:'分享', answer:'答疑' }
const getTypeColor = (t:string)=> typeColors[t] || 'default'
const getTypeLabel = (t:string)=> typeLabels[t] || t
const toggleLike = (id:number)=>{ const d = discussions.value.find(x=>x.id===id); if(!d) return; d.liked ? (d.likes--, d.liked=false) : (d.likes++, d.liked=true, message.success('已点赞')) }
const viewDetail = (id:number)=> message.info('查看讨论详情功能待实现')
const submitDiscussion = ()=> (message.success('讨论发布成功！'), showCreateModal.value=false, newDiscussion.value={ type:'question', title:'', content:'' })
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
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  transition: all 0.3s;
}

.discussion-item:hover {
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

.pagination {
  margin-top: 20px;
  text-align: center;
}
</style>
