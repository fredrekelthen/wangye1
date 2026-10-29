<template>
  <div class="exam-container">
    <div class="exam-header">
      <div class="header-left">
        <a-button type="link" @click="goBack" class="back-btn">
          ← 返回课程
        </a-button>
        <h1 class="exam-title">智能生成测试</h1>
      </div>
      <div class="header-right">
        <div class="timer">
          <span class="timer-icon">⏱️</span>
          <span class="timer-text">{{ formatTime(elapsedTime) }}</span>
        </div>
      </div>
    </div>

    <div class="exam-body">
      <div class="question-nav">
        <div class="nav-header">
          <h3>题目导航</h3>
          <p class="progress-text">已答 {{ answeredCount }}/{{ questions.length }}</p>
        </div>
        <div class="nav-list-wrapper">
          <div class="nav-list">
            <div
              v-for="(q, idx) in questions"
              :key="q.id"
              :class="['nav-item', { 
                active: currentIndex === idx, 
                answered: userAnswers[q.id] !== undefined,
                correct: showResults && questionResults[q.id]?.isCorrect,
                wrong: showResults && questionResults[q.id]?.isCorrect === false
              }]"
              @click="selectQuestion(idx)"
            >
              {{ idx + 1 }}
            </div>
          </div>
        </div>
      </div>

      <div class="question-content">
        <div v-if="currentQuestion" class="question-card">
          <div class="question-header">
            <span class="question-number">{{ currentIndex + 1 }}.</span>
            <a-tag :color="getQuestionTypeColor(currentQuestion.type)">
              {{ getQuestionTypeLabel(currentQuestion.type) }}
            </a-tag>
            <span class="question-score">({{ currentQuestion.score }}分)</span>
            <span 
              v-if="showResults" 
              :class="['result-badge', questionResults[currentQuestion.id]?.isCorrect ? 'correct' : 'wrong']"
            >
              {{ questionResults[currentQuestion.id]?.isCorrect ? '✓ 正确' : '✗ 错误' }}
            </span>
          </div>

          <div class="question-body">
            <span v-html="renderLatexContent(currentQuestion.content)"></span>
          </div>

          <div v-if="currentQuestion.type === 'choice'" class="options">
            <div
              v-for="(option, optIdx) in currentQuestion.options"
              :key="optIdx"
              :class="['option', { 
                selected: userAnswers[currentQuestion.id] === optIdx,
                correct: showResults && optIdx === currentQuestion.answer,
                wrong: showResults && userAnswers[currentQuestion.id] === optIdx && optIdx !== currentQuestion.answer
              }]"
              @click="!showResults && selectOption(currentQuestion.id, optIdx)"
            >
              {{ ['A', 'B', 'C', 'D'][optIdx] }}.
              <span v-html="renderLatexContent(option)"></span>
              <span v-if="showResults && optIdx === currentQuestion.answer" class="correct-mark">✓</span>
              <span v-if="showResults && userAnswers[currentQuestion.id] === optIdx && optIdx !== currentQuestion.answer" class="wrong-mark">✗</span>
            </div>
          </div>

          <div v-if="currentQuestion.type === 'fill'" class="fill-input">
            <a-input
              v-model:value="userAnswers[currentQuestion.id]"
              :disabled="showResults"
              placeholder="请输入答案（支持数学公式，如：3.00×10^8 或 $\\pi r^2$）"
              class="answer-input"
            />
            <div v-if="showResults" class="answer-compare">
              <div class="answer-item">
                <span class="label">您的答案：</span>
                <span :class="['answer-value', questionResults[currentQuestion.id]?.isCorrect ? 'correct' : 'wrong']">
                  {{ userAnswers[currentQuestion.id] || '未作答' }}
                </span>
              </div>
              <div class="answer-item">
                <span class="label">正确答案：</span>
                <span class="answer-value correct">{{ renderLatexContent(String(currentQuestion.answer)) }}</span>
              </div>
            </div>
          </div>

          <div v-if="currentQuestion.type === 'essay'" class="essay-input">
            <a-textarea
              v-model:value="userAnswers[currentQuestion.id]"
              :disabled="showResults"
              :rows="8"
              placeholder="请详细回答上述问题..."
              class="answer-textarea"
            />
            <div v-if="!showResults" class="self-score">
              <a-input-number
                v-model:value="selfScores[currentQuestion.id]"
                :min="0"
                :max="currentQuestion.score"
                style="width: 100px"
              />
              <span class="score-label">自评分数 (0-{{ currentQuestion.score }})</span>
            </div>
            <div v-if="showResults" class="self-score-result">
              <span class="label">您的自评分数：</span>
              <span class="score-value">{{ selfScores[currentQuestion.id] || 0 }}/{{ currentQuestion.score }}分</span>
            </div>
          </div>

          <div v-if="showResults && currentQuestion.explanation" class="explanation">
            <h4 class="explanation-title">📝 题目解析</h4>
            <p v-html="renderLatexContent(currentQuestion.explanation)"></p>
          </div>
        </div>

        <div class="question-footer">
          <a-button
            v-if="!showResults && currentIndex > 0"
            type="primary"
            @click="prevQuestion"
          >
            ← 上一题
          </a-button>
          <a-button
            v-if="!showResults && currentIndex < questions.length - 1"
            type="primary"
            @click="nextQuestion"
          >
            下一题 →
          </a-button>
          <a-button
            v-if="!showResults"
            type="danger"
            @click="submitExam"
            :disabled="answeredCount === 0"
            class="submit-btn"
          >
            提交答案
          </a-button>
          <a-button
            v-if="showResults"
            type="primary"
            @click="restartExam"
            class="submit-btn"
          >
            🔄 重新测试
          </a-button>
        </div>
      </div>
    </div>

    <a-modal
      v-model:open="showResultModal"
      title="测试完成"
      width="800px"
      :footer="null"
    >
      <div class="result-content">
        <div class="result-header">
          <div class="result-icon">{{ getScoreIcon() }}</div>
          <h3>测试已完成！</h3>
          <div class="score-display">
            <span class="score-value">{{ totalScore }}</span>
            <span class="score-total">/{{ maxScore }}分</span>
          </div>
          <p class="score-comment">{{ getScoreComment() }}</p>
        </div>
        
        <div class="result-stats">
          <div class="stat-item">
            <span class="stat-label">总题数</span>
            <span class="stat-value">{{ questions.length }} 题</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">答题时间</span>
            <span class="stat-value">{{ formatTime(elapsedTime) }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">正确题数</span>
            <span class="stat-value correct">{{ correctCount }} 题</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">错误题数</span>
            <span class="stat-value wrong">{{ questions.length - correctCount }} 题</span>
          </div>
        </div>

        <div class="result-actions">
          <a-input
            v-model:value="examName"
            placeholder="输入测试名称"
            style="width: 300px; margin-right: 12px"
          />
          <a-button type="primary" @click="saveExamResult">保存测试结果</a-button>
          <a-button @click="viewDetails">查看详情</a-button>
          <a-button @click="restartExam">重新测试</a-button>
          <a-button @click="goBack">返回课程</a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

const router = useRouter()

interface Question {
  id: number
  type: 'choice' | 'fill' | 'essay'
  content: string
  imageUrl?: string
  options?: string[]
  answer: string | number
  score: number
  explanation?: string
}

interface QuestionResult {
  isCorrect: boolean
  userAnswer: string | number | undefined
  correctAnswer: string | number
  score: number
}

const questions = ref<Question[]>([])
const currentIndex = ref(0)
const userAnswers = reactive<Record<number, string | number>>({})
const selfScores = reactive<Record<number, number>>({})
const questionResults = reactive<Record<number, QuestionResult>>({})
const elapsedTime = ref(0)
const showResultModal = ref(false)
const showResults = ref(false)
const examName = ref('')
let timerInterval: number | null = null

const currentQuestion = computed(() => questions.value[currentIndex.value])

const answeredCount = computed(() => {
  return questions.value.filter(q => userAnswers[q.id] !== undefined).length
})

const correctCount = computed(() => {
  return questions.value.filter(q => questionResults[q.id]?.isCorrect).length
})

const totalScore = computed(() => {
  return questions.value.reduce((sum, q) => {
    const result = questionResults[q.id]
    if (result) {
      if (q.type === 'essay') {
        return sum + (selfScores[q.id] || 0)
      }
      return sum + (result.isCorrect ? q.score : 0)
    }
    return sum
  }, 0)
})

const maxScore = computed(() => {
  return questions.value.reduce((sum, q) => sum + q.score, 0)
})

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

const renderLatexContent = (content: string) => {
  if (!content) return ''
  return content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

const formatTime = (seconds: number) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

const selectQuestion = (idx: number) => {
  currentIndex.value = idx
}

const selectOption = (questionId: number, optIdx: number) => {
  userAnswers[questionId] = optIdx
}

const prevQuestion = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--
  }
}

const nextQuestion = () => {
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++
  }
}

const submitExam = () => {
  if (timerInterval) {
    clearInterval(timerInterval)
  }
  
  questions.value.forEach(q => {
    let isCorrect = false
    const userAnswer = userAnswers[q.id]
    
    if (q.type === 'choice') {
      isCorrect = userAnswer === q.answer
    } else if (q.type === 'fill') {
      isCorrect = String(userAnswer).trim().toLowerCase() === String(q.answer).trim().toLowerCase()
    } else if (q.type === 'essay') {
      isCorrect = true
    }
    
    questionResults[q.id] = {
      isCorrect,
      userAnswer,
      correctAnswer: q.answer,
      score: isCorrect ? q.score : 0
    }
  })
  
  const fileName = sessionStorage.getItem('examFileName') || '测试'
  examName.value = `${fileName} - 测试`
  
  showResultModal.value = true
}

const viewDetails = () => {
  showResultModal.value = false
  showResults.value = true
}

const saveExamResult = () => {
  const examResult = {
    id: Date.now(),
    name: examName.value || '测试',
    questions: questions.value,
    userAnswers: { ...userAnswers },
    questionResults: { ...questionResults },
    totalScore: totalScore.value,
    maxScore: maxScore.value,
    correctCount: correctCount.value,
    totalQuestions: questions.value.length,
    elapsedTime: elapsedTime.value,
    createdAt: new Date().toISOString()
  }
  
  const history = JSON.parse(localStorage.getItem('examHistory') || '[]')
  history.unshift(examResult)
  localStorage.setItem('examHistory', JSON.stringify(history.slice(0, 20)))
  
  message.success('测试结果已保存！')
  showResultModal.value = false
  showResults.value = true
}

const restartExam = () => {
  showResultModal.value = false
  showResults.value = false
  currentIndex.value = 0
  elapsedTime.value = 0
  Object.keys(userAnswers).forEach(k => delete userAnswers[k])
  Object.keys(selfScores).forEach(k => delete selfScores[k])
  Object.keys(questionResults).forEach(k => delete questionResults[k])
  
  timerInterval = window.setInterval(() => {
    elapsedTime.value++
  }, 1000)
}

const goBack = () => {
  router.push('/course')
}

const getScoreIcon = () => {
  const percentage = (totalScore.value / maxScore.value) * 100
  if (percentage >= 90) return '🏆'
  if (percentage >= 80) return '🎉'
  if (percentage >= 70) return '👍'
  if (percentage >= 60) return '💪'
  return '📚'
}

const getScoreComment = () => {
  const percentage = (totalScore.value / maxScore.value) * 100
  if (percentage >= 90) return '优秀！继续保持！'
  if (percentage >= 80) return '很好！再接再厉！'
  if (percentage >= 70) return '不错！还有提升空间！'
  if (percentage >= 60) return '及格了，加油！'
  return '需要多加练习哦！'
}

onMounted(() => {
  const storedQuestions = sessionStorage.getItem('examQuestions')
  if (storedQuestions) {
    questions.value = JSON.parse(storedQuestions)
  } else {
    message.warning('未找到测试题目，将返回课程页面')
    setTimeout(() => {
      router.push('/course')
    }, 1500)
    return
  }

  const urlParams = new URLSearchParams(window.location.search)
  const viewMode = urlParams.get('view')
  
  if (viewMode === 'result') {
    const storedUserAnswers = sessionStorage.getItem('examUserAnswers')
    const storedResults = sessionStorage.getItem('examResults')
    
    if (storedUserAnswers) {
      const parsedAnswers = JSON.parse(storedUserAnswers)
      Object.keys(parsedAnswers).forEach(key => {
        userAnswers[Number(key)] = parsedAnswers[key]
      })
    }
    
    if (storedResults) {
      const parsedResults = JSON.parse(storedResults)
      Object.keys(parsedResults).forEach(key => {
        questionResults[Number(key)] = parsedResults[key]
      })
    }
    
    showResults.value = true
  } else {
    timerInterval = window.setInterval(() => {
      elapsedTime.value++
    }, 1000)
  }

  const loadMathJax = () => {
    if ((window as any).MathJax) {
      (window as any).MathJax.typeset()
      return
    }

    const win = window as any
    win.MathJax = {
      tex: {
        inlineMath: [['$', '$'], ['\\(', '\\)']],
        displayMath: [['$$', '$$'], ['\\[', '\\]']],
        processEscapes: true
      },
      svg: {
        fontCache: 'global'
      },
      startup: {
        ready: () => {
          win.MathJax.startup.defaultReady()
          win.MathJax.typeset()
        }
      }
    }

    const mathjaxScript = document.createElement('script')
    mathjaxScript.src = 'https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-svg.js'
    mathjaxScript.async = true
    mathjaxScript.onload = () => {
      setTimeout(() => {
        if ((window as any).MathJax) {
          (window as any).MathJax.typeset()
        }
      }, 300)
    }
    document.head.appendChild(mathjaxScript)
  }

  loadMathJax()

  setTimeout(() => {
    if ((window as any).MathJax) {
      (window as any).MathJax.typeset()
    }
  }, 1000)
})

onUnmounted(() => {
  if (timerInterval) {
    clearInterval(timerInterval)
  }
})

watch(currentIndex, () => {
  setTimeout(() => {
    if ((window as any).MathJax) {
      (window as any).MathJax.typeset()
    }
  }, 200)
})
</script>

<style scoped>
.exam-container {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 20px;
}

.exam-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  padding: 16px 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  font-size: 14px;
}

.exam-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
}

.timer {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fff3cd;
  padding: 8px 16px;
  border-radius: 20px;
  border: 1px solid #ffeeba;
}

.timer-icon {
  font-size: 18px;
}

.timer-text {
  font-size: 16px;
  font-weight: 600;
  color: #856404;
  font-family: 'Courier New', monospace;
}

.exam-body {
  display: flex;
  gap: 20px;
}

.question-nav {
  width: 200px;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  box-sizing: border-box;
}

.nav-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.nav-header h3 {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
}

.progress-text {
  margin: 0;
  font-size: 12px;
  color: #999;
}

.nav-list-wrapper {
  overflow: hidden;
}

.nav-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  background: #f5f5f5;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
}

.nav-item:hover {
  background: #e6f7ff;
}

.nav-item.active {
  background: #1890ff;
  color: #fff;
}

.nav-item.answered {
  background: #52c41a;
  color: #fff;
}

.nav-item.correct {
  background: #52c41a;
  color: #fff;
}

.nav-item.wrong {
  background: #f5222d;
  color: #fff;
}

.nav-item.active.correct,
.nav-item.active.wrong {
  background: #1890ff;
}

.question-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.question-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.question-card .question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.question-card .question-number {
  font-size: 18px;
  font-weight: 700;
  color: #333;
}

.question-card .question-score {
  color: #667eea;
  font-weight: 600;
}

.result-badge {
  margin-left: auto;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
}

.result-badge.correct {
  background: #f6ffed;
  color: #52c41a;
}

.result-badge.wrong {
  background: #fff2f0;
  color: #f5222d;
}

.question-card .question-body {
  font-size: 16px;
  line-height: 1.8;
  color: #333;
  margin-bottom: 20px;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  background: #fafafa;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
}

.option:hover:not(.correct):not(.wrong) {
  border-color: #667eea;
  background: #f0f5ff;
}

.option.selected {
  border-color: #667eea;
  background: #f0f5ff;
}

.option.correct {
  border-color: #52c41a;
  background: #f6ffed;
}

.option.wrong {
  border-color: #f5222d;
  background: #fff2f0;
}

.correct-mark,
.wrong-mark {
  margin-left: auto;
  font-weight: bold;
}

.correct-mark {
  color: #52c41a;
}

.wrong-mark {
  color: #f5222d;
}

.fill-input,
.essay-input {
  margin-top: 16px;
}

.answer-input {
  width: 100%;
}

.answer-textarea {
  width: 100%;
}

.answer-compare {
  margin-top: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

.answer-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.answer-item:last-child {
  margin-bottom: 0;
}

.answer-item .label {
  font-size: 14px;
  color: #666;
}

.answer-value {
  font-size: 14px;
  font-weight: 600;
}

.answer-value.correct {
  color: #52c41a;
}

.answer-value.wrong {
  color: #f5222d;
}

.self-score {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.self-score-result {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.self-score-result .label {
  font-size: 14px;
  color: #666;
}

.score-value {
  font-size: 16px;
  font-weight: 600;
  color: #667eea;
}

.score-label {
  font-size: 14px;
  color: #666;
}

.explanation {
  margin-top: 20px;
  padding: 16px;
  background: #e6f7ff;
  border-radius: 8px;
}

.explanation-title {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1890ff;
}

.explanation p {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
}

.question-footer {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.submit-btn {
  margin-left: 20px;
}

.result-content {
  text-align: center;
  padding: 20px;
}

.result-header {
  margin-bottom: 24px;
}

.result-icon {
  font-size: 80px;
  margin-bottom: 12px;
}

.result-header h3 {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 12px;
}

.score-display {
  margin-bottom: 8px;
}

.score-display .score-value {
  font-size: 48px;
  font-weight: 700;
  color: #667eea;
}

.score-display .score-total {
  font-size: 24px;
  color: #999;
}

.score-comment {
  margin: 0;
  font-size: 16px;
  color: #666;
}

.result-stats {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-bottom: 24px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

.stat-item {
  text-align: center;
}

.stat-label {
  display: block;
  font-size: 14px;
  color: #999;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.stat-value.correct {
  color: #52c41a;
}

.stat-value.wrong {
  color: #f5222d;
}

.result-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
</style>