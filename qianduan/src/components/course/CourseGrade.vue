<template>
  <div class="grade-section">
    <div class="section-header">
      <h3 class="section-title">成绩单</h3>
      <div class="header-actions">
        <a-space>
          <a-select v-model:value="semester" style="width: 150px" placeholder="选择学期">
            <a-select-option value="all">全部学期</a-select-option>
            <a-select-option value="2024-1">2024秋季</a-select-option>
            <a-select-option value="2024-2">2024春季</a-select-option>
            <a-select-option value="2025-1">2025秋季</a-select-option>
          </a-select>
          <a-button type="primary" @click="exportGrade">
            📥 导出成绩
          </a-button>
        </a-space>
      </div>
    </div>

    <div class="grade-summary">
      <div class="summary-card">
        <div class="summary-item">
          <span class="summary-label">平均分</span>
          <span class="summary-value">{{ averageScore }}</span>
        </div>
      </div>
      <div class="summary-card">
        <div class="summary-item">
          <span class="summary-label">最高分</span>
          <span class="summary-value highlight">{{ maxScore }}</span>
        </div>
      </div>
      <div class="summary-card">
        <div class="summary-item">
          <span class="summary-label">最低分</span>
          <span class="summary-value low">{{ minScore }}</span>
        </div>
      </div>
      <div class="summary-card">
        <div class="summary-item">
          <span class="summary-label">排名</span>
          <span class="summary-value rank">{{ rank }}/{{ totalStudents }}</span>
        </div>
      </div>
    </div>

    <div class="grade-chart">
      <div class="chart-header">
        <h4 class="chart-title">成绩趋势</h4>
        <div class="chart-legend">
          <span class="legend-item">
            <span class="legend-color blue"></span>
            <span>课程成绩</span>
          </span>
          <span class="legend-item">
            <span class="legend-color green"></span>
            <span>平均分</span>
          </span>
        </div>
      </div>
      <div class="chart-content">
        <div class="chart-bars">
          <div 
            v-for="exam in filteredExams" 
            :key="exam.id" 
            class="bar-item"
          >
            <div class="bar-wrapper">
              <div 
                class="bar" 
                :style="{ height: exam.height + '%', background: exam.color }"
              >
                <span class="bar-label">{{ exam.score }}</span>
              </div>
              <span class="bar-name">{{ exam.name }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="grade-details">
      <h4 class="details-title">成绩明细</h4>
      <a-table 
        :columns="columns" 
        :data-source="filteredExams" 
        :pagination="{ pageSize: 10 }"
        :row-key="(record) => record.id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'score'">
            <a-tag :color="getScoreColor(record.score)">
              {{ record.score }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'rank'">
            <span :class="['rank', getRankClass(record.rank)]">
              第 {{ record.rank }} 名
            </span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ record.status }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { message } from 'ant-design-vue';

interface Exam {
  id: number;
  name: string;
  score: number;
  rank: number;
  status: string;
  date: string;
  height: string;
  color: string;
  semester: string;
}

const semester = ref('all');

const columns = [
  { title: '考试名称', dataIndex: 'name', key: 'name' },
  { title: '考试日期', dataIndex: 'date', key: 'date' },
  { title: '成绩', dataIndex: 'score', key: 'score' },
  { title: '班级排名', dataIndex: 'rank', key: 'rank' },
  { title: '状态', dataIndex: 'status', key: 'status' }
];

const exams = ref<Exam[]>([
  {
    id: 1,
    name: '期中考试',
    score: 85,
    rank: 12,
    status: '已发布',
    date: '2025-10-20',
    height: '85',
    color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    semester: '2025-1'
  },
  {
    id: 2,
    name: '第一章测验',
    score: 92,
    rank: 5,
    status: '已发布',
    date: '2025-09-25',
    height: '92',
    color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    semester: '2025-1'
  },
  {
    id: 3,
    name: '第二章测验',
    score: 78,
    rank: 18,
    status: '已发布',
    date: '2025-10-05',
    height: '78',
    color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    semester: '2025-1'
  },
  {
    id: 4,
    name: '第三章测验',
    score: 88,
    rank: 10,
    status: '已发布',
    date: '2025-10-15',
    height: '88',
    color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    semester: '2025-1'
  },
  {
    id: 5,
    name: '期中考试',
    score: 82,
    rank: 14,
    status: '已发布',
    date: '2024-11-20',
    height: '82',
    color: 'linear-gradient(135deg, #ffe58f 0%, #edf2fc 100%)',
    semester: '2024-2'
  },
  {
    id: 6,
    name: '期末考试',
    score: 90,
    rank: 8,
    status: '已发布',
    date: '2024-12-20',
    height: '90',
    color: 'linear-gradient(135deg, #87d068 0%, #3dc05e 100%)',
    semester: '2024-2'
  }
]);

const filteredExams = computed(() => semester.value === 'all' ? exams.value : exams.value.filter(e => e.semester === semester.value));
const averageScore = computed(() => {
  const list = filteredExams.value
  return list.length ? (list.reduce((s,e)=>s+e.score,0)/list.length).toFixed(1) : '0.0'
});
const maxScore = computed(() => Math.max(...filteredExams.value.map(e => e.score)));
const minScore = computed(() => Math.min(...filteredExams.value.map(e => e.score)));
const rank = computed(() => 12);
const totalStudents = computed(() => 45);
const getScoreColor = (score: number) => score >= 90 ? 'green' : score >= 80 ? 'blue' : score >= 70 ? 'orange' : score >= 60 ? 'red' : 'default';
const getRankClass = (r: number) => r <= 3 ? 'top' : r <= 10 ? 'good' : r <= 20 ? 'average' : 'low';
const getStatusColor = (status: string) => status === '已发布' ? 'success' : 'default';
const exportGrade = () => message.success('成绩单导出中...');
</script>

<style scoped>
.grade-section {
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

.header-actions {
  display: flex;
  align-items: center;
}

.grade-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.summary-card {
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eef5 100%);
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  transition: all 0.3s;
}

.summary-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-label {
  font-size: 13px;
  color: #999;
}

.summary-value {
  font-size: 32px;
  font-weight: bold;
  color: #333;
}

.summary-value.highlight {
  color: #52c41a;
}

.summary-value.low {
  color: #ff4d4f;
}

.summary-value.rank {
  color: #667eea;
}

.grade-chart {
  background: #fafafa;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.chart-legend {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #666;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend-color {
  width: 16px;
  height: 16px;
  border-radius: 4px;
}

.legend-color.blue {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.legend-color.green {
  background: #52c41a;
}

.chart-content {
  height: 300px;
  display: flex;
  align-items: flex-end;
  padding: 0 40px;
}

.chart-bars {
  display: flex;
  gap: 40px;
  height: 100%;
  align-items: flex-end;
  flex: 1;
}

.bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.bar-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.bar {
  width: 60px;
  min-height: 20px;
  border-radius: 8px 8px 0 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 8px;
  transition: all 0.3s;
}

.bar:hover {
  transform: scaleY(1.05);
}

.bar-label {
  color: #fff;
  font-weight: bold;
  font-size: 14px;
}

.bar-name {
  font-size: 12px;
  color: #666;
  text-align: center;
}

.grade-details {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}

.details-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.rank {
  font-weight: bold;
}

.rank.top {
  color: #f59e0b;
}

.rank.good {
  color: #52c41a;
}

.rank.average {
  color: #faad14;
}

.rank.low {
  color: #999;
}
</style>
