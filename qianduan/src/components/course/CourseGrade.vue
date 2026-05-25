<template>
  <div class="grade-section">
    <div class="section-header">
      <h3 class="section-title">成绩单</h3>
      <div class="header-actions" v-if="isAdmin">
        <a-select v-model:value="selectedExam" style="width: 180px" placeholder="选择测试">
          <a-select-option value="all">全部测试</a-select-option>
          <a-select-option v-for="exam in exams" :key="exam.id" :value="exam.id">
            {{ exam.title }}
          </a-select-option>
        </a-select>
      </div>
      <div class="header-actions" v-else>
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

    <!-- 学生视图 -->
    <div v-if="!isAdmin" class="student-view">
      <!-- 成绩概览 -->
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

      <!-- 成绩趋势图表 -->
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

      <!-- AI成绩分析 -->
      <div class="ai-analysis">
        <div class="analysis-header">
          <h4 class="analysis-title">🤖 AI成绩分析</h4>
          <a-button type="link" @click="refreshAnalysis">🔄 重新分析</a-button>
        </div>
        <div class="analysis-content">
          <div class="analysis-card">
            <h5 class="analysis-subtitle">📊 总体评价</h5>
            <p>{{ analysis.overall }}</p>
          </div>
          <div class="analysis-card">
            <h5 class="analysis-subtitle">📈 进步趋势</h5>
            <p>{{ analysis.trend }}</p>
          </div>
          <div class="analysis-card">
            <h5 class="analysis-subtitle">🎯 建议</h5>
            <p>{{ analysis.suggestions }}</p>
          </div>
        </div>
      </div>

      <!-- 成绩明细 -->
      <div class="grade-details">
        <h4 class="details-title">成绩明细</h4>
        <a-table 
          :columns="studentColumns" 
          :data-source="filteredExams" 
          :pagination="{ pageSize: 10 }"
          :row-key="(record) => record.id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'score'">
              <a-tag v-if="record.isExample" color="green">样例</a-tag>
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

    <!-- 管理员视图 -->
    <div v-else class="admin-view">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="allStudents" tab="所有学生成绩">
          <a-table 
            :columns="adminStudentColumns" 
            :data-source="allStudentGrades" 
            :pagination="{ pageSize: 10 }"
            :row-key="(record) => record.id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'avgScore'">
                <a-tag v-if="record.isExample" color="green">样例</a-tag>
                <a-tag :color="getScoreColor(record.avgScore)">
                  {{ record.avgScore }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" @click="viewStudentDetail(record)">查看详情</a-button>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="examStats" tab="测试统计">
          <div class="stats-grid">
            <div class="stat-card">
              <div class="stat-value">{{ examStats.totalExams }}</div>
              <div class="stat-label">测试总数</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ examStats.totalStudents }}</div>
              <div class="stat-label">参与学生</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ examStats.avgScore }}</div>
              <div class="stat-label">平均成绩</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ examStats.passRate }}%</div>
              <div class="stat-label">及格率</div>
            </div>
          </div>
          
          <div class="exam-ranking">
            <h4 class="ranking-title">测试排名</h4>
            <a-table 
              :columns="examRankingColumns" 
              :data-source="examRankings" 
              :pagination="{ pageSize: 5 }"
              :row-key="(record) => record.id"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'ranking'">
                  <a-tag v-if="record.isExample" color="green">样例</a-tag>
                  <span :class="['ranking-badge', getRankingClass(record.ranking)]">
                    {{ record.ranking }}
                  </span>
                </template>
              </template>
            </a-table>
          </div>
        </a-tab-pane>
        <a-tab-pane key="export" tab="成绩导出">
          <div class="export-section">
            <p class="export-desc">选择要导出的成绩数据</p>
            <a-space>
              <a-select v-model:value="exportType" style="width: 150px">
                <a-select-option value="all">全部成绩</a-select-option>
                <a-select-option value="current">当前测试</a-select-option>
                <a-select-option value="student">单个学生</a-select-option>
              </a-select>
              <a-select v-model:value="exportFormat" style="width: 120px">
                <a-select-option value="excel">Excel</a-select-option>
                <a-select-option value="pdf">PDF</a-select-option>
              </a-select>
              <a-button type="primary" @click="exportAllGrades">导出</a-button>
            </a-space>
          </div>
        </a-tab-pane>
      </a-tabs>
    </div>

    <!-- 学生详情弹窗 -->
    <a-modal 
      v-model:open="showStudentDetail" 
      :title="selectedStudent?.name + ' 的成绩详情'" 
      width="700px"
      :footer="null"
    >
      <div v-if="selectedStudent" class="student-detail">
        <div class="student-info">
          <p><strong>学号：</strong>{{ selectedStudent.id }}</p>
          <p><strong>姓名：</strong>{{ selectedStudent.name }}</p>
          <p><strong>平均分：</strong><span :class="getScoreColor(selectedStudent.avgScore)">{{ selectedStudent.avgScore }}</span></p>
          <p><strong>总测试次数：</strong>{{ selectedStudent.examCount }}</p>
        </div>
        
        <h4 class="detail-title">测试记录</h4>
        <a-table 
          :columns="studentColumns" 
          :data-source="selectedStudent.exams" 
          :pagination="false"
          :row-key="(record) => record.id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'score'">
              <a-tag v-if="record.isExample" color="green">样例</a-tag>
              <a-tag :color="getScoreColor(record.score)">
                {{ record.score }}
              </a-tag>
            </template>
          </template>
        </a-table>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { message } from 'ant-design-vue';

const isAdmin = ref(false);
const semester = ref('all');
const selectedExam = ref('all');
const activeTab = ref('allStudents');
const exportType = ref('all');
const exportFormat = ref('excel');
const showStudentDetail = ref(false);
const selectedStudent = ref<any>(null);

interface Exam {
  id: number;
  title: string;
  isExample?: boolean;
}

interface ExamRecord {
  id: number;
  name: string;
  score: number;
  rank: number;
  status: string;
  date: string;
  height: string;
  color: string;
  semester: string;
  isExample?: boolean;
}

interface StudentGrade {
  id: number;
  name: string;
  avgScore: number;
  examCount: number;
  exams: ExamRecord[];
  isExample?: boolean;
}

const exams = ref<Exam[]>([
  { id: 1, title: '第三章单元测试', isExample: true },
  { id: 2, title: '期中考试', isExample: true }
]);

const studentExams = ref<ExamRecord[]>([
  { id: 1, name: '期中考试', score: 85, rank: 12, status: '已发布', date: '2025-10-20', height: '85', color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', semester: '2025-1', isExample: true },
  { id: 2, name: '第一章测验', score: 92, rank: 5, status: '已发布', date: '2025-09-25', height: '92', color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', semester: '2025-1', isExample: true },
  { id: 3, name: '第二章测验', score: 78, rank: 18, status: '已发布', date: '2025-10-05', height: '78', color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', semester: '2025-1', isExample: true },
  { id: 4, name: '第三章测验', score: 88, rank: 10, status: '已发布', date: '2025-10-15', height: '88', color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', semester: '2025-1', isExample: true },
  { id: 5, name: '期中考试', score: 82, rank: 14, status: '已发布', date: '2024-11-20', height: '82', color: 'linear-gradient(135deg, #ffe58f 0%, #edf2fc 100%)', semester: '2024-2', isExample: true },
  { id: 6, name: '期末考试', score: 90, rank: 8, status: '已发布', date: '2024-12-20', height: '90', color: 'linear-gradient(135deg, #87d068 0%, #3dc05e 100%)', semester: '2024-2', isExample: true }
]);

const allStudentGrades = ref<StudentGrade[]>([
  { id: 1, name: '张同学', avgScore: 85, examCount: 6, exams: studentExams.value.slice(0, 3), isExample: true },
  { id: 2, name: '李同学', avgScore: 92, examCount: 6, exams: studentExams.value.slice(1, 4), isExample: true },
  { id: 3, name: '王同学', avgScore: 78, examCount: 5, exams: studentExams.value.slice(2, 5), isExample: true },
  { id: 4, name: '赵同学', avgScore: 88, examCount: 6, exams: studentExams.value.slice(3, 6), isExample: true },
  { id: 5, name: '刘同学', avgScore: 82, examCount: 4, exams: studentExams.value.slice(0, 2), isExample: true },
  { id: 6, name: '陈同学', avgScore: 90, examCount: 6, exams: studentExams.value.slice(1, 4), isExample: true }
]);

const examRankings = ref([
  { id: 1, ranking: 1, name: '李同学', score: 92, exam: '第三章单元测试', isExample: true },
  { id: 2, ranking: 2, name: '陈同学', score: 90, exam: '第三章单元测试', isExample: true },
  { id: 3, ranking: 3, name: '王同学', score: 88, exam: '第三章单元测试', isExample: true },
  { id: 4, ranking: 4, name: '张同学', score: 85, exam: '第三章单元测试', isExample: true },
  { id: 5, ranking: 5, name: '刘同学', score: 82, exam: '第三章单元测试', isExample: true }
]);

const examStats = ref({
  totalExams: 12,
  totalStudents: 45,
  avgScore: 85.6,
  passRate: 92
});

const analysis = ref({
  overall: '您的整体成绩表现良好，平均分达到85分以上，处于班级中上游水平。',
  trend: '从历次测试来看，您的成绩呈现稳步上升趋势，特别是在第三章测验中取得了明显进步。',
  suggestions: '建议重点复习第二章内容，您在该章节的测验成绩相对较低。同时可以多做一些综合应用题来提升解题能力。'
});

const filteredExams = computed(() => 
  semester.value === 'all' ? studentExams.value : studentExams.value.filter(e => e.semester === semester.value)
);

const averageScore = computed(() => {
  const list = filteredExams.value;
  return list.length ? (list.reduce((s,e)=>s+e.score,0)/list.length).toFixed(1) : '0.0';
});

const maxScore = computed(() => Math.max(...filteredExams.value.map(e => e.score)));
const minScore = computed(() => Math.min(...filteredExams.value.map(e => e.score)));
const rank = computed(() => 12);
const totalStudents = computed(() => 45);

const studentColumns = [
  { title: '考试名称', dataIndex: 'name', key: 'name' },
  { title: '考试日期', dataIndex: 'date', key: 'date' },
  { title: '成绩', dataIndex: 'score', key: 'score' },
  { title: '班级排名', dataIndex: 'rank', key: 'rank' },
  { title: '状态', dataIndex: 'status', key: 'status' }
];

const adminStudentColumns = [
  { title: '学号', dataIndex: 'id', key: 'id' },
  { title: '姓名', dataIndex: 'name', key: 'name' },
  { title: '平均分', dataIndex: 'avgScore', key: 'avgScore' },
  { title: '测试次数', dataIndex: 'examCount', key: 'examCount' },
  { title: '操作', dataIndex: 'action', key: 'action' }
];

const examRankingColumns = [
  { title: '排名', dataIndex: 'ranking', key: 'ranking' },
  { title: '姓名', dataIndex: 'name', key: 'name' },
  { title: '成绩', dataIndex: 'score', key: 'score' },
  { title: '测试', dataIndex: 'exam', key: 'exam' }
];

const getScoreColor = (score: number) => 
  score >= 90 ? 'green' : score >= 80 ? 'blue' : score >= 70 ? 'orange' : score >= 60 ? 'red' : 'default';

const getRankClass = (r: number) => 
  r <= 3 ? 'top' : r <= 10 ? 'good' : r <= 20 ? 'average' : 'low';

const getStatusColor = (status: string) => 
  status === '已发布' ? 'success' : 'default';

const getRankingClass = (r: number) => 
  r <= 3 ? 'top-rank' : r <= 10 ? 'good-rank' : 'normal-rank';

const exportGrade = () => message.success('成绩单导出中...');

const refreshAnalysis = () => {
  message.loading('AI正在分析...', 2);
  setTimeout(() => {
    message.success('分析完成');
  }, 2000);
};

const viewStudentDetail = (student: StudentGrade) => {
  selectedStudent.value = student;
  showStudentDetail.value = true;
};

const exportAllGrades = () => {
  message.success(`正在导出${exportFormat.value === 'excel' ? 'Excel' : 'PDF'}文件...`);
};
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

/* 学生视图 */
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

.summary-value.highlight { color: #52c41a; }
.summary-value.low { color: #ff4d4f; }
.summary-value.rank { color: #667eea; }

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

.legend-color.blue { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.legend-color.green { background: #52c41a; }

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

.bar:hover { transform: scaleY(1.05); }

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

/* AI分析 */
.ai-analysis {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
}

.analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.analysis-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
}

.analysis-content {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.analysis-card {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  padding: 16px;
}

.analysis-subtitle {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}

.analysis-card p {
  margin: 0;
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  line-height: 1.6;
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

.rank { font-weight: bold; }
.rank.top { color: #f59e0b; }
.rank.good { color: #52c41a; }
.rank.average { color: #faad14; }
.rank.low { color: #999; }

/* 管理员视图 */
.admin-view {
  height: calc(100vh - 200px);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eef5 100%);
  border-radius: 12px;
  padding: 24px;
  text-align: center;
}

.stat-value {
  font-size: 36px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.exam-ranking {
  background: #fafafa;
  border-radius: 12px;
  padding: 24px;
}

.ranking-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.ranking-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  font-weight: bold;
  font-size: 14px;
}

.ranking-badge.top-rank {
  background: #fff1f0;
  color: #ff4d4f;
}

.ranking-badge.good-rank {
  background: #fffbe6;
  color: #faad14;
}

.ranking-badge.normal-rank {
  background: #f6ffed;
  color: #52c41a;
}

.export-section {
  padding: 24px;
  background: #fafafa;
  border-radius: 12px;
}

.export-desc {
  margin: 0 0 16px 0;
  color: #666;
}

/* 学生详情弹窗 */
.student-detail {
  max-height: 500px;
  overflow-y: auto;
}

.student-info {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  margin-bottom: 16px;
}

.student-info p {
  margin: 8px 0;
  color: #333;
}

.detail-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}
</style>