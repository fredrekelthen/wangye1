<template>
  <div class="course-tree">
    <div class="tree-header">
      <div class="title">目 录</div>
      <div class="count">{{ totalSubsections }}</div>
    </div>

    <ul class="chapters">
      <li v-for="(chapter, cIdx) in chapters" :key="chapter.id" class="chapter">
        <div class="chapter-title" @click="toggleChapter(cIdx)">
          <span class="marker">{{ chapterExpanded[cIdx] ? '▾' : '▸' }}</span>
          <span class="cname">{{ chapter.title }}</span>
          <span class="chapter-count">{{ chapter.sections.length }}节</span>
        </div>

        <ul v-show="chapterExpanded[cIdx]" class="sections">
          <li v-for="(section, sIdx) in chapter.sections" :key="section.id" class="section">
            <div class="section-title" @click="toggleSection(cIdx, sIdx)">
              <span class="marker">{{ sectionExpanded[cIdx]?.[sIdx] ? '▾' : '▸' }}</span>
              <span class="sname">{{ section.title }}</span>
              <span class="section-count">{{ section.subsections.length }}小节</span>
            </div>

            <ul v-show="sectionExpanded[cIdx]?.[sIdx]" class="subsections">
              <li v-for="subsection in section.subsections" :key="subsection.id" class="subsection">
                <span class="icon">📄</span>
                <span
                  class="label clickable"
                  @click="handleSubClick(chapter, section, subsection)"
                >
                  {{ subsection.title }}
                </span>
                <template v-if="hasMaterial(chapter.id, section.title, subsection.title)">
                  <span class="status read">已上传</span>
                  <a-button
                    type="link"
                    size="small"
                    class="download-btn"
                    @click.stop="handleOpen(materialOf(chapter.id, section.title, subsection.title))"
                  >
                    <eye-outlined />
                    查看
                  </a-button>
                </template>
                <span v-else class="status unread">未读</span>
              </li>
            </ul>
          </li>
          
          <!-- 章节习题 -->
          <li class="section homework-section">
            <div class="section-title homework" @click="handleItemClick(chapter.title, `${chapter.title}习题`)">
              <span class="icon">📝</span>
              <span class="sname">{{ chapter.title }}习题</span>
              <span class="status todo">未完成</span>
            </div>
          </li>
        </ul>
      </li>
    </ul>


  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { EyeOutlined } from '@ant-design/icons-vue'
import { courseKnowledgeStructure } from '@/data/knowledgeGraphData'
import myAxios from '@/request'

// 资料来源：后端真实资料库（上传后已建立 RAG 索引）
const materials = ref([] as any[])
const loadMaterials = async () => {
  try {
    const res = await myAxios.get('/api/materials/list')
    materials.value = res.data?.success ? (res.data.materials || []) : []
  } catch (e) {
    materials.value = []
  }
}
const chapterNumber = (chapterId: string) => Number((chapterId || '').replace('chapter-', '')) || 0

// 匹配某三级标题节点下的资料（按章号 + 节标题 + 三级标题）
const materialsAt = (chapterId: string, sectionTitle: string, subsectionTitle: string) =>
  materials.value.filter(m =>
    m.chapterNumber === chapterNumber(chapterId) &&
    (!m.sectionTitle || m.sectionTitle === sectionTitle) &&
    (!m.subsectionTitle || m.subsectionTitle === subsectionTitle)
  )
const hasMaterial = (chapterId: string, sectionTitle: string, subsectionTitle: string) =>
  materialsAt(chapterId, sectionTitle, subsectionTitle).length > 0
const materialOf = (chapterId: string, sectionTitle: string, subsectionTitle: string) =>
  materialsAt(chapterId, sectionTitle, subsectionTitle)[0]

// 点击三级节点：关闭知识图谱，展示该小节的介绍与学习资料
const handleSubClick = (chapter: any, section: any, subsection: any) => {
  window.dispatchEvent(new CustomEvent('subsection-selected', {
    detail: {
      chapterId: chapter.id,
      chapter: chapter.title,
      section: section.title,
      subsection: subsection.title,
      description: subsection.description || ''
    }
  }))
}
const handleOpen = (m: any) => {
  if (!m) return
  window.open('/api/materials/' + m.id + '/file', '_blank')
}
const handleItemClick = (c: string, l: string) => window.dispatchEvent(new CustomEvent('course-selected', { detail: { chapter: c, lesson: l } }))

onMounted(() => {
  loadMaterials()
  window.addEventListener('material-uploaded', loadMaterials)
})
onUnmounted(() => window.removeEventListener('material-uploaded', loadMaterials))

// 使用 knowledgeGraphData 中的数据
const chapters = ref(courseKnowledgeStructure.chapters)

// 计算总小节数
const totalSubsections = computed(() => {
  return chapters.value.reduce((total, chapter) => {
    return total + chapter.sections.reduce((sTotal, section) => {
      return sTotal + section.subsections.length
    }, 0)
  }, 0)
})

// 章节展开状态
const chapterExpanded = ref<boolean[]>(Array(chapters.value.length).fill(false))
const sectionExpanded = ref<Record<number, boolean[]>>({})

// 初始化章节展开状态
chapters.value.forEach((_, idx) => {
  sectionExpanded.value[idx] = Array(chapters.value[idx].sections.length).fill(false)
})

const toggleChapter = (cIdx: number) => {
  chapterExpanded.value[cIdx] = !chapterExpanded.value[cIdx]
  window.dispatchEvent(new CustomEvent('chapter-selected', { detail: { chapter: chapters.value[cIdx].title } }))
}

const toggleSection = (cIdx: number, sIdx: number) => {
  if (!sectionExpanded.value[cIdx]) {
    sectionExpanded.value[cIdx] = Array(chapters.value[cIdx].sections.length).fill(false)
  }
  sectionExpanded.value[cIdx][sIdx] = !sectionExpanded.value[cIdx][sIdx]
}
</script>

<style scoped>
.course-tree {
  background: #fff;
  border: 1px solid #eaeaea;
  padding: 12px;
  border-radius: 6px;
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

.tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px dashed #eee;
}

.tree-header .title {
  font-size: 16px;
  font-weight: 600;
  color: #007BFF;
}

.tree-header .count {
  background: #F0F9F7;
  color: #007BFF;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.chapters {
  list-style: none;
  padding: 0;
  margin: 12px 0;
}

.chapter {
  margin-bottom: 6px;
}

.chapter-title {
  cursor: pointer;
  padding: 8px 10px;
  border-radius: 4px;
  background: #F0F9F7;
  border: 1px solid #D4EDDA;
  display: flex;
  align-items: center;
  gap: 8px;
}

.chapter-title .marker {
  color: #007BFF;
  font-size: 12px;
}

.chapter-title .cname {
  flex: 1;
  font-weight: 500;
}

.chapter-title .chapter-count {
  font-size: 12px;
  color: #666;
  background: #e8f5e9;
  padding: 2px 6px;
  border-radius: 10px;
}

.sections {
  list-style: none;
  padding-left: 12px;
  margin: 6px 0;
}

.section {
  margin-bottom: 4px;
}

.section-title {
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 4px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.section-title.homework {
  background: #fff8e1;
  border-color: #ffe082;
}

.section-title .marker {
  color: #00B894;
  font-size: 11px;
}

.section-title .sname {
  flex: 1;
}

.section-title .section-count {
  font-size: 11px;
  color: #888;
}

.subsections {
  list-style: none;
  padding-left: 20px;
  margin: 4px 0;
}

.subsection {
  display: flex;
  align-items: center;
  padding: 5px 4px;
  gap: 6px;
  font-size: 13px;
  border-bottom: 1px dashed #f0f0f0;
}

.subsection:last-child {
  border-bottom: none;
}

.icon {
  width: 18px;
  font-size: 12px;
}

.label {
  flex: 1;
  color: #333;
}

.label.clickable {
  cursor: pointer;
  color: #007BFF;
  text-decoration: underline;
}

.label.clickable:hover {
  color: #00B894;
}

.status {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 10px;
}

.status.read {
  background: #f0f0f0;
  color: #666;
}

.status.done {
  background: #D4EDDA;
  color: #007BFF;
}

.status.unread {
  background: #f8f8f8;
  color: #999;
}

.status.todo {
  background: #fff3cd;
  color: #856404;
}

.download-btn {
  padding: 0;
  height: auto;
  font-size: 11px;
  color: #007BFF;
}

.download-btn:hover {
  color: #00B894;
}

.homework-section .section-title {
  gap: 8px;
}

.homework-section .icon {
  width: 18px;
}


</style>
