<template>
  <div class="test-section">
    <div class="section-header">
      <h3 class="section-title">测试</h3>
      <div class="section-actions" v-if="isAdmin">
        <a-space>
          <a-button type="primary" @click="showCreateExamModal = true">发布测试</a-button>
          <a-button @click="showEditQuestionsModal = true">编辑题目</a-button>
        </a-space>
      </div>
    </div>

    <!-- 学生视图：上传资料生成题目 -->
    <div v-if="!isAdmin" class="student-panel">
      <div class="upload-section">
        <h4 class="panel-title">📤 上传资料生成测试题</h4>
        <p class="panel-desc">上传PDF或Word文档，智能助手将为您生成相应的测试题目</p>
        
        <div class="upload-area" @click="triggerFileUpload" @dragover.prevent @drop.prevent="handleDrop">
          <div class="upload-icon">📁</div>
          <p>点击或拖拽文件到此处上传</p>
          <p class="upload-hint">支持 PDF、Word (.docx) 格式</p>
          <input type="file" ref="fileInput" class="file-input" accept=".pdf,.docx" @change="handleFileSelect" />
        </div>

        <div v-if="uploadedFiles.length > 0" class="uploaded-files">
          <div v-for="file in uploadedFiles" :key="file.name" class="uploaded-file">
            <span class="file-icon">{{ getFileIcon(file.name) }}</span>
            <span class="file-name">{{ file.name }}</span>
            <a-button type="text" danger @click="removeFile(file.name)">删除</a-button>
          </div>
          <a-button type="primary" @click="generateQuestions">✨ 生成测试题</a-button>
        </div>
      </div>

      <!-- 我的测试记录 -->
      <div class="my-exams">
        <h4 class="panel-title">📝 我的测试记录</h4>
        <a-table 
          :columns="examColumns" 
          :data-source="myExams" 
          :pagination="{ pageSize: 5 }"
          :row-key="(record) => record.id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'">
              <a-button type="link" @click="viewExam(record)">查看</a-button>
              <a-button type="link" @click="retryExam(record)">重测</a-button>
            </template>
            <template v-else-if="column.key === 'score'">
              <a-tag v-if="record.isExample" color="green">样例</a-tag>
              <a-tag :color="getScoreColor(record.score)">
                {{ record.score }}分
              </a-tag>
            </template>
          </template>
        </a-table>
      </div>
    </div>

    <!-- 管理员视图：测试管理 -->
    <div v-else class="admin-panel">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="exams" tab="测试列表">
          <a-button type="primary" @click="showCreateExamModal = true" style="margin-bottom: 16px;">
            + 新建测试
          </a-button>
          <a-table 
            :columns="adminExamColumns" 
            :data-source="exams" 
            :pagination="{ pageSize: 10 }"
            :row-key="(record) => record.id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag v-if="record.isExample" color="green">样例</a-tag>
                <a-tag :color="record.status === 'published' ? 'success' : 'warning'">
                  {{ record.status === 'published' ? '已发布' : '草稿' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" @click="editExam(record)">编辑</a-button>
                <a-button type="link" @click="publishExam(record)">
                  {{ record.status === 'published' ? '取消发布' : '发布' }}
                </a-button>
                <a-button type="link" danger @click="deleteExam(record.id)">删除</a-button>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="questions" tab="题库管理">
          <a-button type="primary" @click="showAddQuestionModal = true" style="margin-bottom: 16px;">
            + 添加题目
          </a-button>
          <a-table 
            :columns="questionColumns" 
            :data-source="questions" 
            :pagination="{ pageSize: 10 }"
            :row-key="(record) => record.id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'type'">
                <a-tag v-if="record.isExample" color="green">样例</a-tag>
                <a-tag :color="getQuestionTypeColor(record.type)">
                  {{ getQuestionTypeLabel(record.type) }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'content'">
                <div class="question-content">
                  <div v-if="record.content" class="latex-content" v-html="renderLatex(record.content)"></div>
                  <img v-if="record.imageUrl" :src="record.imageUrl" class="question-image" />
                </div>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" @click="editQuestion(record)">编辑</a-button>
                <a-button type="link" danger @click="deleteQuestion(record.id)">删除</a-button>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </div>

    <!-- 考试详情弹窗 -->
    <a-modal 
      v-model:open="showExamDetailModal" 
      :title="currentExam?.title || '测试详情'" 
      width="800px"
      :footer="null"
    >
      <div v-if="currentExam" class="exam-detail">
        <div class="exam-info">
          <p><strong>测试名称：</strong>{{ currentExam.title }}</p>
          <p><strong>描述：</strong>{{ currentExam.description }}</p>
          <p><strong>题目数量：</strong>{{ currentExam.questionCount }}题</p>
          <p><strong>总分：</strong>{{ currentExam.totalScore }}分</p>
        </div>
        
        <div class="questions-list">
          <div 
            v-for="(question, idx) in currentExam.questions" 
            :key="question.id" 
            class="question-item"
          >
            <div class="question-header">
              <span class="question-number">{{ idx + 1 }}.</span>
              <span class="question-type">{{ getQuestionTypeLabel(question.type) }}</span>
              <span class="question-score">({{ question.score }}分)</span>
            </div>
            
            <div class="question-body">
              <div v-if="question.content" class="latex-content" v-html="renderLatex(question.content)"></div>
              <img v-if="question.imageUrl" :src="question.imageUrl" class="question-image" />
              
              <!-- 选择题 -->
              <div v-if="question.type === 'choice'" class="options">
                <div 
                  v-for="(option, optIdx) in question.options" 
                  :key="optIdx"
                  :class="['option', { selected: userAnswers[question.id] === optIdx, correct: showResult && optIdx === question.answer }]"
                  @click="selectOption(question.id, optIdx)"
                >
                  {{ ['A', 'B', 'C', 'D'][optIdx] }}. {{ option }}
                </div>
              </div>
              
              <!-- 填空题 -->
              <div v-if="question.type === 'fill'" class="fill-input">
                <a-input 
                  v-model:value="userAnswers[question.id]" 
                  placeholder="请输入答案"
                />
              </div>
              
              <!-- 大题 -->
              <div v-if="question.type === 'essay'" class="essay-input">
                <a-textarea 
                  v-model:value="userAnswers[question.id]" 
                  :rows="6" 
                  placeholder="请输入答案"
                />
                <div class="self-score" v-if="!showResult">
                  <a-input-number 
                    v-model:value="selfScores[question.id]" 
                    :min="0" 
                    :max="question.score" 
                    style="width: 100px"
                  />
                  <span class="score-label">自评分数 (0-{{ question.score }})</span>
                </div>
              </div>
            </div>
            
            <div v-if="showResult" class="question-result">
              <div v-if="question.type === 'choice' || question.type === 'fill'">
                <span class="result-label">标准答案：</span>
                <span class="correct-answer">
                  {{ question.type === 'choice' ? ['A', 'B', 'C', 'D'][question.answer] : question.answer }}
                </span>
              </div>
              <div v-if="question.type === 'essay'">
                <span class="result-label">您的得分：</span>
                <span class="user-score">{{ selfScores[question.id] || 0 }}/{{ question.score }}</span>
              </div>
            </div>
          </div>
        </div>
        
        <div class="exam-actions">
          <a-button type="primary" @click="submitExam">提交答案</a-button>
          <a-button @click="showExamDetailModal = false">关闭</a-button>
        </div>
      </div>
    </a-modal>

    <!-- 添加/编辑题目弹窗 -->
    <a-modal 
      v-model:open="showAddQuestionModal" 
      :title="editingQuestion ? '编辑题目' : '添加题目'" 
      width="700px"
      :footer="null"
    >
      <a-form :model="questionForm" layout="vertical">
        <a-form-item label="题目类型" required>
          <a-select v-model:value="questionForm.type">
            <a-select-option value="choice">选择题</a-select-option>
            <a-select-option value="fill">填空题</a-select-option>
            <a-select-option value="essay">大题</a-select-option>
          </a-select>
        </a-form-item>
        
        <a-form-item label="题目内容（支持Latex公式，用$包围）" required>
          <a-textarea v-model:value="questionForm.content" :rows="4" placeholder="如：$E = mc^2$" />
        </a-form-item>
        
        <a-form-item label="题目图片（可选）">
          <a-upload :show-upload-list="false" @change="handleImageUpload">
            <a-button>上传图片</a-button>
          </a-upload>
          <img v-if="questionForm.imageUrl" :src="questionForm.imageUrl" class="preview-image" />
        </a-form-item>
        
        <a-form-item label="选项（选择题专用）" v-if="questionForm.type === 'choice'">
          <div v-for="(opt, idx) in questionForm.options" :key="idx" class="option-input">
            <a-input v-model:value="questionForm.options[idx]" :placeholder="['A', 'B', 'C', 'D'][idx]" />
          </div>
        </a-form-item>
        
        <a-form-item label="标准答案" required>
          <template v-if="questionForm.type === 'choice'">
            <a-select v-model:value="questionForm.answer">
              <a-select-option :value="0">A</a-select-option>
              <a-select-option :value="1">B</a-select-option>
              <a-select-option :value="2">C</a-select-option>
              <a-select-option :value="3">D</a-select-option>
            </a-select>
          </template>
          <a-input v-else v-model:value="questionForm.answer" placeholder="输入标准答案" />
        </a-form-item>
        
        <a-form-item label="分值" required>
          <a-input-number v-model:value="questionForm.score" :min="1" :max="100" />
        </a-form-item>
        
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="saveQuestion">保存</a-button>
            <a-button @click="cancelQuestionEdit">取消</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'

const isAdmin = ref(false)
const activeTab = ref('exams')
const showCreateExamModal = ref(false)
const showEditQuestionsModal = ref(false)
const showAddQuestionModal = ref(false)
const showExamDetailModal = ref(false)
const currentExam = ref<Exam | null>(null)
const editingQuestion = ref<Question | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)

interface Question {
  id: number
  type: 'choice' | 'fill' | 'essay'
  content: string
  imageUrl?: string
  options?: string[]
  answer: string | number
  score: number
  isExample?: boolean
}

interface Exam {
  id: number
  title: string
  description: string
  questionCount: number
  totalScore: number
  questions: Question[]
  status: 'draft' | 'published'
  createdAt: string
  isExample?: boolean
}

const userAnswers = reactive<Record<number, string | number>>({})
const selfScores = reactive<Record<number, number>>({})
const showResult = ref(false)
const uploadedFiles = ref<File[]>([])

const questionForm = reactive({
  type: 'choice',
  content: '',
  imageUrl: '',
  options: ['', '', '', ''],
  answer: '',
  score: 10
})

const questions = ref<Question[]>([
  {
    id: 1,
    type: 'choice',
    content: '下列哪个是麦克斯韦方程组中的高斯定理（电场）？',
    options: ['$\\oint_S \\vec{E} \\cdot d\\vec{S} = \\frac{q}{\\epsilon_0}$', '$\\oint_C \\vec{E} \\cdot d\\vec{l} = -\\frac{d\\Phi_B}{dt}$', '$\\oint_S \\vec{B} \\cdot d\\vec{S} = 0$', '$\\oint_C \\vec{B} \\cdot d\\vec{l} = \\mu_0 I + \\mu_0 \\epsilon_0 \\frac{d\\Phi_E}{dt}$'],
    answer: 0,
    score: 10,
    isExample: true
  },
  {
    id: 2,
    type: 'fill',
    content: '真空中的光速 $c = $ ______ $m/s$（保留三位有效数字）',
    answer: '3.00×10^8',
    score: 10,
    isExample: true
  },
  {
    id: 3,
    type: 'essay',
    content: '请简述位移电流的物理意义及其与传导电流的区别。',
    answer: '',
    score: 20,
    isExample: true
  },
  {
    id: 4,
    type: 'choice',
    content: '电场强度 $\\vec{E}$ 的单位是？',
    options: ['N/C', 'V/m', 'Both A and B', 'None of above'],
    answer: 2,
    score: 10,
    isExample: true
  }
])

const exams = ref<Exam[]>([
  {
    id: 1,
    title: '第三章单元测试',
    description: '电磁场基本方程相关测试',
    questionCount: 10,
    totalScore: 100,
    questions: questions.value.slice(0, 4),
    status: 'published',
    createdAt: '2025-12-01',
    isExample: true
  },
  {
    id: 2,
    title: '期中考试',
    description: '涵盖第1-4章内容',
    questionCount: 20,
    totalScore: 100,
    questions: [],
    status: 'draft',
    createdAt: '2025-12-10',
    isExample: true
  }
])

const myExams = ref([
  { id: 1, name: '第三章单元测试', date: '2025-12-05', score: 85, status: '已完成', isExample: true },
  { id: 2, name: '第二章单元测试', date: '2025-11-20', score: 78, status: '已完成', isExample: true },
  { id: 3, name: '第一章单元测试', date: '2025-11-05', score: 92, status: '已完成', isExample: true }
])

const examColumns = [
  { title: '测试名称', dataIndex: 'name', key: 'name' },
  { title: '测试日期', dataIndex: 'date', key: 'date' },
  { title: '得分', dataIndex: 'score', key: 'score' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', dataIndex: 'action', key: 'action' }
]

const adminExamColumns = [
  { title: '测试名称', dataIndex: 'title', key: 'title' },
  { title: '题目数', dataIndex: 'questionCount', key: 'questionCount' },
  { title: '总分', dataIndex: 'totalScore', key: 'totalScore' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
  { title: '操作', dataIndex: 'action', key: 'action' }
]

const questionColumns = [
  { title: '类型', dataIndex: 'type', key: 'type' },
  { title: '内容', dataIndex: 'content', key: 'content' },
  { title: '分值', dataIndex: 'score', key: 'score' },
  { title: '操作', dataIndex: 'action', key: 'action' }
]

const getQuestionTypeColor = (type: string) => ({
  choice: 'blue',
  fill: 'orange',
  essay: 'green'
}[type] || 'default')

const getQuestionTypeLabel = (type: string) => ({
  choice: '选择题',
  fill: '填空题',
  essay: '大题'
}[type] || type)

const getScoreColor = (score: number) => 
  score >= 90 ? 'green' : score >= 80 ? 'blue' : score >= 60 ? 'orange' : 'red'

const getFileIcon = (name: string) => {
  if (name.endsWith('.pdf')) return '📕'
  if (name.endsWith('.docx')) return '📘'
  return '📄'
}

const triggerFileUpload = () => fileInput.value?.click()

const handleFileSelect = (e: Event) => {
  const target = e.target as HTMLInputElement
  if (target.files?.length) {
    uploadedFiles.value.push(...Array.from(target.files))
    message.success('文件上传成功')
  }
}

const handleDrop = (e: DragEvent) => {
  const files = e.dataTransfer?.files
  if (files?.length) {
    uploadedFiles.value.push(...Array.from(files).filter(f => 
      f.name.endsWith('.pdf') || f.name.endsWith('.docx')
    ))
    message.success('文件上传成功')
  }
}

const removeFile = (name: string) => {
  uploadedFiles.value = uploadedFiles.value.filter(f => f.name !== name)
}

const generateQuestions = () => {
  message.success('正在生成测试题...')
  setTimeout(() => {
    message.success('测试题生成成功！')
    uploadedFiles.value = []
  }, 1500)
}

const viewExam = (exam: any) => {
  const examData = exams.value.find(e => e.id === exam.id)
  if (examData) {
    currentExam.value = examData
    showExamDetailModal.value = true
    showResult.value = false
    Object.keys(userAnswers).forEach(k => delete userAnswers[k])
    Object.keys(selfScores).forEach(k => delete selfScores[k])
  }
}

const retryExam = (exam: any) => {
  viewExam(exam)
}

const selectOption = (questionId: number, optIdx: number) => {
  userAnswers[questionId] = optIdx
}

const submitExam = () => {
  showResult.value = true
  message.success('答案已提交，您可以查看结果')
}

const editQuestion = (question: Question) => {
  editingQuestion.value = question
  questionForm.type = question.type
  questionForm.content = question.content
  questionForm.imageUrl = question.imageUrl || ''
  questionForm.options = question.options || ['', '', '', '']
  questionForm.answer = question.answer
  questionForm.score = question.score
  showAddQuestionModal.value = true
}

const deleteQuestion = (id: number) => {
  questions.value = questions.value.filter(q => q.id !== id)
  message.success('题目已删除')
}

const saveQuestion = () => {
  if (!questionForm.content || !questionForm.answer) {
    message.warning('请填写完整信息')
    return
  }
  
  if (editingQuestion.value) {
    const idx = questions.value.findIndex(q => q.id === editingQuestion.value!.id)
    if (idx !== -1) {
      questions.value[idx] = {
        id: editingQuestion.value.id,
        type: questionForm.type as 'choice' | 'fill' | 'essay',
        content: questionForm.content,
        imageUrl: questionForm.imageUrl || undefined,
        options: questionForm.type === 'choice' ? questionForm.options : undefined,
        answer: questionForm.answer,
        score: questionForm.score
      }
    }
    message.success('题目已更新')
  } else {
    questions.value.push({
      id: Date.now(),
      type: questionForm.type as 'choice' | 'fill' | 'essay',
      content: questionForm.content,
      imageUrl: questionForm.imageUrl || undefined,
      options: questionForm.type === 'choice' ? questionForm.options : undefined,
      answer: questionForm.answer,
      score: questionForm.score
    })
    message.success('题目已添加')
  }
  
  cancelQuestionEdit()
}

const cancelQuestionEdit = () => {
  showAddQuestionModal.value = false
  editingQuestion.value = null
  questionForm.type = 'choice'
  questionForm.content = ''
  questionForm.imageUrl = ''
  questionForm.options = ['', '', '', '']
  questionForm.answer = ''
  questionForm.score = 10
}

const handleImageUpload = (info: any) => {
  if (info.file.status === 'done') {
    questionForm.imageUrl = info.file.response?.url || 'https://via.placeholder.com/300x200'
    message.success('图片上传成功')
  }
}

const renderLatex = (content: string) => {
  return content.replace(/\$([^$]+)\$/g, '<span class="latex">$1</span>')
}

const editExam = (exam: Exam) => {
  currentExam.value = exam
  showExamDetailModal.value = true
}

const publishExam = (exam: Exam) => {
  exam.status = exam.status === 'published' ? 'draft' : 'published'
  message.success(exam.status === 'published' ? '测试已发布' : '已取消发布')
}

const deleteExam = (id: number) => {
  exams.value = exams.value.filter(e => e.id !== id)
  message.success('测试已删除')
}
</script>

<style scoped>
.test-section {
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

/* 学生面板 */
.student-panel {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 24px;
}

.upload-section, .my-exams {
  background: #fafafa;
  border-radius: 12px;
  padding: 24px;
}

.panel-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.panel-desc {
  margin: 0 0 16px 0;
  color: #666;
  font-size: 14px;
}

.upload-area {
  border: 2px dashed #d9d9d9;
  border-radius: 12px;
  padding: 32px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-area:hover {
  border-color: #667eea;
  background: #f0f5ff;
}

.upload-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.upload-hint {
  color: #999;
  font-size: 12px;
  margin-top: 8px;
}

.file-input {
  display: none;
}

.uploaded-files {
  margin-top: 16px;
}

.uploaded-file {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 8px;
}

.file-icon {
  font-size: 24px;
}

.file-name {
  flex: 1;
  font-size: 14px;
}

/* 考试详情 */
.exam-detail {
  max-height: 600px;
  overflow-y: auto;
}

.exam-info {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.question-item {
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.question-number {
  font-weight: bold;
  color: #333;
}

.question-type {
  font-size: 12px;
  color: #666;
}

.question-score {
  margin-left: auto;
  color: #667eea;
  font-weight: 600;
}

.latex-content {
  font-size: 15px;
  line-height: 1.8;
  color: #333;
}

.latex-content .latex {
  font-family: 'Times New Roman', serif;
  font-style: italic;
  color: #667eea;
}

.question-image {
  max-width: 100%;
  border-radius: 8px;
  margin-top: 12px;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.option {
  padding: 12px 16px;
  background: #fff;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.option:hover {
  border-color: #667eea;
}

.option.selected {
  border-color: #667eea;
  background: #f0f5ff;
}

.option.correct {
  border-color: #52c41a;
  background: #f6ffed;
}

.fill-input {
  margin-top: 12px;
}

.essay-input {
  margin-top: 12px;
}

.self-score {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.score-label {
  font-size: 14px;
  color: #666;
}

.question-result {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #ddd;
}

.result-label {
  font-weight: 600;
  color: #333;
}

.correct-answer {
  color: #52c41a;
  font-weight: 600;
}

.user-score {
  color: #667eea;
  font-weight: 600;
}

.exam-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

/* 添加题目弹窗 */
.preview-image {
  max-width: 100%;
  max-height: 200px;
  border-radius: 8px;
  margin-top: 12px;
}

.option-input {
  margin-bottom: 8px;
}

/* 管理员面板 */
.admin-panel {
  height: calc(100vh - 200px);
}

.question-content {
  max-width: 400px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>