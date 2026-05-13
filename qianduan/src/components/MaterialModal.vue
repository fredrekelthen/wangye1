<template>
  <a-modal
    v-model:open="modalVisible"
    title="学习资料管理"
    :footer="null"
    width="700px"
    :destroyOnClose="true"
    @cancel="handleCancel"
  >
    <a-tabs v-model:activeKey="activeTab" centered>
      <!-- 上传标签页 -->
      <a-tab-pane key="upload" tab="上传资料">
        <a-form layout="vertical">
          <!-- 选择文件 -->
          <a-form-item label="选择文件">
            <a-upload
              v-model:file-list="fileList"
              :before-upload="beforeUpload"
              :max-count="1"
              accept=".pdf,.docx,.mp4,.ppt,.pptx"
              @remove="handleRemove"
            >
              <a-button>
                <upload-outlined />
                选择文件
              </a-button>
            </a-upload>
            <div class="upload-tip">
              支持格式：PDF、DOCX、MP4、PPT/PPTX
            </div>
          </a-form-item>

          <!-- 选择位置 -->
          <a-form-item label="选择课程位置">
            <a-tree-select
              v-model:value="selectedPath"
              show-search
              style="width: 100%"
              :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }"
              placeholder="请选择课程章节"
              :tree-data="treeData"
              :field-names="{ label: 'label', value: 'path', children: 'children' }"
              tree-default-expand-all
            />
          </a-form-item>

          <!-- 上传按钮 -->
          <a-form-item>
            <a-button
              type="primary"
              block
              :loading="uploading"
              @click="handleUpload"
            >
              开始上传
            </a-button>
          </a-form-item>
        </a-form>

        <!-- 已上传文件列表 -->
        <a-divider>已上传资料</a-divider>
        <a-list
          :data-source="uploadedMaterials"
          size="small"
          :locale="{ emptyText: '暂无上传资料' }"
        >
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta>
                <template #title>
                  <span>{{ item.fileName }}</span>
                </template>
                <template #description>
                  <span>{{ item.chapter }} - {{ item.type }}</span>
                </template>
              </a-list-item-meta>
              <template #actions>
                <a-button
                  type="link"
                  size="small"
                  danger
                  @click="handleDelete(item)"
                >
                  删除
                </a-button>
              </template>
            </a-list-item>
          </template>
        </a-list>
      </a-tab-pane>

      <!-- 下载标签页 -->
      <a-tab-pane key="download" tab="下载资料">
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索资料"
          enter-button
          style="margin-bottom: 16px"
          @search="handleSearch"
        />
        <a-list
          :data-source="filteredMaterials"
          size="small"
          :locale="{ emptyText: '暂无资料' }"
          style="max-height: 400px; overflow-y: auto"
        >
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta>
                <template #avatar>
                  <div class="file-icon">
                    {{ getFileIcon(item.type) }}
                  </div>
                </template>
                <template #title>
                  <span>{{ item.fileName }}</span>
                </template>
                <template #description>
                  <span>{{ item.chapter }}</span>
                  <a-tag :color="getTagColor(item.type)" style="margin-left: 8px">
                    {{ item.type }}
                  </a-tag>
                </template>
              </a-list-item-meta>
              <template #actions>
                <a-button
                  type="primary"
                  size="small"
                  @click="handleDownload(item)"
                >
                  下载
                </a-button>
              </template>
            </a-list-item>
          </template>
        </a-list>
      </a-tab-pane>
    </a-tabs>
  </a-modal>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { courseKnowledgeStructure } from '@/data/knowledgeGraphData'

const props = defineProps({ visible: { type: Boolean, default: false } })
const emit = defineEmits(['update:visible'])
const modalVisible = computed({ get: () => props.visible, set: v => emit('update:visible', v) })

const activeTab = ref('upload')
const fileList = ref<any[]>([])
const uploading = ref(false)
const selectedPath = ref('')
const searchKeyword = ref('')

// 使用knowledgeGraphData中的三级结构数据
const chapters = computed(() => courseKnowledgeStructure.chapters)

// 树形数据（支持三级：章节 -> 节 -> 小节）
const treeData = computed(() => chapters.value.map(ch => ({
  label: ch.title,
  value: ch.title,
  path: ch.title,
  children: ch.sections.map(section => ({
    label: section.title,
    value: section.title,
    path: `${ch.title}/${section.title}`,
    children: section.subsections.length > 0 
      ? section.subsections.map(sub => ({
          label: sub.title,
          value: sub.title,
          path: `${ch.title}/${section.title}/${sub.title}`
        }))
      : undefined
  }))
})))

// 已上传的资料（优先从后端获取，失败时回退到 localStorage）
const uploadedMaterials = ref<any[]>([])
const loadUploadedMaterials = async () => {
  try {
    const res = await fetch('/api/material/list?pageNum=1&pageSize=100', { method: 'GET', credentials: 'include' })
    const data = await res.json()
    if (data.code === 0 && data.data) {
      // data.data 为分页结果，items 可能在 content 或者直接为数组
      const page = data.data
      let items = []
      if (Array.isArray(page)) items = page
      else if (page.content) items = page.content
      else if (page.docs) items = page.docs
      uploadedMaterials.value = items.map((m:any)=>({ id: m.id || m._id || Date.now(), fileName: m.fileName, fileType: m.fileType, type: m.materialType || m.fileType, path: m.path, chapter: m.chapterTitle, lesson: m.lessonTitle, fileUrl: m.filePath || m.fileUrl, filePath: m.filePath, fileSize: m.fileSize }))
      localStorage.setItem('uploadedMaterials', JSON.stringify(uploadedMaterials.value))
      return
    }
  } catch (e) {
    // ignore, fallback to localStorage
  }
  uploadedMaterials.value = JSON.parse(localStorage.getItem('uploadedMaterials') || '[]')
}

// 过滤后的资料列表
const filteredMaterials = computed(() => {
  const kw = (searchKeyword.value || '').toLowerCase()
  return kw ? uploadedMaterials.value.filter(i => i.fileName.toLowerCase().includes(kw) || i.chapter.toLowerCase().includes(kw)) : uploadedMaterials.value
})

// 文件上传前检查
const beforeUpload = (file:any) => { const valid = ['application/pdf','application/vnd.openxmlformats-officedocument.wordprocessingml.document','video/mp4','application/vnd.ms-powerpoint','application/vnd.openxmlformats-officedocument.presentationml.presentation']; if (!valid.includes(file.type)) return message.error('只能上传 PDF、DOCX、MP4、PPT/PPTX 格式的文件！') || false; if (file.size/1024/1024 >=5) return message.error('文件大小不能超过 5MB（localStorage 限制）！') || false; return true }

// 移除文件
const handleRemove = () => {
  fileList.value = [];
};

// 处理上传：先上传文件，获得 filePath，再提交资料元数据到后端保存
const handleUpload = async () => {
  if (!fileList.value.length) return message.error('请先选择文件！')
  if (!selectedPath.value) return message.error('请选择课程位置！')
  uploading.value = true
  try {
    const file = fileList.value[0].originFileObj
    const maxSize = 50 * 1024 * 1024 // 50MB 单文件上限
    if (file.size > maxSize) return message.error('文件大小不能超过 50MB！')

    // 上传文件到后端
    const form = new FormData()
    form.append('file', file)
    const res = await fetch('/api/material/upload-file', { method: 'POST', body: form, credentials: 'include' })
    const data = await res.json()
    if (data.code !== 0) {
      return message.error(data.description || '文件上传失败')
    }

    const fileInfo = data.data
    const parts = (selectedPath.value || '').split('/')
    const chapter = parts[0] || ''
    const lesson = parts.slice(1).join('/') || ''

    // 提交元数据到后端创建 Material 记录
    const metaBody = {
      fileName: fileInfo.fileName || file.name,
      fileType: fileInfo.fileType,
      filePath: fileInfo.filePath,
      fileSize: fileInfo.fileSize,
      chapterTitle: chapter,
      lessonTitle: lesson,
      description: '',
      md5: fileInfo.md5,
      isPublic: true
    }
    const metaRes = await fetch('/api/material/upload', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(metaBody), credentials: 'include' })
    const metaData = await metaRes.json()
    if (metaData.code !== 0) {
      return message.error(metaData.description || '保存资料元数据失败')
    }

    const saved = metaData.data
    const newMaterial = { id: saved.id || Date.now(), fileName: saved.fileName, fileType: saved.fileType, type: saved.materialType || saved.fileType, path: saved.path || selectedPath.value, chapter: saved.chapterTitle || chapter, lesson: saved.lessonTitle || lesson, fileUrl: saved.filePath, filePath: saved.filePath, fileSize: saved.fileSize }
    uploadedMaterials.value.push(newMaterial)
    localStorage.setItem('uploadedMaterials', JSON.stringify(uploadedMaterials.value))
    message.success('上传成功！')
    fileList.value = []; selectedPath.value = '';
    window.dispatchEvent(new CustomEvent('material-uploaded',{detail:newMaterial}))
  } catch (e:any) { message.error('上传失败：' + (e?.message||e)) } finally { uploading.value = false }
}

// 删除资料
const handleDelete = (item:any)=>{ uploadedMaterials.value = uploadedMaterials.value.filter(m=>m.id!==item.id); localStorage.setItem('uploadedMaterials', JSON.stringify(uploadedMaterials.value)); message.success('删除成功！') }

// 搜索
const handleSearch = () => {
  // searchKeyword 的变化会自动触发 filteredMaterials 的更新
};

// 下载/预览资料
const handleDownload = (item:any)=>{
  if (!item.filePath && !item.fileUrl) return message.error('文件路径不存在')
  const url = item.filePath || item.fileUrl
  window.open(url, '_blank')
  message.success('打开：' + item.fileName)
}

// 获取文件图标
const getFileIcon = (t:string)=>({PDF:'📄',DOCX:'📝','视频':'🎬',PPT:'📊'}[t]||'📁')

// 获取标签颜色
const getTagColor = (t:string)=>({PDF:'blue',DOCX:'green','视频':'red',PPT:'orange'}[t]||'default')

// 处理取消
const handleCancel = () => { modalVisible.value = false; fileList.value = []; selectedPath.value = ''; searchKeyword.value = '' }
onMounted(loadUploadedMaterials)
</script>

<style scoped>
.upload-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.file-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  background: #f5f5f5;
  border-radius: 8px;
}

:deep(.ant-btn-primary) {
  background: linear-gradient(135deg, #007BFF 0%, #00B894 100%);
  border-color: #00B894;
}

:deep(.ant-btn-primary:hover) {
  background: linear-gradient(135deg, #00B894 0%, #55E6C1 100%);
  border-color: #55E6C1;
}

:deep(.ant-tabs-tab-active) {
  color: #007BFF !important;
}
</style>
