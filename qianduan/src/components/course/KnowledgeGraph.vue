<template>
  <div class="knowledge-graph-container">
    <div class="graph-header" v-if="viewMode === 'chapter'">
      <a-button size="small" @click="returnToCourseView" class="back-btn">
        &larr; 返回课程总览
      </a-button>
      <span class="graph-header-title">{{ chapterTitle }}</span>
    </div>

    <!-- G6 渲染容器 -->
    <div class="g6-container" ref="graphRef"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { Graph } from '@antv/g6'
import { courseKnowledgeStructure } from '@/data/knowledgeGraphData'

interface Props {
  title?: string
  chapterId?: number
}

const props = withDefaults(defineProps<Props>(), {
  title: '知识图谱'
})

const emit = defineEmits<{
  (e: 'nodeClick', node: any): void
}>()

const graphRef = ref<HTMLDivElement | null>(null)
const viewMode = ref<'course' | 'chapter'>('course')
const chapterTitle = ref('')
let graph: any = null

// 防抖：避免快速连续点击导致的问题
let clickDebounceTimer: any = null
const debounceClick = (fn: () => void, delay = 200) => {
  if (clickDebounceTimer) {
    clearTimeout(clickDebounceTimer)
  }
  clickDebounceTimer = setTimeout(fn, delay)
}

// 页面风格一致的配色方案
const COLORS = {
  root: { 
    fill: '#1890ff', 
    stroke: '#096dd9', 
    text: '#ffffff' 
  },
  level1: { 
    fill: '#e6f7ff', 
    stroke: '#91d5ff', 
    text: '#1890ff' 
  },
  level2: { 
    fill: '#f6ffed', 
    stroke: '#b7eb8f', 
    text: '#389e0d' 
  },
  edge: { stroke: '#91d5ff' },
}

// 每个章节的核心知识点ID列表（各选取3-4个最核心的知识点）
const CORE_SUBSECTIONS = {
  'chapter-1': ['1-2-2', '1-3-3', '1-4-2', '1-5-2'], // 直角坐标系、梯度、散度、旋度
  'chapter-2': ['2-1-1', '2-4-1', '2-6-1', '2-8-1'], // 库仑定律、电场强度与电位、导体、边界条件
  'chapter-3': ['3-1-2', '3-2-3', '3-5-1', '3-6-1'], // 毕奥-萨伐尔定律、安培环路定理、自电感、磁场能量
  'chapter-4': ['4-1-2', '4-2-1', '4-3-1', '4-4-1'], // 唯一性定理、分离变量法、镜像法、有限差分法
  'chapter-5': ['5-1-1', '5-1-3', '5-4-1', '5-4-2'], // 麦克斯韦第一、三方程、坡印亭定理、复坡印亭矢量
  'chapter-6': ['6-1-2', '6-2-1', '6-2-2', '6-3-4'], // 平面波特性、线极化、圆极化、趋肤效应
  'chapter-7': ['7-3-1', '7-4-3', '7-7-1', '7-7-6'], // 垂直极化斜入射、全反射/表面波、负折射效应、完美吸波材料
  'chapter-8': ['8-1-1', '8-2-1', '8-3-1', '8-3-3'], // TEM波、平行双线、矩形波导TM波、简并模与主模
  'chapter-9': ['9-2-2', '9-5-2', '9-6-1', '9-6-5'], // 电偶极子辐射场、远区场、方向性函数、增益
}

/**
 * 构建课程总览图谱数据（显示根节点、章节节点和核心知识点）
 */
function buildCourseData() {
  const nodes: any[] = []
  const edges: any[] = []
  const totalChapters = courseKnowledgeStructure.chapters.length

  // 根节点：电磁场与电磁波
  nodes.push({
    id: 'root',
    data: {
      label: '电磁场与电磁波',
      description: '课程总览',
      type: 'root',
      nodeType: 'root',
      side: 'center',
    },
  })

  // 一级节点：章节名 + 三级节点：核心知识点
  courseKnowledgeStructure.chapters.forEach((chapter, index) => {
    const chapterNum = parseInt(chapter.id.replace('chapter-', ''), 10)
    const side = index < Math.ceil(totalChapters / 2) ? 'left' : 'right'

    // 添加章节节点
    nodes.push({
      id: chapter.id,
      data: {
        label: chapter.title.replace(/^第\d章\s*/, ''),
        fullLabel: chapter.title,
        description: chapter.description,
        type: 'chapter',
        nodeType: 'level1',
        chapterId: chapterNum,
        sectionCount: chapter.sections.length,
        side,
      },
    })
    edges.push({ id: `edge-root-${chapter.id}`, source: 'root', target: chapter.id })

    // 添加该章节的核心知识点（三级节点）
    const coreSubsectionIds = CORE_SUBSECTIONS[chapter.id as keyof typeof CORE_SUBSECTIONS] || []
    coreSubsectionIds.forEach((subId) => {
      // 从所有小节的 subsections 中查找对应的知识点
      for (const section of chapter.sections) {
        const subsection = section.subsections.find((sub: any) => sub.id === subId)
        if (subsection) {
          nodes.push({
            id: subsection.id,
            data: {
              label: subsection.title.replace(/^\d+\.\d+\.\d+\s*/, ''), // 移除序号
              description: subsection.description,
              type: 'subsection',
              nodeType: 'level2',
              chapterId: chapterNum,
              chapterTitle: chapter.title,
              sectionTitle: section.title,
              side,
            },
          })
          edges.push({ id: `edge-${chapter.id}-${subsection.id}`, source: chapter.id, target: subsection.id })
          break
        }
      }
    })
  })

  return { nodes, edges }
}

/**
 * 构建单章节图谱数据
 */
function buildChapterData(chapterId: number) {
  const chapter = courseKnowledgeStructure.chapters.find(
    (c) => c.id === `chapter-${chapterId}`
  )
  if (!chapter) return null

  const nodes: any[] = []
  const edges: any[] = []
  const totalSections = chapter.sections.length

  nodes.push({
    id: 'root',
    data: {
      label: chapter.title.replace(/^第\d章\s*/, ''),
      fullLabel: chapter.title,
      description: chapter.description,
      type: 'root-chapter',
      nodeType: 'root',
      side: 'center',
    },
  })

  chapter.sections.forEach((section, index) => {
    const side = index < Math.ceil(totalSections / 2) ? 'left' : 'right'
    nodes.push({
      id: section.id,
      data: {
        label: section.title,
        description: section.description,
        type: 'section',
        nodeType: 'level1',
        chapterId,
        chapterTitle: chapter.title,
        side,
      },
    })
    edges.push({ id: `edge-root-${section.id}`, source: 'root', target: section.id })

    section.subsections.forEach((sub) => {
      nodes.push({
        id: sub.id,
        data: {
          label: sub.title,
          description: sub.description,
          type: 'subsection',
          nodeType: 'level2',
          chapterId,
          chapterTitle: chapter.title,
          sectionTitle: section.title,
          side, 
        },
      })
      edges.push({ id: `edge-${section.id}-${sub.id}`, source: section.id, target: sub.id })
    })
  })

  return { nodes, edges }
}

// 创建 G6 实例并渲染
function renderGraph() {
  if (!graphRef.value) return

  // 销毁旧图谱实例
  if (graph) {
    try {
      // 先移除所有事件监听器
      graph.off('node:click')
      graph.off('node:pointerenter')
      graph.off('node:pointerleave')
      graph.destroy()
    } catch { /* 已销毁或无效，忽略 */ }
    graph = null
  }

  const isCourseMode = props.chapterId === undefined || props.chapterId < 0
  viewMode.value = isCourseMode ? 'course' : 'chapter'

  let graphData: any
  if (isCourseMode) {
    graphData = buildCourseData()
  } else {
    graphData = buildChapterData(props.chapterId)
    if (!graphData) return
    chapterTitle.value =
      courseKnowledgeStructure.chapters.find(
        (c) => c.id === `chapter-${props.chapterId}`
      )?.title || ''
  }

  graph = new Graph({
    container: graphRef.value,
    autoFit: 'view',
    padding: [40, 80],
    layout: {
      type: 'mindmap',
      direction: 'H',
      getHeight: () => 36,
      getWidth: (d: any) => {
        const label = d.data?.data?.label || d.data?.label || d.label || ''
        return Math.min(Math.max(label.length * 14 + 30, 100), 260)
      },
      getVGap: () => 18,
      // 【优化点】：拉大水平间距，给贝塞尔曲线充足的舒展空间，让发散感更强
      getHGap: () => 80, 
      getSide: (d: any) => {
        return d.data?.data?.side || d.data?.side || d.side || 'right'
      },
    },
    behaviors: [
      { type: 'drag-element', enable: true },
      { type: 'zoom-canvas', sensitivity: 1, minZoom: 0.25, maxZoom: 3, enableOptimize: true },
      { type: 'drag-canvas' }, 
      { type: 'collapse-expand', trigger: 'click', animation: { duration: 200 } },
    ],
    node: {
      type: 'rect',
      style: (d: any) => {
        const nodeType = d.data?.nodeType || ''
        const isRoot = nodeType === 'root'
        const isLevel1 = nodeType === 'level1'
        const label = d.data?.label || ''

        let fillColor, textColor, shadowColor, strokeColor
        if (isRoot) {
          fillColor = '#1890ff'
          textColor = '#ffffff'
          shadowColor = 'rgba(24, 144, 255, 0.3)'
          strokeColor = '#096dd9'
        } else if (isLevel1) {
          fillColor = '#e6f7ff'
          textColor = '#1890ff'
          shadowColor = 'rgba(24, 144, 255, 0.15)'
          strokeColor = '#91d5ff'
        } else {
          fillColor = '#f6ffed'
          textColor = '#389e0d'
          shadowColor = 'rgba(56, 158, 13, 0.15)'
          strokeColor = '#b7eb8f'
        }

        let fontSize = 13
        let nodeWidth = 160
        let nodeHeight = 44
        if (isRoot) {
          fontSize = 17
          nodeWidth = 220
          nodeHeight = 56
        } else if (isLevel1) {
          fontSize = 14
          nodeWidth = 180
          nodeHeight = 48
        } else {
          fontSize = 12
          nodeWidth = 170
          nodeHeight = 42
        }

        const labelLength = label.length
        if (labelLength > 8) {
          nodeWidth += (labelLength - 8) * 10
        }

        return {
          size: [nodeWidth, nodeHeight],
          radius: 8,
          fill: fillColor,
          stroke: strokeColor,
          lineWidth: isRoot ? 0 : 1.5,
          shadowColor: shadowColor,
          shadowBlur: isRoot ? 16 : 8,
          shadowOffsetX: 0,
          shadowOffsetY: isRoot ? 6 : 4,
          cursor: 'pointer',
          labelText: label,
          labelFill: textColor,
          labelFontSize: fontSize,
          labelFontWeight: isRoot ? 700 : 600,
          labelFontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
          labelPlacement: 'center',
          labelTextAlign: 'center',
          labelTextBaseline: 'middle',
          ports: [
            { key: 'left', placement: [0, 0.5] },
            { key: 'right', placement: [1, 0.5] }
          ]
        }
      },
      state: {
        hover: {
          lineWidth: 2.5,
          stroke: '#40a9ff',
          shadowBlur: 16,
          transform: 'translateY(-3px)'
        },
        selected: {
          lineWidth: 3,
          stroke: '#1890ff',
          shadowBlur: 20
        }
      },
      animation: {
        appear: {
          animation: 'scale-in',
          duration: 600,
          easing: 'easeOutElastic'
        }
      }
    },
    edge: {
      type: 'cubic-horizontal',
      style: {
        endArrow: false,
        stroke: '#91d5ff',
        lineWidth: 2,
        shadowColor: 'rgba(24, 144, 255, 0.1)',
        shadowBlur: 3
      }
    },
  })

  // 事件绑定：点击节点处理（添加节点存在性检查 + 防抖）
  graph.on('node:click', (evt: any) => {
    debounceClick(() => {
      try {
        const nodeId = evt.itemId || evt.id || evt.target?.id;
        if (!nodeId || !graph) return;

        // 检查节点是否存在于当前图谱中
        const nodeData = graph.getNodeData(nodeId);
        if (!nodeData) return;

        const businessData = nodeData?.data || nodeData;
        if (businessData) {
          handleNodeClick(businessData);
        }
      } catch (error) {
        // 图谱重新渲染期间可能出现节点不存在的情况，忽略
        console.debug('Node click error:', error);
      }
    })
  })

  // 悬停高亮
  graph.on('node:pointerenter', (evt: any) => {
    try {
      const nodeId = evt.itemId || evt.id || evt.target?.id;
      if (nodeId && graph) graph.setElementState(nodeId, 'hover', true)
    } catch (error) {
      // 图谱重新渲染期间可能出现节点不存在的情况，忽略
    }
  })

  // 移开失去高亮
  graph.on('node:pointerleave', (evt: any) => {
    try {
      const nodeId = evt.itemId || evt.id || evt.target?.id;
      if (nodeId && graph) graph.setElementState(nodeId, 'hover', false)
    } catch (error) {
      // 图谱重新渲染期间可能出现节点不存在的情况，忽略
    }
  })

  graph.setData(graphData)
  graph.render()
}

// 节点点击处理：添加完整的错误检查
function handleNodeClick(nodeData: any) {
  if (!nodeData) return

  const type = nodeData.type

  // 知识点点击：触发课程选择事件
  if ((type === 'section' || type === 'subsection') && nodeData.chapterTitle) {
    try {
      window.dispatchEvent(
        new CustomEvent('course-selected', {
          detail: {
            chapter: nodeData.chapterTitle,
            lesson: nodeData.label,
          },
        })
      )
      message.success(`正在打开: ${nodeData.label}`)
    } catch (error) {
      console.debug('知识点跳转错误:', error)
    }
    return
  }

  // 章节点击：切换到章节视图
  if (type === 'chapter') {
    try {
      emit('nodeClick', {
        type: 'chapter',
        chapterId: nodeData.chapterId,
        chapterTitle: nodeData.fullLabel,
      })
    } catch (error) {
      console.debug('章节切换错误:', error)
    }
    return
  }
}

function returnToCourseView() {
  emit('nodeClick', { type: 'course-view' })
}

watch(() => props.chapterId, () => nextTick(() => renderGraph()))
onMounted(() => nextTick(() => renderGraph()))
onBeforeUnmount(() => {
  // 清理防抖定时器
  if (clickDebounceTimer) {
    clearTimeout(clickDebounceTimer)
    clickDebounceTimer = null
  }
  
  // 销毁图谱实例
  if (graph) {
    try { graph.destroy() } catch { /* 已销毁或无效，忽略 */ }
    graph = null
  }
})

defineExpose({ renderGraph })
</script>

<style scoped>
.knowledge-graph-container {
  background: #fff;
  border-radius: 4px;
  padding: 20px;
  min-height: 880px;
  position: relative;
  overflow: hidden;
  border: 1px solid #e6e6e6;
}

.graph-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e6e6e6;
  gap: 12px;
}

.back-btn {
  border-radius: 4px;
  background: #1890ff;
  color: white;
  border: none;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: #40a9ff;
  transform: translateY(-1px);
}

.graph-header-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.g6-container {
  width: 100%;
  height: calc(880px - 80px);
  min-height: 800px;
  background: #f8f9fa;
  border-radius: 8px;
  position: relative;
}

:deep(.g6-node) {
  animation: float 3s ease-in-out infinite;
}

:deep(.g6-node:nth-child(3n)) {
  animation-delay: 0.5s;
}

:deep(.g6-node:nth-child(3n+1)) {
  animation-delay: 1s;
}

:deep(.g6-node:nth-child(3n+2)) {
  animation-delay: 1.5s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-6px);
  }
}
</style>
