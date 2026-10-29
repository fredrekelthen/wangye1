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

    <div v-if="!isAdmin" class="student-panel">
      <div class="upload-section">
        <h4 class="panel-title">📤 上传资料生成测试题</h4>
        <p class="panel-desc">上传PDF或Word文档，智能助手将为您生成相应的测试题目（6-9题，包含选择题和大题）</p>
        
        <div class="upload-area" @click="triggerFileUpload" @dragover.prevent @drop.prevent="handleDrop">
          <div class="upload-icon">📁</div>
          <p>点击或拖拽文件到此处上传</p>
          <p class="upload-hint">支持 PDF、Word (.docx) 格式，单个文件不超过 10MB</p>
          <input type="file" ref="fileInput" class="file-input" accept=".pdf,.docx" @change="handleFileSelect" />
        </div>

        <div v-if="uploadedFiles.length > 0" class="uploaded-files">
          <div v-for="file in uploadedFiles" :key="file.name" class="uploaded-file">
            <span class="file-icon">{{ getFileIcon(file.name) }}</span>
            <span class="file-name">{{ file.name }}</span>
            <span class="file-size">{{ formatFileSize(file.size) }}</span>
            <a-button type="text" danger @click="removeFile(file.name)">删除</a-button>
          </div>
          
          <div class="difficulty-select">
            <span class="difficulty-label">选择难度：</span>
            <a-select v-model:value="selectedDifficulty" style="width: 150px">
              <a-select-option value="basic">基础</a-select-option>
              <a-select-option value="medium">中等</a-select-option>
              <a-select-option value="advanced">综合</a-select-option>
            </a-select>
          </div>
          
          <a-button type="primary" @click="generateQuestions" :loading="isGenerating">
            ✨ 生成测试题
          </a-button>
        </div>

        <div v-if="generatedQuestions.length > 0" class="generated-questions">
          <div class="success-message">
            <div class="success-icon">✅</div>
            <h4 class="panel-title">测试题生成成功！</h4>
            <p class="success-desc">共生成 {{ generatedQuestions.length }} 道题目，点击下方按钮开始测试</p>
            <div class="question-stats">
              <span class="stat-item">选择题：{{ generatedQuestions.filter(q => q.type === 'choice').length }} 题</span>
              <span class="stat-item">填空题：{{ generatedQuestions.filter(q => q.type === 'fill').length }} 题</span>
              <span class="stat-item">大题：{{ generatedQuestions.filter(q => q.type === 'essay').length }} 题</span>
              <span class="stat-item">总分：{{ generatedQuestions.reduce((sum, q) => sum + q.score, 0) }} 分</span>
            </div>
          </div>
          <a-button type="primary" size="large" @click="startGeneratedExam">开始测试</a-button>
        </div>
      </div>

      <div class="my-exams">
        <h4 class="panel-title">📝 我的测试记录</h4>
        <a-table 
          :columns="examColumns" 
          :data-source="myExams" 
          :pagination="{ pageSize: 5 }"
          :row-key="(record) => record.id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <template v-if="editingExamName === record.id">
                <a-input 
                  v-model:value="editingNameValue" 
                  style="width: 200px"
                  @keyup.enter="saveExamName(record)"
                />
                <a-button type="text" size="small" @click="saveExamName(record)">保存</a-button>
                <a-button type="text" size="small" @click="cancelEditExamName">取消</a-button>
              </template>
              <template v-else>
                {{ record.name }}
                <a-button type="text" size="small" @click="startEditExamName(record)" v-if="!record.isExample">编辑</a-button>
              </template>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" @click="viewExam(record)">查看</a-button>
              <a-button type="link" @click="retryExam(record)">重测</a-button>
              <a-button type="link" danger @click="deleteExamHistory(record)" v-if="!record.isExample">删除</a-button>
            </template>
            <template v-else-if="column.key === 'score'">
              <a-tag v-if="record.isExample" color="green">样例</a-tag>
              <a-tag :color="getScoreColor(record.score)">
                {{ record.score }}/{{ record.maxScore || 100 }}分
              </a-tag>
            </template>
          </template>
        </a-table>
      </div>
    </div>

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
                  <span v-if="record.content" v-html="renderLatexContent(record.content)"></span>
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

    <a-modal 
      v-model:open="showExamDetailModal" 
      :title="currentExam?.title || '测试详情'" 
      width="900px"
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
              <span v-if="question.content" v-html="renderLatexContent(question.content)"></span>
              <img v-if="question.imageUrl" :src="question.imageUrl" class="question-image" />
              
              <div v-if="question.type === 'choice'" class="options">
                <div 
                  v-for="(option, optIdx) in question.options" 
                  :key="optIdx"
                  :class="['option', { selected: userAnswers[question.id] === optIdx, correct: showResult && optIdx === question.answer }]"
                  @click="selectOption(question.id, optIdx)"
                >
                  {{ ['A', 'B', 'C', 'D'][optIdx] }}. 
                  <span v-html="renderLatexContent(option)"></span>
                </div>
              </div>
              
              <div v-if="question.type === 'fill'" class="fill-input">
                <a-input 
                  v-model:value="userAnswers[question.id]" 
                  placeholder="请输入答案（支持数学公式，如：3.00×10^8 或 πr²）"
                />
              </div>
              
              <div v-if="question.type === 'essay'" class="essay-input">
                <a-textarea 
                  v-model:value="userAnswers[question.id]" 
                  :rows="8" 
                  placeholder="请详细回答上述问题..."
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
              <div v-if="question.type === 'choice'">
                <span class="result-label">标准答案：</span>
                <span class="correct-answer">
                  {{ ['A', 'B', 'C', 'D'][question.answer] }}
                  <span v-html="renderLatexContent(question.options?.[question.answer] || '')"></span>
                </span>
              </div>
              <div v-if="question.type === 'fill'">
                <span class="result-label">标准答案：</span>
                <span class="correct-answer">{{ question.answer }}</span>
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

    <a-modal 
      v-model:open="showAddQuestionModal" 
      :title="editingQuestion ? '编辑题目' : '添加题目'" 
      width="800px"
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
        
        <a-form-item label="题目内容（支持LaTeX数学公式，用$包围）" required>
          <a-textarea v-model:value="questionForm.content" :rows="4" placeholder="如：计算 $\vec{E} \cdot \vec{B}$ 的值" />
          <p class="form-hint">提示：使用 $...$ 包围数学公式，例如 $E = mc^2$ 或 $\frac{1}{2}mv^2$</p>
        </a-form-item>
        
        <a-form-item label="题目图片（可选）">
          <a-upload :show-upload-list="false" @change="handleImageUpload">
            <a-button>上传图片</a-button>
          </a-upload>
          <img v-if="questionForm.imageUrl" :src="questionForm.imageUrl" class="preview-image" />
        </a-form-item>
        
        <a-form-item label="选项（选择题专用）" v-if="questionForm.type === 'choice'">
          <div v-for="(opt, idx) in questionForm.options" :key="idx" class="option-input">
            <a-input v-model:value="questionForm.options[idx]" :placeholder="['A', 'B', 'C', 'D'][idx] + '. 选项内容（支持LaTeX公式）'" />
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
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'

const router = useRouter()

const isAdmin = ref(false)
const activeTab = ref('exams')
const showCreateExamModal = ref(false)
const showEditQuestionsModal = ref(false)
const showAddQuestionModal = ref(false)
const showExamDetailModal = ref(false)
const currentExam = ref<Exam | null>(null)
const editingQuestion = ref<Question | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const isGenerating = ref(false)

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
const generatedQuestions = ref<Question[]>([])
const selectedDifficulty = ref<'basic' | 'medium' | 'advanced'>('basic')

const questionForm = reactive({
  type: 'choice',
  content: '',
  imageUrl: '',
  options: ['', '', '', ''],
  answer: '',
  score: 10
})

const mockBasicQuestions = [
  {
    id: 2001,
    type: 'choice' as const,
    content: '电场强度的定义式是？',
    options: [
      '$\\vec{E} = \\frac{\\vec{F}}{q}$',
      '$\\vec{E} = \\frac{kq}{r^2}\\hat{r}$',
      '$\\vec{E} = \\rho\\vec{r}$',
      '$\\vec{E} = \\mu_0\\vec{H}$'
    ],
    answer: 0,
    score: 10
  },
  {
    id: 2002,
    type: 'choice' as const,
    content: '以下哪个是库仑定律的正确表达式？',
    options: [
      '$F = k\\frac{q_1q_2}{r^2}$',
      '$F = k\\frac{q_1q_2}{r}$',
      '$F = kq_1q_2r^2$',
      '$F = \\frac{k}{q_1q_2r^2}$'
    ],
    answer: 0,
    score: 10
  },
  {
    id: 2003,
    type: 'choice' as const,
    content: '电通量的单位是？',
    options: [
      '$N\\cdot m^2/C$',
      '$N/C$',
      '$C/m^2$',
      '$V/m$'
    ],
    answer: 0,
    score: 10
  },
  {
    id: 2004,
    type: 'fill' as const,
    content: '真空中的介电常数 $\\epsilon_0$ 约等于 ______ $F/m$（保留两位有效数字）',
    answer: '$8.85 \\times 10^{-12}$',
    score: 10
  },
  {
    id: 2005,
    type: 'choice' as const,
    content: '高斯定理的数学表达式是？',
    options: [
      '$\\oint_S \\vec{E} \\cdot d\\vec{S} = \\frac{q_{enclosed}}{\\epsilon_0}$',
      '$\\oint_C \\vec{E} \\cdot d\\vec{l} = 0$',
      '$\\oint_S \\vec{B} \\cdot d\\vec{S} = 0$',
      '$\\oint_C \\vec{B} \\cdot d\\vec{l} = \\mu_0 I$'
    ],
    answer: 0,
    score: 10
  },
  {
    id: 2006,
    type: 'choice' as const,
    content: '点电荷的电场强度与距离的关系是？',
    options: [
      '与 $r^2$ 成反比',
      '与 $r$ 成反比',
      '与 $r^2$ 成正比',
      '与 $r$ 成正比'
    ],
    answer: 0,
    score: 10
  },
  {
    id: 2007,
    type: 'fill' as const,
    content: '电场强度的方向与正电荷受力方向 ______（填"相同"或"相反"）',
    answer: '相同',
    score: 10
  },
  {
    id: 2008,
    type: 'essay' as const,
    content: '请简述电场强度的定义及其物理意义。',
    answer: '',
    score: 20
  }
]

const mockMediumQuestions = [
  {
    id: 3001,
    type: 'choice' as const,
    content: '根据高斯定理，通过闭合曲面的电通量 $$\\Phi_E = \\oint_S \\vec{E} \\cdot d\\vec{S}$$ 等于？',
    options: [
      '$$\\frac{q_{enclosed}}{\\epsilon_0}$$',
      '$$\\mu_0 I_{enclosed}$$',
      '$$-\\frac{d\\Phi_B}{dt}$$',
      '$$\\frac{d\\Phi_E}{dt}$$'
    ],
    answer: 0,
    score: 10
  },
  {
    id: 3002,
    type: 'choice' as const,
    content: '位移电流的表达式为 $$I_d = $$？',
    options: [
      '$$\\epsilon_0 \\frac{d\\Phi_E}{dt}$$',
      '$$\\frac{V}{R}$$',
      '$$\\rho v$$',
      '$$\\sigma E$$'
    ],
    answer: 0,
    score: 10
  },
  {
    id: 3003,
    type: 'choice' as const,
    content: '真空中麦克斯韦方程组的旋度方程表明？',
    options: [
      '变化的电场产生磁场',
      '变化的磁场产生电场',
      '电荷产生电场',
      '磁场总是无源的'
    ],
    answer: 0,
    score: 10
  },
  {
    id: 3004,
    type: 'fill' as const,
    content: '坡印廷矢量 $$\\vec{S} = $$ ______，它表示电磁能流密度。',
    answer: '$$\\frac{1}{\\mu_0} \\vec{E} \\times \\vec{B}$$',
    score: 10
  },
  {
    id: 3005,
    type: 'choice' as const,
    content: '平面电磁波在真空中的传播速度 $$v = $$？',
    options: [
      '$$\\frac{1}{\\sqrt{\\mu_0 \\epsilon_0}}$$',
      '$$\\sqrt{\\frac{\\mu_0}{\\epsilon_0}}$$',
      '$$\\sqrt{\\mu_0 \\epsilon_0}$$',
      '$$\\frac{\\mu_0}{\\epsilon_0}$$'
    ],
    answer: 0,
    score: 10
  },
  {
    id: 3006,
    type: 'choice' as const,
    content: '电场强度 $$\\vec{E}$$ 和电位移矢量 $$\\vec{D}$$ 的关系是？',
    options: [
      '$$\\vec{D} = \\epsilon_0 \\vec{E} + \\vec{P}$$',
      '$$\\vec{D} = \\mu_0 \\vec{E}$$',
      '$$\\vec{E} = \\rho \\vec{D}$$',
      '$$\\vec{D} = \\sigma \\vec{E}$$'
    ],
    answer: 0,
    score: 10
  },
  {
    id: 3007,
    type: 'fill' as const,
    content: '安培环路定理的微分形式为 $$\\nabla \\times \\vec{B} = $$ ______。',
    answer: '$$\\mu_0 (\\vec{J} + \\epsilon_0 \\frac{\\partial \\vec{E}}{\\partial t})$$',
    score: 15
  },
  {
    id: 3008,
    type: 'essay' as const,
    content: '请详细阐述麦克斯韦方程组的四个方程，并说明每个方程的物理意义。',
    answer: '',
    score: 25
  }
]

const mockAdvancedQuestions = [
  {
    id: 4001,
    type: 'choice' as const,
    content: '在时变电磁场中，电场强度 $$\\vec{E}$$ 和磁场强度 $$\\vec{H}$$ 的关系由哪个方程描述？',
    options: [
      '法拉第电磁感应定律',
      '安培环路定理（含位移电流）',
      '高斯定理（电场）',
      '高斯定理（磁场）'
    ],
    answer: 1,
    score: 10
  },
  {
    id: 4002,
    type: 'choice' as const,
    content: '以下关于电磁波的描述，哪个是正确的？',
    options: [
      '电场和磁场相互垂直，且都垂直于传播方向',
      '电场和磁场同相，且平行于传播方向',
      '电磁波只能在真空中传播',
      '电磁波的传播速度与频率有关'
    ],
    answer: 0,
    score: 10
  },
  {
    id: 4003,
    type: 'choice' as const,
    content: '麦克斯韦方程组的微分形式中，哪个方程描述了变化的磁场产生电场？',
    options: [
      '$$\\nabla \\times \\vec{E} = -\\frac{\\partial \\vec{B}}{\\partial t}$$',
      '$$\\nabla \\times \\vec{H} = \\vec{J} + \\frac{\\partial \\vec{D}}{\\partial t}$$',
      '$$\\nabla \\cdot \\vec{D} = \\rho$$',
      '$$\\nabla \\cdot \\vec{B} = 0$$'
    ],
    answer: 0,
    score: 10
  },
  {
    id: 4004,
    type: 'fill' as const,
    content: '平面电磁波的电场和磁场满足 $$\\frac{E}{H} = $$ ______，称为波阻抗。',
    answer: '$$\\sqrt{\\frac{\\mu}{\\epsilon}}$$',
    score: 10
  },
  {
    id: 4005,
    type: 'choice' as const,
    content: '坡印廷矢量的物理意义是？',
    options: [
      '电磁能流密度',
      '电场能量密度',
      '磁场能量密度',
      '电荷密度'
    ],
    answer: 0,
    score: 10
  },
  {
    id: 4006,
    type: 'fill' as const,
    content: '能量守恒定律在电磁场中的表达式为 $$\\nabla \\cdot \\vec{S} + $$ ______ $$= 0$$。',
    answer: '$$\\frac{\\partial w}{\\partial t}$$',
    score: 15
  },
  {
    id: 4007,
    type: 'essay' as const,
    content: '请推导平面电磁波的波动方程，并说明其解的物理意义。',
    answer: '',
    score: 20
  },
  {
    id: 4008,
    type: 'essay' as const,
    content: '请分析位移电流的物理意义，并说明它在麦克斯韦方程组中的作用。',
    answer: '',
    score: 25
  }
]

const mockGeneratedQuestions = [
  {
    id: 1001,
    type: 'choice' as const,
    content: '根据高斯定理，通过闭合曲面的电通量 $$\\Phi_E = \\oint_S \\vec{E} \\cdot d\\vec{S}$$ 等于？',
    options: [
      '$$\\frac{q_{enclosed}}{\\epsilon_0}$$',
      '$$\\mu_0 I_{enclosed}$$',
      '$$-\\frac{d\\Phi_B}{dt}$$',
      '$$\\frac{d\\Phi_E}{dt}$$'
    ],
    answer: 0,
    score: 10
  },
  {
    id: 1002,
    type: 'choice' as const,
    content: '位移电流的表达式为 $$I_d = $$？',
    options: [
      '$$\\epsilon_0 \\frac{d\\Phi_E}{dt}$$',
      '$$\\frac{V}{R}$$',
      '$$\\rho v$$',
      '$$\\sigma E$$'
    ],
    answer: 0,
    score: 10
  },
  {
    id: 1003,
    type: 'choice' as const,
    content: '真空中麦克斯韦方程组的旋度方程表明？',
    options: [
      '变化的电场产生磁场',
      '变化的磁场产生电场',
      '电荷产生电场',
      '磁场总是无源的'
    ],
    answer: 0,
    score: 10
  },
  {
    id: 1004,
    type: 'fill' as const,
    content: '坡印廷矢量 $$\\vec{S} = $$ ______，它表示电磁能流密度。',
    answer: '$$\\frac{1}{\\mu_0} \\vec{E} \\times \\vec{B}$$',
    score: 10
  },
  {
    id: 1005,
    type: 'choice' as const,
    content: '平面电磁波在真空中的传播速度 $$v = $$？',
    options: [
      '$$\\frac{1}{\\sqrt{\\mu_0 \\epsilon_0}}$$',
      '$$\\sqrt{\\frac{\\mu_0}{\\epsilon_0}}$$',
      '$$\\sqrt{\\mu_0 \\epsilon_0}$$',
      '$$\\frac{\\mu_0}{\\epsilon_0}$$'
    ],
    answer: 0,
    score: 10
  },
  {
    id: 1006,
    type: 'choice' as const,
    content: '电场强度 $$\\vec{E}$$ 和电位移矢量 $$\\vec{D}$$ 的关系是？',
    options: [
      '$$\\vec{D} = \\epsilon_0 \\vec{E} + \\vec{P}$$',
      '$$\\vec{D} = \\mu_0 \\vec{E}$$',
      '$$\\vec{E} = \\rho \\vec{D}$$',
      '$$\\vec{D} = \\sigma \\vec{E}$$'
    ],
    answer: 0,
    score: 10
  },
  {
    id: 1007,
    type: 'fill' as const,
    content: '安培环路定理的微分形式为 $$\\nabla \\times \\vec{B} = $$ ______。',
    answer: '$$\\mu_0 (\\vec{J} + \\epsilon_0 \\frac{\\partial \\vec{E}}{\\partial t})$$',
    score: 15
  },
  {
    id: 1008,
    type: 'essay' as const,
    content: '请详细阐述麦克斯韦方程组的四个方程，并说明每个方程的物理意义。',
    answer: '',
    score: 25
  }
]

const questions = ref<Question[]>([
  {
    id: 1,
    type: 'choice',
    content: '下列哪个是麦克斯韦方程组中的高斯定理（电场）？',
    options: ['$$\\oint_S \\vec{E} \\cdot d\\vec{S} = \\frac{q}{\\epsilon_0}$$', '$$\\oint_C \\vec{E} \\cdot d\\vec{l} = -\\frac{d\\Phi_B}{dt}$$', '$$\\oint_S \\vec{B} \\cdot d\\vec{S} = 0$$', '$$\\oint_C \\vec{B} \\cdot d\\vec{l} = \\mu_0 I + \\mu_0 \\epsilon_0 \\frac{d\\Phi_E}{dt}$$'],
    answer: 0,
    score: 10,
    isExample: true
  },
  {
    id: 2,
    type: 'fill',
    content: '真空中的光速 $$c = $$ ______ $$m/s$$（保留三位有效数字）',
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
    content: '电场强度 $$\\vec{E}$$ 的单位是？',
    options: ['N/C', 'V/m', 'Both A and B', 'None of above'],
    answer: 2,
    score: 10,
    isExample: true
  },
  {
    id: 5,
    type: 'choice',
    content: '磁场的高斯定理 $$\\oint_S \\vec{B} \\cdot d\\vec{S} = 0$$ 表明？',
    options: ['磁场是无源场', '磁场是有源场', '磁单极子存在', '磁场与电荷无关'],
    answer: 0,
    score: 10,
    isExample: true
  },
  {
    id: 6,
    type: 'fill',
    content: '法拉第电磁感应定律的数学表达式为 $$\\oint_C \\vec{E} \\cdot d\\vec{l} = $$ ______。',
    answer: '$$-\\frac{d\\Phi_B}{dt}$$',
    score: 10,
    isExample: true
  },
  {
    id: 7,
    type: 'essay',
    content: '请解释坡印廷矢量的物理意义，并推导其表达式。',
    answer: '',
    score: 20,
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

const myExams = ref<any[]>([])

const loadExamHistory = () => {
  const history = JSON.parse(localStorage.getItem('examHistory') || '[]')
  myExams.value = history.map((item: any) => ({
    id: item.id,
    name: item.name,
    date: new Date(item.createdAt).toLocaleDateString('zh-CN'),
    score: item.totalScore,
    maxScore: item.maxScore,
    status: '已完成',
    isExample: false,
    questions: item.questions,
    userAnswers: item.userAnswers,
    questionResults: item.questionResults
  }))
}

const examColumns = [
  { title: '测试名称', dataIndex: 'name', key: 'name' },
  { title: '测试日期', dataIndex: 'date', key: 'date' },
  { title: '得分', dataIndex: 'score', key: 'score' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', dataIndex: 'action', key: 'action' }
]

const editingExamName = ref<number | null>(null)
const editingNameValue = ref('')

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

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const triggerFileUpload = () => fileInput.value?.click()

const handleFileSelect = (e: Event) => {
  const target = e.target as HTMLInputElement
  if (target.files?.length) {
    const files = Array.from(target.files).filter(f => {
      const isValidType = f.name.endsWith('.pdf') || f.name.endsWith('.docx')
      const isValidSize = f.size < 10 * 1024 * 1024
      if (!isValidType) {
        message.warning(f.name + ' 格式不支持，仅支持 PDF 和 DOCX')
      }
      if (!isValidSize) {
        message.warning(f.name + ' 文件大小超过 10MB 限制')
      }
      return isValidType && isValidSize
    })
    uploadedFiles.value.push(...files)
    if (files.length > 0) {
      message.success('文件上传成功')
    }
  }
}

const handleDrop = (e: DragEvent) => {
  const files = e.dataTransfer?.files
  if (files?.length) {
    const validFiles = Array.from(files).filter(f => {
      const isValidType = f.name.endsWith('.pdf') || f.name.endsWith('.docx')
      const isValidSize = f.size < 10 * 1024 * 1024
      return isValidType && isValidSize
    })
    uploadedFiles.value.push(...validFiles)
    message.success(`${validFiles.length} 个文件上传成功`)
  }
}

const removeFile = (name: string) => {
  uploadedFiles.value = uploadedFiles.value.filter(f => f.name !== name)
}

const generateQuestions = async () => {
  if (uploadedFiles.value.length === 0) {
    message.warning('请先上传资料文件')
    return
  }
  
  isGenerating.value = true
  const difficultyLabels = { basic: '基础', medium: '中等', advanced: '综合' }
  message.info(`智能助手正在分析资料并生成${difficultyLabels[selectedDifficulty.value]}难度测试题...`)
  
  try {
    await new Promise(resolve => setTimeout(resolve, 2000))
    
    let questionsToUse: Question[]
    switch (selectedDifficulty.value) {
      case 'basic':
        questionsToUse = mockBasicQuestions
        break
      case 'medium':
        questionsToUse = mockMediumQuestions
        break
      case 'advanced':
        questionsToUse = mockAdvancedQuestions
        break
      default:
        questionsToUse = mockBasicQuestions
    }
    
    generatedQuestions.value = questionsToUse
    
    if (uploadedFiles.value.length > 0) {
      sessionStorage.setItem('examFileName', uploadedFiles.value[0].name.replace(/\.(pdf|docx)$/i, ''))
    }
    
    message.success(`测试题生成成功！共生成 ${generatedQuestions.value.length} 道${difficultyLabels[selectedDifficulty.value]}难度题目`)
    uploadedFiles.value = []
  } catch (error) {
    message.error('生成测试题失败，请重试')
  } finally {
    isGenerating.value = false
  }
}

const startGeneratedExam = () => {
  sessionStorage.setItem('examQuestions', JSON.stringify(generatedQuestions.value))
  router.push('/exam')
}

const viewExam = (exam: any) => {
  const examData = exams.value.find(e => e.id === exam.id)
  if (examData) {
    currentExam.value = examData
    showExamDetailModal.value = true
    showResult.value = false
    Object.keys(userAnswers).forEach(k => delete userAnswers[k])
    Object.keys(selfScores).forEach(k => delete selfScores[k])
  } else if (exam.questions) {
    sessionStorage.setItem('examQuestions', JSON.stringify(exam.questions))
    sessionStorage.setItem('examUserAnswers', JSON.stringify(exam.userAnswers))
    sessionStorage.setItem('examResults', JSON.stringify(exam.questionResults))
    router.push('/exam?view=result')
  }
}

const retryExam = (exam: any) => {
  if (exam.questions) {
    sessionStorage.setItem('examQuestions', JSON.stringify(exam.questions))
    router.push('/exam')
  } else {
    viewExam(exam)
  }
}

const startEditExamName = (exam: any) => {
  editingExamName.value = exam.id
  editingNameValue.value = exam.name
}

const saveExamName = (exam: any) => {
  const history = JSON.parse(localStorage.getItem('examHistory') || '[]')
  const item = history.find((h: any) => h.id === exam.id)
  if (item) {
    item.name = editingNameValue.value
    localStorage.setItem('examHistory', JSON.stringify(history))
    message.success('测试名称已修改')
  }
  editingExamName.value = null
  refreshExamHistory()
}

const cancelEditExamName = () => {
  editingExamName.value = null
}

const deleteExamHistory = (exam: any) => {
  Modal.confirm({
    title: '删除确认',
    content: `确定要删除测试「${exam.name}」吗？此操作不可恢复。`,
    okText: '确定删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: () => {
      const history = JSON.parse(localStorage.getItem('examHistory') || '[]')
      const newHistory = history.filter((h: any) => h.id !== exam.id)
      localStorage.setItem('examHistory', JSON.stringify(newHistory))
      refreshExamHistory()
      message.success('测试删除成功')
    }
  })
}

const refreshExamHistory = () => {
  const history = JSON.parse(localStorage.getItem('examHistory') || '[]')
  myExams.value = history.map((item: any) => ({
    id: item.id,
    name: item.name,
    date: new Date(item.createdAt).toLocaleDateString('zh-CN'),
    score: item.totalScore,
    maxScore: item.maxScore,
    status: '已完成',
    isExample: false,
    questions: item.questions,
    userAnswers: item.userAnswers,
    questionResults: item.questionResults
  }))
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

const renderLatexContent = (content: string) => {
  if (!content) return ''
  
  let result = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  
  return result
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

onMounted(() => {
  loadExamHistory()
  ;(window as any).MathJax = {
    tex: {
      inlineMath: [['$', '$'], ['\\(', '\\)']],
      displayMath: [['$$', '$$'], ['\\[', '\\]']]
    },
    svg: {
      fontCache: 'global'
    },
    startup: {
      ready: () => {
        const win = window as any
        win.MathJax.startup.defaultReady()
        win.MathJax.typeset()
      }
    }
  }
  
  const mathjaxScript = document.createElement('script')
  mathjaxScript.src = 'https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js'
  mathjaxScript.async = true
  document.head.appendChild(mathjaxScript)
})

watch(generatedQuestions, () => {
  setTimeout(() => {
    if ((window as any).MathJax) {
      (window as any).MathJax.typeset()
    }
  }, 500)
})
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

.file-size {
  font-size: 12px;
  color: #999;
}

.difficulty-select {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
}

.difficulty-label {
  font-size: 14px;
  color: #666;
}

.success-message {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.success-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.success-message .panel-title {
  font-size: 20px;
  color: #1890ff;
  margin-bottom: 8px;
}

.success-desc {
  font-size: 14px;
  color: #666;
  margin: 0 0 16px 0;
}

.question-stats {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-item {
  padding: 8px 16px;
  background: #f5f5f5;
  border-radius: 20px;
  font-size: 14px;
  color: #666;
}

.generated-questions {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px dashed #ddd;
}

.question-preview {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 20px;
}

.question-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.question-card .question-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.question-card .question-number {
  font-weight: bold;
  color: #333;
}

.question-card .question-score {
  margin-left: auto;
  color: #667eea;
  font-weight: 600;
}

.question-card .question-content {
  margin-bottom: 12px;
}

.options-preview {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-item {
  padding: 10px 14px;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 14px;
}

.fill-preview, .essay-preview {
  padding: 12px;
  background: #e8f5e9;
  border-radius: 6px;
}

.answer-hint {
  color: #1b5e20;
  font-size: 14px;
}

.exam-detail {
  max-height: 700px;
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

.preview-image {
  max-width: 100%;
  max-height: 200px;
  border-radius: 8px;
  margin-top: 12px;
}

.option-input {
  margin-bottom: 8px;
}

.form-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}

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
