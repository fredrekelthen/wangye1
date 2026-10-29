<template>
  <div id="material-manage">
    <a-card title="资料管理" class="manage-card">
      <!-- 统计信息 -->
      <div class="stats-row">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-statistic title="总资料数" :value="displayStats.totalMaterials" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 20px; border-radius: 12px; color: white;">
              <template #suffix>
                <FileTextOutlined />
              </template>
            </a-statistic>
          </a-col>
          <a-col :span="6">
            <a-statistic title="文档块数" :value="displayStats.totalChunks" style="background: linear-gradient(135deg, #00b894 0%, #00cec9 100%); padding: 20px; border-radius: 12px; color: white;">
              <template #suffix>
                <ClusterOutlined />
              </template>
            </a-statistic>
          </a-col>
          <a-col :span="6">
            <a-statistic title="已索引" :value="indexedCount" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); padding: 20px; border-radius: 12px; color: white;">
              <template #suffix>
                <CheckCircleOutlined />
              </template>
            </a-statistic>
          </a-col>
          <a-col :span="6">
            <a-statistic title="待处理" :value="pendingCount" style="background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%); padding: 20px; border-radius: 12px; color: #333;">
              <template #suffix>
                <ClockCircleOutlined />
              </template>
            </a-statistic>
          </a-col>
        </a-row>
      </div>

      <!-- 操作按钮 -->
      <div class="action-bar">
        <div class="left-actions">
          <a-button type="primary" @click="showUploadModal = true">
            <template #icon><UploadOutlined /></template>
            上传资料
          </a-button>
          <a-button @click="fetchMaterials">
            <template #icon><ReloadOutlined /></template>
            刷新列表
          </a-button>
        </div>
        <div class="right-actions">
          <a-input-search
            v-model:value="searchKeyword"
            placeholder="搜索资料..."
            style="width: 300px"
            @search="handleSearch"
          />
        </div>
      </div>

      <!-- 资料列表 -->
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="all" tab="全部资料">
          <a-alert
            v-if="filteredMaterials.length > 0 && filteredMaterials.some(m => m.id?.startsWith('example'))"
            message="以下为样例数据，实际数据需要后端支持"
            type="info"
            show-icon
            style="margin-bottom: 16px"
          />
          <a-table
            :columns="columns"
            :data-source="filteredMaterials"
            :pagination="{ pageSize: 10 }"
            :loading="loading"
            :row-key="(record) => record.id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'isExample'">
                <a-tag v-if="record.id?.startsWith('example')" color="green">样例</a-tag>
                <span v-else>-</span>
              </template>
              <template v-else-if="column.key === 'fileType'">
                <a-tag :color="getFileTypeColor(record.fileType)">
                  {{ getFileTypeLabel(record.fileType) }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'chapterNumber'">
                {{ record.chapterNumber ? '第' + record.chapterNumber + '章' : '未分类' }}
              </template>
              <template v-else-if="column.key === 'isIndexed'">
                <a-badge v-if="record.isIndexed" status="success" text="已索引" />
                <a-badge v-else status="processing" text="处理中..." />
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="viewMaterial(record)">查看</a-button>
                  <a-button type="link" size="small" @click="generateContent(record)">
                    <RocketOutlined /> 生成内容
                  </a-button>
                  <a-popconfirm title="确定删除该资料？" @confirm="deleteMaterial(record.id)">
                    <a-button type="link" danger size="small">删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="byChapter" tab="按章节分组">
          <a-collapse v-model:activeKey="chapterActiveKey">
            <a-collapse-panel
              v-for="(materials, chapter) in materialsByChapter"
              :key="chapter"
              :header="'第 ' + chapter + ' 章 (' + materials.length + ' 个资料)'"
            >
              <template #extra>
                <a-space>
                  <a-button size="small" type="primary" @click.stop="generateChapterContent(chapter)">
                    <RocketOutlined /> 生成章节内容
                  </a-button>
                </a-space>
              </template>

              <a-table
                :columns="columns"
                :data-source="materials"
                :pagination="false"
                size="small"
                :row-key="(record) => record.id"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'fileType'">
                    <a-tag :color="getFileTypeColor(record.fileType)">
                      {{ getFileTypeLabel(record.fileType) }}
                    </a-tag>
                  </template>
                  <template v-else-if="column.key === 'isIndexed'">
                    <a-badge v-if="record.isIndexed" status="success" text="已索引" />
                    <a-badge v-else status="processing" text="处理中..." />
                  </template>
                  <template v-else-if="column.key === 'action'">
                    <a-space>
                      <a-button type="link" size="small" @click="viewMaterial(record)">查看</a-button>
                      <a-button type="link" size="small" @click="generateContent(record)">
                        <RocketOutlined /> 生成
                      </a-button>
                      <a-popconfirm title="确定删除？" @confirm="deleteMaterial(record.id)">
                        <a-button type="link" danger size="small">删除</a-button>
                      </a-popconfirm>
                    </a-space>
                  </template>
                </template>
              </a-table>
            </a-collapse-panel>
          </a-collapse>

          <a-empty v-if="Object.keys(materialsByChapter).length === 0 && !loading" description="暂无资料" />
        </a-tab-pane>
      </a-tabs>

      <a-spin v-if="loading" tip="加载中..." />
    </a-card>

    <!-- 上传资料弹窗 -->
    <a-modal
      v-model:open="showUploadModal"
      title="上传学习资料"
      width="600px"
      :footer="null"
    >
      <a-form :model="uploadForm" layout="vertical">
        <a-form-item label="资料标题">
          <a-input v-model:value="uploadForm.title" placeholder="可选，留空则自动使用文件名" />
        </a-form-item>

        <a-form-item label="所属章节" required>
          <div class="selector-col">
            <a-select
              v-model:value="selectedChapterId"
              :options="chapterOptions"
              :field-names="{ label: 'title', value: 'id' }"
              placeholder="选择章"
              style="width: 100%"
              show-search
              :filter-option="filterSelectOption"
              @change="onChapterChange"
            />
            <a-select
              v-model:value="selectedSectionId"
              :options="sectionOptions"
              :field-names="{ label: 'title', value: 'id' }"
              placeholder="选择节（可选）"
              style="width: 100%"
              show-search
              :filter-option="filterSelectOption"
              :disabled="!selectedChapterId"
              @change="onSectionChange"
            />
            <a-select
              v-model:value="selectedSubsectionId"
              :options="subsectionOptions"
              :field-names="{ label: 'title', value: 'id' }"
              placeholder="选择三级标题（可选）"
              style="width: 100%"
              show-search
              :filter-option="filterSelectOption"
              :disabled="!selectedSectionId"
              @change="onSubsectionChange"
            />
          </div>
        </a-form-item>

        <a-form-item label="上传文件" required>
          <a-upload
            :before-upload="beforeUpload"
            :file-list="fileList"
            accept=".pdf,.docx,.doc,.txt,.md"
            @remove="handleRemove"
          >
            <a-button>
              <UploadOutlined /> 选择文件
            </a-button>
          </a-upload>
          <div class="upload-hint">
            <span>支持 PDF、Word (.docx/.doc)、TXT、Markdown 格式</span>
            <span>文件大小不超过 10MB</span>
          </div>
        </a-form-item>

        <a-form-item>
          <a-space>
            <a-button type="primary" :loading="uploading" @click="handleUpload">
              {{ uploading ? '上传中...' : '开始上传' }}
            </a-button>
            <a-button @click="cancelUpload">取消</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 查看资料弹窗 -->
    <a-modal
      v-model:open="showViewModal"
      :title="currentMaterial?.title"
      width="700px"
      :footer="null"
    >
      <div v-if="currentMaterial" class="material-detail">
        <a-descriptions bordered :column="2">
          <a-descriptions-item label="文件名">{{ currentMaterial.fileName }}</a-descriptions-item>
          <a-descriptions-item label="文件类型">
            <a-tag :color="getFileTypeColor(currentMaterial.fileType)">
              {{ getFileTypeLabel(currentMaterial.fileType) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="所属章节">
            {{ currentMaterial.chapterNumber ? '第' + currentMaterial.chapterNumber + '章' : '未分类' }}
            <template v-if="currentMaterial.sectionTitle"> / {{ currentMaterial.sectionTitle }}</template>
          </a-descriptions-item>
          <a-descriptions-item label="上传者">{{ currentMaterial.uploadedBy || '管理员' }}</a-descriptions-item>
          <a-descriptions-item label="上传时间">{{ formatDate(currentMaterial.uploadedAt) }}</a-descriptions-item>
          <a-descriptions-item label="索引状态">
            <a-badge v-if="currentMaterial.isIndexed" status="success" text="已建立索引" />
            <a-badge v-else status="processing" text="处理中" />
          </a-descriptions-item>
        </a-descriptions>

        <div v-if="currentMaterial.description" class="material-desc">
          <h4>资料描述</h4>
          <p>{{ currentMaterial.description }}</p>
        </div>

        <div class="material-actions">
          <a-space>
            <a-button type="primary" @click="generateContent(currentMaterial)">
              <RocketOutlined /> 基于此资料生成学习内容
            </a-button>
            <a-button @click="showViewModal = false">关闭</a-button>
          </a-space>
        </div>
      </div>
    </a-modal>

    <!-- 生成内容弹窗 -->
    <a-modal
      v-model:open="showGenerateModal"
      title="AI 生成学习内容"
      width="800px"
      :footer="null"
    >
      <div class="generate-content">
        <a-spin v-if="generating" tip="正在基于知识库生成学习内容..." />

        <div v-if="!generating && generatedContent" class="generated-result">
          <a-alert type="success" message="学习内容生成成功！" style="margin-bottom: 16px" />

          <div class="content-section">
            <h4>📚 学习内容</h4>
            <div class="content-body" v-html="renderMarkdown(generatedContent)"></div>
          </div>

          <div class="action-buttons">
            <a-button type="primary" @click="copyContent">
              <CopyOutlined /> 复制内容
            </a-button>
            <a-button @click="showGenerateModal = false">关闭</a-button>
          </div>
        </div>

        <div v-if="!generating && !generatedContent" class="generate-placeholder">
          <RocketOutlined class="rocket-icon" />
          <p>点击下方按钮，基于已上传的资料生成学习内容</p>
          <a-button type="primary" size="large" @click="startGeneration">
            <RocketOutlined /> 开始生成
          </a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  UploadOutlined,
  ReloadOutlined,
  FileTextOutlined,
  ClusterOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  RocketOutlined,
  CopyOutlined,
} from '@ant-design/icons-vue'
import myAxios from '@/request'
import { courseKnowledgeStructure } from '@/data/knowledgeGraphData'

const loading = ref(false)
const uploading = ref(false)
const generating = ref(false)
const showUploadModal = ref(false)
const showViewModal = ref(false)
const showGenerateModal = ref(false)
const activeTab = ref('all')
const chapterActiveKey = ref(['1'])
const searchKeyword = ref('')

const exampleMaterials = [
  { id: 'example-1', title: '矢量分析基础知识', fileType: 'pdf', chapterNumber: 1, description: '包含标量场与矢量场的基础概念', isIndexed: true, uploadedAt: '2025-05-01', uploadedBy: '管理员' },
  { id: 'example-2', title: '直角坐标系详解', fileType: 'docx', chapterNumber: 1, description: '深入讲解直角坐标系的应用', isIndexed: true, uploadedAt: '2025-05-05', uploadedBy: '管理员' },
  { id: 'example-3', title: '梯度与散度学习笔记', fileType: 'docx', chapterNumber: 1, description: '梯度、散度的定义与计算方法', isIndexed: true, uploadedAt: '2025-05-10', uploadedBy: '管理员' },
  { id: 'example-4', title: '库仑定律课件', fileType: 'ppt', chapterNumber: 2, description: '第二章静电场课件PPT', isIndexed: true, uploadedAt: '2025-05-15', uploadedBy: '管理员' },
  { id: 'example-5', title: '电场强度视频教程', fileType: 'mp4', chapterNumber: 2, description: '电场强度相关概念的视频讲解', isIndexed: false, uploadedAt: '2025-05-20', uploadedBy: '管理员' },
]

const materials = ref<any[]>([])
const materialsByChapter = ref<Record<number, any[]>>({})
const stats = ref({
  totalMaterials: 0,
  totalChunks: 0,
  averageChunksPerMaterial: 0
})

const displayStats = computed(() => {
  if (materials.value.length > 0) {
    return stats.value
  }
  return {
    totalMaterials: exampleMaterials.length,
    totalChunks: exampleMaterials.length * 3,
    averageChunksPerMaterial: 3
  }
})
const currentMaterial = ref<any>(null)
const generatedContent = ref('')

// 章节/小节下拉数据源：与课程内容目录（knowledgeGraphData）保持一致
const chapterOptions = computed(() => [
  ...courseKnowledgeStructure.chapters.map((c: any) => ({
    id: c.id,
    title: c.title,
    chapterNumber: Number((c.id || '').replace('chapter-', '')) || 0,
    sections: (c.sections || []).map((s: any) => ({
      id: s.id,
      title: s.title,
      subsections: s.subsections || []
    }))
  })),
  // 附录A 不在课程内容目录内，但知识库资料中存在，单独补充
  { id: 'chapter-0', title: '附录A 矢量基本运算公式', chapterNumber: 0, sections: [] }
])

const selectedChapterId = ref<string>('')
const selectedSectionId = ref<string>('')
const selectedSubsectionId = ref<string>('')

const sectionOptions = computed(() => {
  const ch = chapterOptions.value.find((c: any) => c.id === selectedChapterId.value)
  return ch ? ch.sections.map((s: any) => ({ id: s.id, title: s.title })) : []
})

const subsectionOptions = computed(() => {
  const ch = chapterOptions.value.find((c: any) => c.id === selectedChapterId.value)
  const sec = ch?.sections.find((s: any) => s.id === selectedSectionId.value)
  return sec ? (sec.subsections || []).map((ss: any) => ({ id: ss.id, title: ss.title })) : []
})

const onChapterChange = () => {
  selectedSectionId.value = ''
  selectedSubsectionId.value = ''
  const ch = chapterOptions.value.find((c: any) => c.id === selectedChapterId.value)
  uploadForm.chapterNumber = ch ? ch.chapterNumber : null
  uploadForm.sectionTitle = ''
  uploadForm.subsectionTitle = ''
}

const onSectionChange = () => {
  selectedSubsectionId.value = ''
  const ch = chapterOptions.value.find((c: any) => c.id === selectedChapterId.value)
  const sec = ch?.sections.find((s: any) => s.id === selectedSectionId.value)
  uploadForm.sectionTitle = sec ? sec.title : ''
  uploadForm.subsectionTitle = ''
}

const onSubsectionChange = () => {
  const ch = chapterOptions.value.find((c: any) => c.id === selectedChapterId.value)
  const sec = ch?.sections.find((s: any) => s.id === selectedSectionId.value)
  const sub = sec?.subsections.find((ss: any) => ss.id === selectedSubsectionId.value)
  uploadForm.subsectionTitle = sub ? sub.title : ''
}

// 下拉搜索：按标题过滤
const filterSelectOption = (input: string, option: any) =>
  String(option?.title ?? '').toLowerCase().includes(String(input ?? '').toLowerCase())

const uploadForm = reactive({
  title: '',
  chapterNumber: null as number | null,
  sectionTitle: '' as string,
  subsectionTitle: '' as string,
  file: null as File | null
})

const fileList = ref<any[]>([])

const columns = [
  { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '类型', dataIndex: 'fileType', key: 'fileType', width: 100 },
  { title: '章节', dataIndex: 'chapterNumber', key: 'chapterNumber', width: 100 },
  { title: '样例', key: 'isExample', width: 80 },
  { title: '索引状态', dataIndex: 'isIndexed', key: 'isIndexed', width: 120 },
  { title: '上传时间', dataIndex: 'uploadedAt', key: 'uploadedAt', width: 180 },
  { title: '操作', key: 'action', width: 200 }
]

const filteredMaterials = computed(() => {
  const realData = searchKeyword.value 
    ? materials.value.filter(m =>
        m.title?.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
        m.description?.toLowerCase().includes(searchKeyword.value.toLowerCase())
      )
    : materials.value
  
  if (realData.length === 0) {
    return exampleMaterials
  }
  return realData
})

const indexedCount = computed(() => materials.value.filter(m => m.isIndexed).length)
const pendingCount = computed(() => materials.value.filter(m => !m.isIndexed).length)

const getFileTypeColor = (type: string) => ({
  pdf: 'red',
  docx: 'blue',
  doc: 'blue',
  txt: 'green',
  md: 'orange'
}[type] || 'default')

const getFileTypeLabel = (type: string) => ({
  pdf: 'PDF',
  docx: 'Word',
  doc: 'Word',
  txt: 'TXT',
  md: 'Markdown'
}[type] || type)

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const fetchMaterials = async () => {
  loading.value = true
  try {
    const res = await myAxios.get('/api/materials/list')
    if (res.data && res.data.success) {
      materials.value = res.data.materials || []
      organizeByChapter()
    } else {
      materials.value = []
    }
  } catch (error) {
    console.error('获取资料列表失败:', error)
    materials.value = []
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const res = await myAxios.get('/api/materials/stats')
    if (res.data && res.data.success) {
      stats.value = res.data.stats
    }
  } catch (error) {
    console.error('获取统计信息失败:', error)
    // 即使出错也设置默认值
    stats.value = {
      totalMaterials: 0,
      totalChunks: 0,
      averageChunksPerMaterial: 0
    }
  }
}

const organizeByChapter = () => {
  const organized: Record<number, any[]> = {}
  materials.value.forEach(material => {
    const chapter = material.chapterNumber || 0
    if (!organized[chapter]) {
      organized[chapter] = []
    }
    organized[chapter].push(material)
  })
  materialsByChapter.value = organized
  chapterActiveKey.value = Object.keys(organized).length > 0 ? [Object.keys(organized)[0]] : []
}

const beforeUpload = (file: File) => {
  const isValidType = ['application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/msword', 'text/plain', 'text/markdown'].includes(file.type) ||
    file.name.endsWith('.pdf') || file.name.endsWith('.docx') || file.name.endsWith('.doc') ||
    file.name.endsWith('.txt') || file.name.endsWith('.md')

  if (!isValidType) {
    message.error('只支持 PDF、Word、TXT、Markdown 格式文件')
    return false
  }

  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    message.error('文件大小不能超过 10MB')
    return false
  }

  uploadForm.file = file
  fileList.value = [file as any]

  // 自动填充标题
  if (!uploadForm.title) {
    uploadForm.title = file.name.replace(/\.[^/.]+$/, '')
  }

  return false // 阻止自动上传
}

const handleRemove = () => {
  uploadForm.file = null
  fileList.value = []
}

const handleUpload = async () => {
  if (!uploadForm.file) {
    message.warning('请选择要上传的文件')
    return
  }
  if (!uploadForm.chapterNumber) {
    message.warning('请选择所属章节')
    return
  }

  uploading.value = true

  const formData = new FormData()
  formData.append('file', uploadForm.file)
  formData.append('title', uploadForm.title || uploadForm.file.name.replace(/\.[^/.]+$/, ''))
  formData.append('chapterNumber', String(uploadForm.chapterNumber))
  if (uploadForm.sectionTitle) {
    formData.append('sectionTitle', uploadForm.sectionTitle)
  }
  if (uploadForm.subsectionTitle) {
    formData.append('subsectionTitle', uploadForm.subsectionTitle)
  }
  formData.append('uploadedBy', 'admin')

  try {
    const res = await myAxios.post('/api/materials/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    if (res.data.success) {
      message.success(res.data.message || '资料上传成功')
      cancelUpload()
      fetchMaterials()
      fetchStats()
    } else {
      message.error(res.data.message || '上传失败')
    }
  } catch (error) {
    console.error('上传失败:', error)
    message.error('上传失败，请重试')
  } finally {
    uploading.value = false
  }
}

const cancelUpload = () => {
  showUploadModal.value = false
  uploadForm.title = ''
  uploadForm.chapterNumber = null
  uploadForm.sectionTitle = ''
  uploadForm.subsectionTitle = ''
  selectedChapterId.value = ''
  selectedSectionId.value = ''
  selectedSubsectionId.value = ''
  uploadForm.file = null
  fileList.value = []
}

const viewMaterial = (material: any) => {
  currentMaterial.value = material
  showViewModal.value = true
}

const deleteMaterial = async (id: string) => {
  try {
    const res = await myAxios.delete(`/api/materials/${id}`)
    if (res.data.success) {
      message.success('删除成功')
      fetchMaterials()
      fetchStats()
    } else {
      message.error(res.data.message || '删除失败')
    }
  } catch (error) {
    console.error('删除失败:', error)
    message.error('删除失败')
  }
}

const handleSearch = (value: string) => {
  searchKeyword.value = value
}

const generateContent = async (material: any) => {
  currentMaterial.value = material
  generatedContent.value = ''
  showGenerateModal.value = true
}

const generateChapterContent = async (chapter: number) => {
  try {
    const res = await myAxios.get(`/api/materials/generate-content/${chapter}`)
    if (res.data.success) {
      generatedContent.value = `基于第 ${chapter} 章的资料，正在生成学习内容...\n\n请稍候...`
      showGenerateModal.value = true
      message.info('正在生成学习内容，请稍候...')
    } else {
      message.warning(res.data.message || '该章节暂无资料')
    }
  } catch (error) {
    console.error('生成内容失败:', error)
    message.error('生成失败')
  }
}

const startGeneration = async () => {
  if (!currentMaterial.value) {
    message.warning('请先选择一个资料')
    return
  }

  const chapter = currentMaterial.value.chapterNumber
  generating.value = true

  try {
    // 按章生成：后端拉取该章全部片段作为上下文（不走 top-K 检索），
    // 整章内容较长，故单独放宽超时到 180s，避免沿用全局 10s 超时。
    const res = await myAxios.post(`/api/materials/generate-chapter/${chapter}`, {}, { timeout: 180000 })

    if (res.data.success) {
      generatedContent.value = res.data.answer
    } else {
      generatedContent.value = res.data.message || '抱歉，无法生成学习内容，请确保知识库中有相关资料。'
    }
  } catch (error) {
    console.error('生成失败:', error)
    generatedContent.value = '生成过程中出现错误，请重试。'
  } finally {
    generating.value = false
  }
}

const copyContent = () => {
  if (generatedContent.value) {
    navigator.clipboard.writeText(generatedContent.value)
    message.success('内容已复制到剪贴板')
  }
}

const renderMarkdown = (content: string) => {
  // 简单的Markdown渲染
  return content
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')
    .replace(/\*\*(.*)\*\*/gim, '<strong>$1</strong>')
    .replace(/\*(.*)\*/gim, '<em>$1</em>')
    .replace(/\n/gim, '<br>')
}

onMounted(() => {
  fetchMaterials()
  fetchStats()
})
</script>

<style scoped>
#material-manage {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.manage-card {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border-radius: 12px;
}

.stats-row {
  margin-bottom: 24px;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

.left-actions,
.right-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.selector-col {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.upload-hint {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #999;
}

.material-detail {
  padding: 10px 0;
}

.material-desc {
  margin-top: 20px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

.material-desc h4 {
  margin: 0 0 8px 0;
  color: #333;
}

.material-desc p {
  margin: 0;
  color: #666;
  line-height: 1.6;
}

.material-actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}

.generate-content {
  min-height: 400px;
}

.generate-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  color: #999;
}

.rocket-icon {
  font-size: 64px;
  margin-bottom: 20px;
  color: #667eea;
}

.content-section {
  margin-top: 20px;
}

.content-section h4 {
  margin: 0 0 12px 0;
  color: #333;
  font-size: 16px;
}

.content-body {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  line-height: 1.8;
  max-height: 500px;
  overflow-y: auto;
}

.action-buttons {
  margin-top: 20px;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.generated-result {
  animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
