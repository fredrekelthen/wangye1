<template>
  <div class="notice-section">
    <div class="section-header">
      <h3 class="section-title">公告</h3>
      <div class="section-actions" v-if="isAdmin">
        <a-space>
          <a-button type="primary" size="small" @click="showCreateModal = true">
            发布公告
          </a-button>
        </a-space>
      </div>
    </div>

    <div class="notice-list">
      <div 
        v-for="notice in notices" 
        :key="notice.id" 
        class="notice-item"
        :class="{ read: notice.isRead }"
      >
        <div class="notice-header">
          <div class="notice-type">
            <span v-if="notice.isExample" class="type-badge example">样例</span>
            <span v-if="notice.type === 'urgent'" class="type-badge urgent">重要</span>
            <span v-else-if="notice.type === 'assignment'" class="type-badge assignment">作业</span>
            <span v-else-if="notice.type === 'exam'" class="type-badge exam">考试</span>
            <span v-else class="type-badge normal">通知</span>
            <span v-if="!notice.isRead" class="type-badge unread">未读</span>
          </div>
          <span class="notice-date">{{ notice.date }}</span>
          <div class="notice-actions" v-if="isAdmin">
            <a-button type="text" size="small" @click="editNotice(notice)">编辑</a-button>
            <a-button type="text" size="small" danger @click="deleteNotice(notice.id)">删除</a-button>
          </div>
        </div>
        <h4 class="notice-title">{{ notice.title }}</h4>
        <p class="notice-content">{{ notice.content }}</p>
        <div class="notice-footer">
          <div class="read-status" v-if="!notice.isRead && !isAdmin">
            <a-button type="link" size="small" @click="markAsRead(notice.id)">
              👁️ 标记为已读
            </a-button>
          </div>
          <div class="read-status" v-else-if="notice.isRead">
            <span class="read-icon">✓</span>
            <span>已读</span>
          </div>
          <div class="read-count" v-if="isAdmin">
            <span>已读人数: {{ notice.readCount }}/{{ notice.totalCount }}</span>
          </div>
          <div class="notice-footer" v-if="notice.attachments && notice.attachments.length > 0">
            <span class="attachment-label">📎 附件：</span>
            <a-space>
              <a 
                  v-for="(file, idx) in notice.attachments" 
                  :key="file.name || idx" 
                  class="attachment-link"
                  :href="file.url"
                >
                {{ file.name }}
              </a>
            </a-space>
          </div>
        </div>
      </div>
    </div>

    <div class="pagination">
      <a-pagination 
        v-model:current="currentPage" 
        :total="totalNotices" 
        :page-size="pageSize"
        show-quick-jumper
        :show-size-changer="false"
      />
    </div>

    <!-- 发布/编辑公告弹窗 -->
    <a-modal 
      v-model:open="showCreateModal" 
      :title="editingNotice ? '编辑公告' : '发布公告'" 
      :footer="null"
      width="600px"
    >
      <a-form 
        :model="formData" 
        layout="vertical"
        @finish="submitNotice"
      >
        <a-form-item label="公告类型" name="type" required>
          <a-select v-model:value="formData.type">
            <a-select-option value="normal">通知</a-select-option>
            <a-select-option value="urgent">重要</a-select-option>
            <a-select-option value="assignment">作业</a-select-option>
            <a-select-option value="exam">考试</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="标题" name="title" required>
          <a-input v-model:value="formData.title" placeholder="请输入公告标题" />
        </a-form-item>
        <a-form-item label="内容" name="content" required>
          <a-textarea 
            v-model:value="formData.content" 
            :rows="6" 
            placeholder="请输入公告内容"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">保存</a-button>
            <a-button @click="cancelEdit">取消</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue';
import { message } from 'ant-design-vue';

const isAdmin = ref(true);
const currentPage = ref(1);
const pageSize = 10;
const showCreateModal = ref(false);
const editingNotice = ref<Notice | null>(null);

interface Notice {
  id: number;
  type: string;
  date: string;
  title: string;
  content: string;
  attachments?: { name: string; url: string }[];
  isRead: boolean;
  readCount: number;
  totalCount: number;
  isExample?: boolean;
}

const formData = reactive({
  type: 'normal',
  title: '',
  content: ''
});

const notices = ref<Notice[]>([
  {
    id: 1,
    type: 'urgent',
    date: '2025-12-10',
    title: '【重要】期中考试安排通知',
    content: '各位同学请注意，期中考试将于12月20日进行，考试范围涵盖第1-4章内容。请提前做好复习准备，考试时间为上午9:00-11:00，地点在教学楼302教室。请携带学生证和计算器。',
    attachments: [
      { name: '考试安排表.pdf', url: '#' },
      { name: '复习重点.docx', url: '#' }
    ],
    isRead: false,
    readCount: 32,
    totalCount: 45,
    isExample: true
  },
  {
    id: 2,
    type: 'assignment',
    date: '2025-12-08',
    title: '第三章习题作业提交截止提醒',
    content: '第三章习题作业将于12月12日23:59截止提交，请还未提交的同学抓紧时间完成。作业需提交到学习平台的作业模块，支持PDF、Word格式。',
    attachments: [
      { name: '作业要求.pdf', url: '#' }
    ],
    isRead: true,
    readCount: 40,
    totalCount: 45,
    isExample: true
  },
  {
    id: 3,
    type: 'exam',
    date: '2025-12-05',
    title: '期末考试形式说明',
    content: '本学期期末考试将采用闭卷形式，考试时间为120分钟。试卷包含选择题、计算题和综合应用题。考试范围覆盖全课程内容，请全面复习。',
    attachments: [],
    isRead: true,
    readCount: 38,
    totalCount: 45,
    isExample: true
  },
  {
    id: 4,
    type: 'normal',
    date: '2025-12-03',
    title: '课程调整通知',
    content: '由于老师临时有事，原定于12月8日的课程调整至12月9日进行，时间地点不变。请同学们相互转告，准时上课。',
    attachments: [],
    isRead: false,
    readCount: 28,
    totalCount: 45,
    isExample: true
  },
  {
    id: 5,
    type: 'normal',
    date: '2025-12-01',
    title: '学习资料更新',
    content: '课程学习资料已更新，新增了第三章的补充材料和案例分析，请同学们登录学习平台查看最新内容。',
    attachments: [
      { name: '补充材料.zip', url: '#' }
    ],
    isRead: true,
    readCount: 35,
    totalCount: 45,
    isExample: true
  }
]);

const totalNotices = computed(() => notices.value.length);

const markAsRead = (id: number) => {
  const notice = notices.value.find(n => n.id === id);
  if (notice && !notice.isRead) {
    notice.isRead = true;
    notice.readCount++;
    message.success('已标记为已读');
  }
};

const editNotice = (notice: Notice) => {
  editingNotice.value = notice;
  formData.type = notice.type;
  formData.title = notice.title;
  formData.content = notice.content;
  showCreateModal.value = true;
};

const deleteNotice = (id: number) => {
  notices.value = notices.value.filter(n => n.id !== id);
  message.success('公告已删除');
};

const submitNotice = () => {
  if (!formData.title || !formData.content) {
    message.warning('请填写完整信息');
    return;
  }
  
  if (editingNotice.value) {
    const idx = notices.value.findIndex(n => n.id === editingNotice.value!.id);
    if (idx !== -1) {
      notices.value[idx].type = formData.type;
      notices.value[idx].title = formData.title;
      notices.value[idx].content = formData.content;
    }
    message.success('公告已更新');
  } else {
    notices.value.unshift({
      id: Date.now(),
      type: formData.type,
      date: new Date().toISOString().split('T')[0],
      title: formData.title,
      content: formData.content,
      attachments: [],
      isRead: false,
      readCount: 0,
      totalCount: 45
    });
    message.success('公告发布成功');
  }
  
  cancelEdit();
};

const cancelEdit = () => {
  showCreateModal.value = false;
  editingNotice.value = null;
  formData.type = 'normal';
  formData.title = '';
  formData.content = '';
};
</script>

<style scoped>
.notice-section {
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

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.notice-item {
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
  border-left: 4px solid #e8e8e8;
  transition: all 0.3s;
}

.notice-item:hover {
  background: #f5f5f5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.notice-item.read {
  background: #fff;
  border-left-color: #d9d9d9;
}

.notice-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.notice-type {
  display: flex;
  gap: 8px;
}

.type-badge {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.type-badge.urgent {
  background: #fff1f0;
  color: #ff4d4f;
}

.type-badge.assignment {
  background: #fff7e6;
  color: #fa8c16;
}

.type-badge.exam {
  background: #ffe7ba;
  color: #faad14;
}

.type-badge.normal {
  background: #e6f7ff;
  color: #1890ff;
}

.type-badge.unread {
  background: #52c41a;
  color: #fff;
}

.type-badge.example {
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.notice-date {
  color: #999;
  font-size: 12px;
}

.notice-actions {
  display: flex;
  gap: 8px;
}

.notice-title {
  margin: 0 0 10px 0;
  font-size: 16px;
  font-weight: 600;
  color: #222;
}

.notice-content {
  margin: 0 0 12px 0;
  color: #666;
  line-height: 1.6;
}

.notice-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid #eee;
}

.read-status {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #667eea;
}

.read-icon {
  color: #52c41a;
  font-weight: bold;
}

.read-count {
  font-size: 12px;
  color: #999;
}

.attachment-label {
  font-size: 13px;
  color: #999;
}

.attachment-link {
  color: #667eea;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.3s;
}

.attachment-link:hover {
  color: #764ba2;
  text-decoration: underline;
}

.pagination {
  margin-top: 20px;
  text-align: center;
}
</style>