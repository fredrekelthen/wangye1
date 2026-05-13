<template>
  <div id="material-manage">
    <a-card title="资料管理" class="manage-card">
      <!-- 操作按钮 -->
      <div style="margin-bottom: 16px; display: flex; gap: 12px; align-items: center;">
        <a-button type="primary" @click="showAddModal">
          <template #icon><PlusOutlined /></template> 添加资料
        </a-button>
        <span style="color: #666;">选择对应的学习内容后，为该内容添加新的学习资料</span>
      </div>

      <!-- 统计信息 -->
      <a-row :gutter="16" style="margin-bottom: 20px;">
        <a-col :span="8">
          <a-statistic title="总章节数" :value="statistics.totalChapters" style="background: #f0f2f5; padding: 16px; border-radius: 8px;" />
        </a-col>
        <a-col :span="8">
          <a-statistic title="总资料数" :value="statistics.totalMaterials" style="background: #f0f2f5; padding: 16px; border-radius: 8px;" />
        </a-col>
        <a-col :span="8">
          <a-statistic title="总小节数" :value="statistics.totalSections" style="background: #f0f2f5; padding: 16px; border-radius: 8px;" />
        </a-col>
      </a-row>

      <!-- 按章节展示的资料列表 -->
      <a-collapse v-model:activeKey="activeKeys" accordion>
        <a-collapse-panel
          v-for="(chapter, index) in materials"
          :key="index"
          :header="chapter.title || chapter.chapterTitle"
        >
          <template #extra>
            <a-tag color="blue">{{ chapter.sections?.length || 0 }} 节</a-tag>
          </template>

          <div class="chapter-content">
            <h4>描述</h4>
            <p>{{ chapter.description || '暂无描述' }}</p>

            <!-- 小节/学习内容列表 -->
            <a-collapse v-if="chapter.sections && chapter.sections.length > 0">
              <a-collapse-panel
                v-for="(section, sIndex) in chapter.sections"
                :key="sIndex"
                :header="section.title || section.sectionTitle"
              >
                <template #extra>
                  <a-tag>{{ section.subsections?.length || section.materials?.length || 0 }} 小节</a-tag>
                </template>

                <!-- 资料表格 -->
                <a-table
                  v-if="section.subsections && section.subsections.length > 0"
                  :columns="subsectionColumns"
                  :data-source="section.subsections"
                  :pagination="false"
                  size="small"
                  row-key="id"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'action'">
                      <a-button type="link" size="small">查看</a-button>
                      <a-popconfirm title="确定删除该资料？" @confirm="handleDeleteMaterial(record.id)">
                        <a-button type="link" danger size="small">删除</a-button>
                      </a-popconfirm>
                    </template>
                  </template>
                </a-table>

                <a-empty v-else description="暂无资料" />
              </a-collapse-panel>
            </a-collapse>

            <a-empty v-else description="暂无小节" />
          </div>
        </a-collapse-panel>
      </a-collapse>

      <a-empty v-if="!loading && materials.length === 0" description="暂无课程资料" />
      <a-spin v-if="loading" tip="加载中..." />
    </a-card>

    <!-- 添加资料弹窗 -->
    <a-modal v-model:open="addModalVisible" title="添加学习资料" @ok="handleAddMaterial" ok-text="确认添加" cancel-text="取消" width="600px">
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="所属章节" required>
          <a-select v-model:value="newMaterial.chapterIndex" placeholder="请选择章节" @change="onChapterChange">
            <a-select-option v-for="(ch, idx) in materials" :key="idx" :value="idx">{{ ch.title || ch.chapterTitle }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="学习内容" required>
          <a-select v-model:value="newMaterial.sectionIndex" placeholder="请先选择章节" :disabled="newMaterial.chapterIndex === null || newMaterial.chapterIndex === undefined">
            <a-select-option v-for="(sec, idx) in currentSections" :key="idx" :value="idx">{{ sec.title || sec.sectionTitle }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="资料标题" required>
          <a-input v-model:value="newMaterial.title" placeholder="请输入资料标题" />
        </a-form-item>
        <a-form-item label="资料类型" required>
          <a-radio-group v-model:value="newMaterial.type">
            <a-radio value="text">文本内容</a-radio>
            <a-radio value="video">视频链接</a-radio>
            <a-radio value="pdf">PDF文档</a-radio>
            <a-radio value="image">图片</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="资料内容" required>
          <a-textarea v-if="newMaterial.type === 'text'" v-model:value="newMaterial.content" placeholder="请输入文本内容" :rows="4" />
          <a-input v-else v-model:value="newMaterial.content" :placeholder="'请输入' + (newMaterial.type === 'video' ? '视频链接' : newMaterial.type === 'pdf' ? 'PDF地址' : '图片地址')" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="newMaterial.description" placeholder="请输入资料描述（可选）" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import myAxios from '@/request'

const loading = ref(false)
const materials = ref<any[]>([])
const activeKeys = ref<string[]>(['0'])
const addModalVisible = ref(false)

// 新增资料表单数据
const newMaterial = reactive({
  chapterIndex: null as number | null,
  sectionIndex: null as number | null,
  title: '',
  type: 'text',
  content: '',
  description: '',
})

// 当前选中章节的小节列表
const currentSections = computed(() => {
  if (newMaterial.chapterIndex === null || newMaterial.chapterIndex === undefined) return []
  const chapter = materials.value[newMaterial.chapterIndex]
  return chapter?.sections || []
})

const statistics = reactive({
  totalChapters: 0,
  totalMaterials: 0,
  totalSections: 0,
})

const subsectionColumns = [
  {
    title: '标题',
    dataIndex: 'title',
    key: 'title',
    ellipsis: true,
  },
  {
    title: '类型',
    dataIndex: 'type',
    key: 'type',
    width: 100,
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 80,
  },
  {
    title: '操作',
    key: 'action',
    width: 150,
  },
]

// 获取课程资料
const fetchMaterials = async () => {
  loading.value = true
  try {
    const res = await myAxios.get('/api/admin/materials-grouped')
    if (res.data.code === 0) {
      const courseData = res.data.data

      if (courseData.chapters) {
        materials.value = courseData.chapters
        calculateStatistics(courseData.chapters)
      } else if (Array.isArray(courseData)) {
        materials.value = courseData
        calculateStatistics(courseData)
      }
    } else {
      message.error(res.data.message || '获取资料列表失败')
    }
  } catch (error) {
    console.error('获取资料列表失败:', error)
    message.error('获取资料列表失败')
  } finally {
    loading.value = false
  }
}

// 计算统计数据
const calculateStatistics = (chapters: any[]) => {
  let sections = 0
  let subsections = 0

  chapters.forEach((chapter) => {
    if (chapter.sections) {
      sections += chapter.sections.length
      chapter.sections.forEach((section: any) => {
        if (section.subsections) {
          subsections += section.subsections.length
        }
      })
    }
  })

  statistics.totalChapters = chapters.length
  statistics.totalSections = sections + subsections
  statistics.totalMaterials = subsections
}

// 显示添加资料弹窗
const showAddModal = () => {
  newMaterial.chapterIndex = null
  newMaterial.sectionIndex = null
  newMaterial.title = ''
  newMaterial.type = 'text'
  newMaterial.content = ''
  newMaterial.description = ''
  addModalVisible.value = true
}

// 章节变更时清空小节选择
const onChapterChange = () => {
  newMaterial.sectionIndex = null
}

// 添加资料
const handleAddMaterial = async () => {
  // 验证表单
  if (newMaterial.chapterIndex === null || newMaterial.chapterIndex === undefined) {
    message.warning('请选择所属章节')
    return
  }
  if (newMaterial.sectionIndex === null || newMaterial.sectionIndex === undefined) {
    message.warning('请选择学习内容')
    return
  }
  if (!newMaterial.title) {
    message.warning('请输入资料标题')
    return
  }
  if (!newMaterial.content) {
    message.warning('请输入资料内容')
    return
  }

  try {
    const res = await myAxios.post('/api/admin/material', {
      chapterIndex: newMaterial.chapterIndex,
      sectionIndex: newMaterial.sectionIndex,
      title: newMaterial.title,
      type: newMaterial.type,
      content: newMaterial.content,
      description: newMaterial.description,
    })

    if (res.data.code === 0) {
      message.success('添加成功')
      addModalVisible.value = false
      fetchMaterials()
    } else {
      message.error(res.data.message || '添加失败')
    }
  } catch (error) {
    console.error('添加资料失败:', error)
    message.error('添加资料失败')
  }
}

// 删除资料
const handleDeleteMaterial = async (materialId: string) => {
  try {
    const res = await myAxios.delete(`/api/admin/material/${materialId}`)
    if (res.data.code === 0) {
      message.success('删除成功')
      fetchMaterials()
    } else {
      message.error(res.data.message || '删除失败')
    }
  } catch (error) {
    console.error('删除资料失败:', error)
    message.error('删除资料失败')
  }
}

onMounted(() => {
  fetchMaterials()
})
</script>

<style scoped>
#material-manage {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.manage-card {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.chapter-content h4 {
  margin-bottom: 8px;
  color: #333;
  font-size: 14px;
}

.chapter-content p {
  color: #666;
  margin-bottom: 16px;
}
</style>
