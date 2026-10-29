<template>
  <div class="emulation-section">
    <div class="section-header">
      <h3 class="section-title">电磁仿真</h3>
      <a-tag v-if="fromTopic" color="purple" class="topic-tag">来自知识图谱：{{ fromTopic }}</a-tag>
      <a-button type="link" danger @click="clearHistory" v-if="history.length > 0">清空记录</a-button>
    </div>

    <div class="emulation-body">
      <!-- 左侧：参数配置 -->
      <div class="config-panel">
        <h4 class="panel-title">📡 仿真配置</h4>

        <a-form layout="vertical">
          <a-form-item label="仿真类型">
            <a-select v-model:value="form.type" @change="handleTypeChange">
              <a-select-option v-for="opt in emulationOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </a-select-option>
            </a-select>
            <p class="field-desc">{{ currentOption?.desc }}</p>
          </a-form-item>

          <a-form-item v-for="field in currentFields" :key="field.key" :label="field.label">
            <a-select
              v-if="field.type === 'select'"
              v-model:value="form.params[field.key]"
              :placeholder="field.placeholder"
            >
              <a-select-option v-for="opt in field.options" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </a-select-option>
            </a-select>
            <a-input
              v-else
              v-model:value="form.params[field.key]"
              :placeholder="field.placeholder"
            />
            <p class="field-desc" v-if="field.desc">{{ field.desc }}</p>
          </a-form-item>

          <a-form-item>
            <a-space>
              <a-button type="primary" :loading="loading" @click="runEmulation">
                ⚡ 开始仿真
              </a-button>
              <a-button @click="resetForm">重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>

        <p class="tip">提示：仿真由后台 Python 服务计算，耗时约 5~30 秒，请耐心等待。</p>
      </div>

      <!-- 右侧：结果展示 -->
      <div class="result-panel">
        <h4 class="panel-title">📊 仿真结果</h4>

        <div v-if="loading" class="loading-box">
          <a-spin size="large" />
          <p class="loading-text">仿真计算中，请稍候...</p>
        </div>

        <template v-else-if="result">
          <div class="result-meta">
            <a-tag color="blue">{{ lastTypeLabel }}</a-tag>
            <span class="result-time">{{ lastRunTime }}</span>
          </div>
          <div v-if="resultImage" class="result-image-box">
            <img class="result-image" :src="resultImage" alt="仿真结果图" />
          </div>
          <pre class="result-json">{{ formattedResult }}</pre>
        </template>

        <a-empty v-else description="暂无仿真结果，请在左侧配置参数后开始仿真" />
      </div>
    </div>

    <!-- 历史记录 -->
    <div class="history-panel" v-if="history.length > 0">
      <h4 class="panel-title">🕘 历史记录</h4>
      <a-table
        :columns="historyColumns"
        :data-source="history"
        :pagination="{ pageSize: 5 }"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a-tag color="blue">{{ record.typeLabel }}</a-tag>
          </template>
          <template v-else-if="column.key === 'params'">
            <span class="params-text">{{ formatParams(record.params) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" @click="viewHistory(record)">查看</a-button>
            <a-button type="link" danger @click="removeHistory(record)">删除</a-button>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { emulationApi, emulationOptions } from '@/api/emulation'

interface FieldOption {
  value: string
  label: string
}

interface FieldDef {
  key: string
  label: string
  placeholder?: string
  desc?: string
  required?: boolean
  type?: 'input' | 'select'
  options?: FieldOption[]
}

interface HistoryItem {
  id: number
  type: string
  typeLabel: string
  params: Record<string, string>
  result: any
  time: string
}

const stateOptions: FieldOption[] = [
  { value: '1', label: '1 - GA（单阵元）' },
  { value: '2', label: '2 - PSO（单阵元）' },
  { value: '3', label: '3 - SA（单阵元）' },
  { value: '4', label: '4 - Tabu（单阵元）' },
  { value: '5', label: '5 - GA（偶阵元）' },
  { value: '6', label: '6 - PSO（偶阵元）' },
  { value: '7', label: '7 - SA（偶阵元）' },
  { value: '8', label: '8 - Tabu（偶阵元）' },
]

/** 各仿真类型的参数字段定义（参数名与 Python 仿真服务接口保持一致） */
const fieldDefs: Record<string, FieldDef[]> = {
  doublet: [
    { key: 'nu', label: 'nu', placeholder: '如 4', required: true },
    { key: 'value1', label: 'value1（角度）', placeholder: '如 60', required: true },
    { key: 'value2', label: 'value2（预留，可空）', placeholder: '可留空' },
    { key: 'state', label: '算法 / 阵元', type: 'select', options: stateOptions, required: true },
  ],
  monopole: [
    { key: 'nu', label: 'nu', placeholder: '如 4', required: true },
    { key: 'value1', label: 'value1', placeholder: '如 60', required: true },
    { key: 'value2', label: 'value2', placeholder: '如 3' },
  ],
  ybfs: [
    { key: 'nu', label: 'nu', placeholder: '如 30', required: true },
    { key: 'value1', label: 'value1', placeholder: '如 6', required: true },
    { key: 'value2', label: 'value2', placeholder: '如 3' },
  ],
  qzs: [
    { key: 'nu', label: 'nu', placeholder: '如 4', required: true },
    { key: 'value1', label: 'value1', placeholder: '如 8', required: true },
  ],
  qfs: [
    { key: 'nu', label: 'nu', placeholder: '如 50', required: true },
  ],
  gs: [
    { key: 'nu', label: 'nu', placeholder: '如 1', required: true },
    { key: 'value1', label: 'value1', placeholder: '如 5', required: true },
    { key: 'value2', label: 'value2', placeholder: '如 200' },
  ],
  zxb: [
    { key: 'value1', label: 'value1', placeholder: '如 5e7', required: true },
    { key: 'value2', label: 'value2', placeholder: '如 10', required: true },
  ],
  emulation: [
    { key: 'nu', label: 'nu（阵元数）', placeholder: '如 4', required: true },
    { key: 'value1', label: 'value1（目标角度）', placeholder: '如 60', required: true },
    {
      key: 'kind',
      label: '阵元类型',
      type: 'select',
      options: [
        { value: 'sp', label: '单阵元' },
        { value: 'dp', label: '偶阵元' },
      ],
    },
  ],
}

const allKeys = Array.from(new Set(Object.values(fieldDefs).flat().map(f => f.key)))

const form = reactive({
  type: 'doublet',
  params: allKeys.reduce((acc, key) => ({ ...acc, [key]: '' }), {} as Record<string, string>),
})

const loading = ref(false)
const result = ref<any>(null)
const lastTypeLabel = ref('')
const lastRunTime = ref('')
const history = ref<HistoryItem[]>([])

const currentOption = computed(() => emulationOptions.find(o => o.value === form.type))
const currentFields = computed<FieldDef[]>(() => fieldDefs[form.type] || [])

/** 结果图：服务端复刻 MATLAB 绘图，以 base64 随响应下发 */
const resultImage = computed(() =>
  result.value?.imageBase64 ? 'data:image/png;base64,' + result.value.imageBase64 : ''
)

/** 文本结果：剔除体积较大的图片字段，避免刷屏 */
const formattedResult = computed(() => {
  if (!result.value) return ''
  const { imageBase64, ...rest } = result.value
  return JSON.stringify(rest, null, 2)
})

const historyColumns = [
  { title: '仿真类型', dataIndex: 'typeLabel', key: 'type' },
  { title: '参数', dataIndex: 'params', key: 'params' },
  { title: '时间', dataIndex: 'time', key: 'time' },
  { title: '操作', dataIndex: 'action', key: 'action' },
]

const handleTypeChange = () => {
  currentFields.value.forEach(f => {
    if (f.type !== 'select') form.params[f.key] = ''
  })
  if (form.type === 'emulation') form.params['kind'] = 'sp'
  result.value = null
}

// 知识图谱跳转：?type=xxx 预选仿真类型，?topic=xxx 显示来源主题
const route = useRoute()
const fromTopic = ref('')
const applyRouteQuery = () => {
  const type = route.query.type as string | undefined
  const topic = route.query.topic as string | undefined
  fromTopic.value = topic || ''
  if (type && emulationOptions.some(o => o.value === type) && form.type !== type) {
    form.type = type
    handleTypeChange()
  }
}
onMounted(applyRouteQuery)
watch(() => [route.query.type, route.query.topic], applyRouteQuery)

const resetForm = () => {
  allKeys.forEach(k => (form.params[k] = ''))
  if (form.type === 'doublet') form.params['state'] = '1'
  result.value = null
}

const formatParams = (params: Record<string, string>) =>
  Object.entries(params)
    .filter(([, v]) => v !== '' && v !== undefined && v !== null)
    .map(([k, v]) => `${k}=${v}`)
    .join(', ') || '-'

const runEmulation = async () => {
  const missing = currentFields.value.filter(f => f.required && !form.params[f.key])
  if (missing.length > 0) {
    message.warning(`请填写：${missing.map(f => f.label).join('、')}`)
    return
  }

  loading.value = true
  result.value = null
  try {
    const apiFn = (emulationApi as any)[form.type]
    const res = await apiFn({ ...form.params })
    result.value = res.data
    lastTypeLabel.value = currentOption.value?.label || form.type
    lastRunTime.value = new Date().toLocaleString('zh-CN')

    const compactParams: Record<string, string> = {}
    Object.entries(form.params).forEach(([k, v]) => {
      if (v !== '' && v !== undefined && v !== null) compactParams[k] = v as string
    })
    history.value.unshift({
      id: Date.now(),
      type: form.type,
      typeLabel: lastTypeLabel.value,
      params: compactParams,
      result: res.data,
      time: lastRunTime.value,
    })
    history.value = history.value.slice(0, 20)
    saveHistory()
    message.success('仿真完成')
  } catch (e) {
    // 错误提示已在 axios 拦截器中统一处理
  } finally {
    loading.value = false
  }
}

const viewHistory = (record: HistoryItem) => {
  form.type = record.type
  Object.entries(record.params).forEach(([k, v]) => {
    if (k in form.params) form.params[k] = v
  })
  result.value = record.result
  lastTypeLabel.value = record.typeLabel
  lastRunTime.value = record.time
}

const removeHistory = (record: HistoryItem) => {
  history.value = history.value.filter(h => h.id !== record.id)
  saveHistory()
}

const clearHistory = () => {
  history.value = []
  saveHistory()
  message.success('历史记录已清空')
}

const HISTORY_KEY = 'emulationHistory'

const saveHistory = () => {
  try {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(history.value))
  } catch (e) {
    // 结果图体积较大时可能超出存储配额，逐步减少保留条数
    history.value = history.value.slice(0, 5)
    try {
      localStorage.setItem(HISTORY_KEY, JSON.stringify(history.value))
    } catch {
      // 仍失败则放弃持久化，不影响本次使用
    }
  }
}

const loadHistory = () => {
  try {
    const raw = localStorage.getItem(HISTORY_KEY)
    history.value = raw ? JSON.parse(raw) : []
  } catch {
    history.value = []
  }
}

loadHistory()
</script>

<style scoped>
.topic-tag {
  margin-left: auto;
  margin-right: 8px;
}

.emulation-section {
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

.emulation-body {
  display: grid;
  grid-template-columns: 340px 1fr;
  gap: 24px;
}

.config-panel,
.result-panel {
  background: #fafafa;
  border-radius: 12px;
  padding: 24px;
}

.panel-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.field-desc {
  margin: 6px 0 0 0;
  font-size: 12px;
  color: #999;
  line-height: 1.5;
}

.tip {
  margin: 8px 0 0 0;
  font-size: 12px;
  color: #faad14;
  line-height: 1.6;
}

.loading-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}

.loading-text {
  margin-top: 16px;
  color: #666;
  font-size: 14px;
}

.result-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.result-time {
  font-size: 12px;
  color: #999;
}

.result-image-box {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
  padding: 12px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
}

.result-image {
  max-width: 100%;
  max-height: 420px;
}

.result-json {
  margin: 0;
  padding: 16px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  max-height: 420px;
  overflow: auto;
  font-size: 13px;
  line-height: 1.6;
  color: #333;
  white-space: pre-wrap;
  word-break: break-all;
}

.history-panel {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.params-text {
  font-size: 13px;
  color: #666;
}
</style>
