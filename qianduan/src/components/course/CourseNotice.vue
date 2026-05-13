<template>
  <div class="notice-section">
    <div class="section-header">
      <h3 class="section-title">公告</h3>
      <a-button type="primary" size="small" v-if="isAdmin">
        发布公告
      </a-button>
    </div>

    <div class="notice-list">
      <div 
        v-for="notice in notices" 
        :key="notice.id" 
        class="notice-item"
      >
        <div class="notice-header">
          <div class="notice-type">
            <span v-if="notice.type === 'urgent'" class="type-badge urgent">重要</span>
            <span v-else-if="notice.type === 'assignment'" class="type-badge assignment">作业</span>
            <span v-else-if="notice.type === 'exam'" class="type-badge exam">考试</span>
            <span v-else class="type-badge normal">通知</span>
          </div>
          <span class="notice-date">{{ notice.date }}</span>
        </div>
        <h4 class="notice-title">{{ notice.title }}</h4>
        <p class="notice-content">{{ notice.content }}</p>
        <div class="notice-footer" v-if="notice.attachments && notice.attachments.length > 0">
          <span class="attachment-label">📎 附件：</span>
          <a-space>
            <a 
                v-for="(file, idx) in notice.attachments" 
                :key="file.name || idx" 
                class="attachment-link"
              >
              {{ file.name }}
            </a>
          </a-space>
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';

const isAdmin = ref(false);
const currentPage = ref(1);
const pageSize = 10;

const notices = ref([
  {
    id: 1,
    type: 'urgent',
    date: '2025-12-10',
    title: '【重要】期中考试安排通知',
    content: '各位同学请注意，期中考试将于12月20日进行，考试范围涵盖第1-4章内容。请提前做好复习准备，考试时间为上午9:00-11:00，地点在教学楼302教室。请携带学生证和计算器。',
    attachments: [
      { name: '考试安排表.pdf', url: '#' },
      { name: '复习重点.docx', url: '#' }
    ]
  },
  {
    id: 2,
    type: 'assignment',
    date: '2025-12-08',
    title: '第三章习题作业提交截止提醒',
    content: '第三章习题作业将于12月12日23:59截止提交，请还未提交的同学抓紧时间完成。作业需提交到学习平台的作业模块，支持PDF、Word格式。',
    attachments: [
      { name: '作业要求.pdf', url: '#' }
    ]
  },
  {
    id: 3,
    type: 'exam',
    date: '2025-12-05',
    title: '期末考试形式说明',
    content: '本学期期末考试将采用闭卷形式，考试时间为120分钟。试卷包含选择题、计算题和综合应用题。考试范围覆盖全课程内容，请全面复习。',
    attachments: []
  },
  {
    id: 4,
    type: 'normal',
    date: '2025-12-03',
    title: '课程调整通知',
    content: '由于老师临时有事，原定于12月8日的课程调整至12月9日进行，时间地点不变。请同学们相互转告，准时上课。',
    attachments: []
  },
  {
    id: 5,
    type: 'normal',
    date: '2025-12-01',
    title: '学习资料更新',
    content: '课程学习资料已更新，新增了第三章的补充材料和案例分析，请同学们登录学习平台查看最新内容。',
    attachments: [
      { name: '补充材料.zip', url: '#' }
    ]
  }
]);

const totalNotices = computed(() => notices.value.length);
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
  border-left-color: #667eea;
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

.notice-date {
  color: #999;
  font-size: 12px;
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
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #eee;
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
