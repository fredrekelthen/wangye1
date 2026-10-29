<template>
  <div class="knowledge-graph-container">
    <div class="graph-toolbar">
      <div class="toolbar-left">
        <a-button v-if="viewMode === 'chapter'" size="small" @click="returnToCourseView" class="back-btn">
          &larr; 返回课程总览
        </a-button>
        <span class="toolbar-title">{{ viewMode === 'chapter' ? chapterTitle : '电磁场与电磁波 · 知识图谱' }}</span>
      </div>
      <div class="toolbar-right">
        <div class="legend">
          <span class="legend-item"><i class="legend-node legend-root"></i>核心</span>
          <span class="legend-item"><i class="legend-node legend-chapter"></i>章/节</span>
          <span class="legend-item"><i class="legend-node legend-sub"></i>知识点</span>
          <span class="legend-item"><i class="legend-node legend-group"></i>同节知识点</span>
          <span class="legend-item"><i class="legend-node legend-prereq"></i>前置知识</span>
          <span class="legend-item"><i class="legend-node legend-app"></i>应用</span>
          <span class="legend-item"><i class="legend-node legend-exp"></i>实验</span>
        </div>
        <a-button type="primary" @click="openCodeGen">
          <span class="codegen-icon">⚡</span> 生成仿真代码
        </a-button>
      </div>
    </div>

    <!-- 关系类型图例：连线按前置依赖 / 递进 / 对比 / 扩展 / 交叉引用分色 -->
    <div class="rel-legend">
      <span class="rel-item" v-for="item in relationLegend" :key="item.type">
        <i class="rel-line" :style="{ borderTopColor: item.stroke, borderTopStyle: item.dashed ? 'dashed' : 'solid' }"></i>
        {{ item.text }}
      </span>
      <span class="rel-hint">悬浮知识点可查看它在知识体系中的位置与前后衔接</span>
    </div>

    <!-- G6 渲染容器 -->
    <div class="g6-container" ref="graphRef"></div>

    <!-- 知识点关联卡片：位置 / 前置依赖 / 递进 / 对比扩展 / 跨章交叉引用 -->
    <div
      v-if="hoverContext"
      class="kg-card"
      :style="cardStyle"
      @mouseenter="cancelHideCard"
      @mouseleave="scheduleHideCard"
    >
      <div class="kg-card-title">{{ hoverContext.label }}</div>
      <div class="kg-card-pos">{{ hoverContext.position }}</div>
      <div v-if="hoverContext.description" class="kg-card-note">{{ hoverContext.description }}</div>
      <div v-if="hoverContext.foundation.length" class="kg-card-foundation">
        先修基础：{{ hoverContext.foundation.join('、') }}
      </div>
      <div v-for="section in cardSections" :key="section.title" class="kg-card-section">
        <div class="kg-card-group">{{ section.title }}</div>
        <a
          v-for="ref in section.refs"
          :key="ref.id"
          class="kg-card-link"
          @click="focusRef(ref)"
        >
          <span class="kg-card-tag" :class="'kg-tag-' + ref.type">{{ RELATION_TEXT[ref.type] }}</span>
          <span class="kg-card-label">{{ ref.label }}</span>
          <span v-if="!ref.inView && ref.chapterId" class="kg-card-ch">第 {{ ref.chapterId }} 章</span>
        </a>
        <div v-if="section.more" class="kg-card-more">其余 {{ section.more }} 条同类关联见对应章节图谱</div>
        <div v-for="ref in section.refs.filter((r) => r.note)" :key="ref.id + '-note'" class="kg-card-tip">
          {{ ref.label }}：{{ ref.note }}
        </div>
      </div>
    </div>

    <!-- 前置知识说明对话框 -->
    <a-modal
      v-model:open="prereqVisible"
      :title="prereqNode?.groupLabel ? `前置知识 · ${prereqNode.groupLabel}` : '前置知识'"
      :width="480"
      :footer="null"
    >
      <div v-if="prereqNode" class="prereq-body">
        <div class="prereq-title">{{ prereqNode.label }}</div>
        <div class="prereq-desc">{{ prereqNode.description || '暂无说明' }}</div>
        <div class="prereq-hint">该知识点属于课程先修内容，建议在学习相关章节前复习。</div>
      </div>
    </a-modal>

    <!-- 电磁场仿真代码生成对话框 -->
    <a-modal
      v-model:open="codeGenVisible"
      title="电磁场仿真代码生成"
      :width="780"
      :footer="null"
      :maskClosable="false"
    >
      <div class="codegen-body">
        <a-alert
          v-if="lastNodeLabel"
          type="info"
          show-icon
          class="codegen-tip"
          message="已结合所选知识点预填需求，可在下方补充说明后生成。"
          :description="`关联知识点：${lastNodeLabel}`"
        />
        <a-textarea
          v-model:value="requirement"
          :rows="4"
          placeholder="请用自然语言描述你想要的电磁场仿真，例如：生成半波偶极子天线在 300MHz 的远场辐射方向图 Python 仿真代码"
        />
        <div class="codegen-actions">
          <a-button type="primary" :loading="generating" @click="generateCode">生成 Python 代码</a-button>
          <span class="codegen-hint">仅支持电磁场仿真（天线辐射 / 电磁波传播）相关需求</span>
        </div>

        <a-alert
          v-if="result && !result.generated"
          type="warning"
          show-icon
          class="codegen-result-block"
          :message="result.message || '当前需求未识别为电磁场仿真任务，已拒绝生成。'"
        />

        <div v-if="result && result.generated" class="codegen-result-block">
          <div class="result-meta">
            <a-tag v-for="dep in (result.dependencies || [])" :key="dep" color="blue">{{ dep }}</a-tag>
            <a-button size="small" class="copy-btn" @click="copyCode">复制代码</a-button>
          </div>
          <div v-if="result.executionHint" class="result-hint">{{ result.executionHint }}</div>
          <pre class="code-block"><code>{{ result.code }}</code></pre>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { courseKnowledgeStructure } from '@/data/knowledgeGraphData'
import { buildConceptGraph, nodeMetrics, NODE_SIZE, RELATION_TEXT } from '@/data/conceptGraphBuilder'
import type { ConceptNodeData, NodeKind, EdgeKind, NodeContext, NodeRef } from '@/data/conceptGraphBuilder'
import type { RelationKind } from '@/data/knowledgeRelations'
import { emCodeApi } from '@/api/emCode'
import type { EmCodeResult } from '@/api/emCode'

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

const router = useRouter()
const graphRef = ref<HTMLDivElement | null>(null)
const viewMode = ref<'course' | 'chapter'>('course')
const chapterTitle = ref('')
let graph: any = null

// ===== 前置知识说明 =====
const prereqVisible = ref(false)
const prereqNode = ref<ConceptNodeData | null>(null)

// ===== 电磁场仿真代码生成 =====
const codeGenVisible = ref(false)
const generating = ref(false)
const requirement = ref('')
const lastNodeLabel = ref('')
const result = ref<EmCodeResult | null>(null)

function openCodeGen() {
  result.value = null
  requirement.value = lastNodeLabel.value ? `${lastNodeLabel.value} 的 Python 仿真代码` : ''
  codeGenVisible.value = true
}

async function generateCode() {
  const req = requirement.value.trim()
  if (!req) {
    message.warning('请先描述仿真需求')
    return
  }
  generating.value = true
  result.value = null
  try {
    const resp = await emCodeApi.generate(req)
    result.value = resp.data
    if (!resp.data?.generated) {
      message.warning(resp.data?.message || '未生成代码')
    } else {
      message.success('代码已生成')
    }
  } catch (e) {
    // 错误已由拦截器统一提示
  } finally {
    generating.value = false
  }
}

async function copyCode() {
  const code = result.value?.code || ''
  try {
    await navigator.clipboard.writeText(code)
    message.success('代码已复制到剪贴板')
  } catch {
    message.error('复制失败，请手动选择复制')
  }
}

// ===== 知识点关联卡片（悬浮节点时显示它在知识体系中的位置与前后衔接） =====
const hoverContext = ref<(NodeContext & { label: string; description?: string }) | null>(null)
const cardStyle = ref<Record<string, string>>({})
let hideCardTimer: any = null

// 切换章节后需要自动定位 / 高亮的目标节点
let pendingFocusId: string | null = null

const cardSections = computed(() => {
  const ctx = hoverContext.value
  if (!ctx) return []
  const cap = (refs: NodeRef[]) => ({ refs: refs.slice(0, 6), more: Math.max(0, refs.length - 6) })
  return [
    { title: '在知识体系中的位置', ...cap(ctx.outline) },
    { title: '前置依赖（先掌握）', ...cap(ctx.prereq) },
    { title: '递进关系（接着学）', ...cap(ctx.next) },
    { title: '对比 / 扩展 / 交叉引用', ...cap(ctx.cross) },
    { title: '应用与实验', ...cap(ctx.apply) },
  ].filter((s) => s.refs.length > 0)
})

function cancelHideCard() {
  if (hideCardTimer) {
    clearTimeout(hideCardTimer)
    hideCardTimer = null
  }
}

function scheduleHideCard() {
  cancelHideCard()
  hideCardTimer = setTimeout(() => {
    hoverContext.value = null
  }, 240)
}

/** 悬浮节点时在鼠标附近弹出关联卡片（位置 / 前后衔接 / 跨章引用） */
function showContextCard(nodeId: string, evt: any) {
  cancelHideCard()
  const node = graph?.getNodeData(nodeId)
  const ctx = node?.data?.context as NodeContext | undefined
  if (!ctx) {
    hoverContext.value = null
    return
  }
  hoverContext.value = { ...ctx, label: node.data.label, description: node.data.description }
  const host = graphRef.value?.parentElement?.getBoundingClientRect()
  const rect = graphRef.value?.getBoundingClientRect()
  const nx = evt?.client?.x ?? evt?.clientX ?? 0
  const ny = evt?.client?.y ?? evt?.clientY ?? 0
  const hostW = host?.width ?? 900
  const hostH = host?.height ?? 800
  const cardW = 340
  const cardH = 320
  const baseX = (rect?.left ?? 0) - (host?.left ?? 0)
  const baseY = (rect?.top ?? 0) - (host?.top ?? 0)
  const left = Math.min(Math.max(baseX + nx + 18, 8), Math.max(hostW - cardW - 8, 8))
  const top = Math.min(Math.max(baseY + ny + 12, 8), Math.max(hostH - cardH - 8, 8))
  cardStyle.value = { left: `${Math.round(left)}px`, top: `${Math.round(top)}px` }
}

/** 点击卡片里的关联：视图内直接定位高亮，跨章则切到对应章节后再定位 */
function focusRef(ref: NodeRef) {
  if (!graph) return
  if (ref.inView) {
    try {
      graph.focusElement(ref.id).catch(() => { /* 元素已重绘，忽略 */ })
      graph.setElementState(ref.id, 'highlight', true)
      setTimeout(() => {
        try {
          graph?.setElementState(ref.id, 'highlight', false)
        } catch { /* 已重绘，忽略 */ }
      }, 1800)
    } catch { /* 元素不存在，忽略 */ }
    scheduleHideCard()
    return
  }
  if (!ref.chapterId) return
  pendingFocusId = ref.id
  hoverContext.value = null
  emit('nodeClick', { type: 'chapter', chapterId: ref.chapterId })
}

// 防抖：避免快速连续点击导致的问题
let clickDebounceTimer: any = null
const debounceClick = (fn: () => void, delay = 200) => {
  if (clickDebounceTimer) {
    clearTimeout(clickDebounceTimer)
  }
  clickDebounceTimer = setTimeout(fn, delay)
}

// ===== 概念图配色（参照模板：红心 / 蓝章 / 橙知识点 / 绿前置 / 紫应用 / 黑实验）=====
// 尺寸与字号统一放在 conceptGraphBuilder（布局与渲染共用），这里只保留颜色与线宽
const NODE_STYLE: Record<NodeKind, { stroke: string; text: string; lineWidth: number }> = {
  root:        { stroke: '#e53935', text: '#e53935', lineWidth: 2.5 },
  chapter:     { stroke: '#1e40af', text: '#111827', lineWidth: 2 },
  section:     { stroke: '#1e40af', text: '#111827', lineWidth: 2 },
  subsection:  { stroke: '#f59e0b', text: '#111827', lineWidth: 1.5 },
  prereq:      { stroke: '#1e40af', text: '#111827', lineWidth: 1.5 },
  application: { stroke: '#c026d3', text: '#111827', lineWidth: 2 },
  experiment:  { stroke: '#111827', text: '#111827', lineWidth: 2.5 },
}

const EDGE_STYLE: Record<EdgeKind, { stroke: string; lineWidth: number; lineDash?: number[]; type: string; curveOffset: number }> = {
  main:        { stroke: '#e53935', lineWidth: 2,   type: 'line',      curveOffset: 0 },
  sub:         { stroke: '#2563eb', lineWidth: 1.5, type: 'line',      curveOffset: 0 },
  // 扩展节点在外圈、目标在内圈，弧线向外鼓出，走外侧空白带，避免压住中间节点
  prereq:      { stroke: '#16a34a', lineWidth: 1.5, lineDash: [8, 4],  type: 'quadratic', curveOffset: 44 },
  application: { stroke: '#c026d3', lineWidth: 2.5, type: 'quadratic', curveOffset: 66 },
  experiment:  { stroke: '#111827', lineWidth: 2.5, type: 'quadratic', curveOffset: 66 },
  // 章节间关联跨越整个画布，弧度放大以绕开中间的主体结构
  relation:    { stroke: '#2563eb', lineWidth: 3,   lineDash: [10, 5], type: 'quadratic', curveOffset: 96 },
}

// 知识依赖按类型分色：前置依赖 / 递进 / 对比 / 扩展 / 交叉引用 / 应用
const RELATION_STYLE: Record<RelationKind, { stroke: string; lineDash: number[]; lineWidth: number }> = {
  prereq:   { stroke: '#16a34a', lineDash: [10, 5],        lineWidth: 2.2 },
  progress: { stroke: '#2563eb', lineDash: [10, 5],        lineWidth: 3 },
  compare:  { stroke: '#f59e0b', lineDash: [2, 4],         lineWidth: 2.4 },
  extend:   { stroke: '#7c3aed', lineDash: [12, 4, 2, 4],  lineWidth: 2.4 },
  cross:    { stroke: '#0891b2', lineDash: [2, 5],         lineWidth: 2.2 },
  apply:    { stroke: '#c026d3', lineDash: [8, 4],         lineWidth: 2.6 },
}

const relationLegend = (Object.keys(RELATION_STYLE) as RelationKind[]).map((type) => ({
  type,
  text: RELATION_TEXT[type],
  stroke: RELATION_STYLE[type].stroke,
  dashed: RELATION_STYLE[type].lineDash.length > 1 || RELATION_STYLE[type].lineDash[0] <= 3,
}))

const COMBO_STROKE = '#16a34a'
const COMBO_LABEL = '#e53935'

// 当前高亮的 combo（点击 combo 高亮组内节点及其出边，再次点击取消）
let activeCombo: string | null = null

/**
 * 以"章中心"（root，画面中最大的节点）为圆心取景：
 * 取内容相对根节点的最大水平/垂直半径，算出一个对称缩放比，
 * 再把根节点平移到画布正中 —— 既保证最大节点在视觉中心，也不会让任何节点出界。
 */
async function fitOnRoot() {
  if (!graph || !graphRef.value) return
  const root = graph.getNodeData('root')
  if (!root || !root.style) {
    graph.fitView().catch(() => { /* 忽略 */ })
    return
  }
  const rect = graphRef.value.getBoundingClientRect()
  const W = rect.width
  const H = rect.height
  if (W < 80 || H < 80) return

  let maxX = 1
  let maxY = 1
  graph.getData().nodes.forEach((n: any) => {
    const nd = graph.getNodeData(n.id)
    if (!nd || !nd.style) return
    const b = graph.getElementRenderBounds(n.id)
    const hw = b && b.halfExtents ? b.halfExtents[0] : 60
    const hh = b && b.halfExtents ? b.halfExtents[1] : 24
    maxX = Math.max(maxX, Math.abs(nd.style.x - root.style.x) + hw)
    maxY = Math.max(maxY, Math.abs(nd.style.y - root.style.y) + hh)
  })

  const pad = 34
  const zoom = Math.max(0.2, Math.min((W / 2 - pad) / maxX, (H / 2 - pad) / maxY, 1.3))
  await graph.zoomTo(zoom, false)
  // 迭代两次把根节点精确对齐到画布中心（平移量与缩放解耦，避免一次偏差）
  for (let i = 0; i < 2; i++) {
    const p = graph.getViewportByCanvas([root.style.x, root.style.y])
    const dx = W / 2 - p[0]
    const dy = H / 2 - p[1]
    if (Math.abs(dx) < 0.5 && Math.abs(dy) < 0.5) break
    await graph.translateBy([dx, dy], false)
  }
}

// 创建 G6 实例并渲染
async function renderGraph() {
  if (!graphRef.value) return

  // 按需异步加载 G6，首屏不阻塞
  const { Graph } = await import('@antv/g6')

  // 销毁旧图谱实例
  if (graph) {
    try {
      graph.off()
      graph.destroy()
    } catch { /* 已销毁或无效，忽略 */ }
    graph = null
  }
  activeCombo = null

  const isCourseMode = props.chapterId === undefined || props.chapterId < 0
  viewMode.value = isCourseMode ? 'course' : 'chapter'

  const graphData = buildConceptGraph(isCourseMode ? undefined : props.chapterId)
  if (!graphData) return
  if (!isCourseMode) {
    chapterTitle.value =
      courseKnowledgeStructure.chapters.find((c) => c.id === `chapter-${props.chapterId}`)?.title || ''
  }

  // 不配置 layout：坐标已由 buildConceptGraph 内的确定性布局写入节点 style，
  // G6 只负责渲染，拖拽后也不会被重新布局覆盖。
  graph = new Graph({
    container: graphRef.value,
    // 不用 autoFit：内容左右不对称时它按包围盒居中，会让"章中心"偏离画面正中，
    // 改为渲染后调用 fitOnRoot()，以根节点为圆心取景。
    padding: [40, 60],
    behaviors: [
      { type: 'drag-element' },
      { type: 'zoom-canvas', sensitivity: 1, minZoom: 0.2, maxZoom: 3, enableOptimize: true },
      { type: 'drag-canvas' },
    ],
    node: {
      type: 'ellipse',
      style: (d: any) => {
        const kind: NodeKind = d.data?.kind || 'subsection'
        const label: string = d.data?.label || ''
        const s = NODE_STYLE[kind] || NODE_STYLE.subsection
        const spec = NODE_SIZE[kind] || NODE_SIZE.subsection
        const m = nodeMetrics(kind, label)
        return {
          size: m.size,
          fill: '#ffffff',
          stroke: s.stroke,
          lineWidth: s.lineWidth,
          cursor: 'pointer',
          labelText: label,
          labelFill: s.text,
          labelFontSize: spec.fontSize,
          labelFontWeight: spec.fontWeight,
          labelFontFamily: '"PingFang SC", "Microsoft YaHei", "Noto Sans SC", sans-serif',
          labelPlacement: 'center',
          labelTextAlign: 'center',
          labelTextBaseline: 'middle',
          labelWordWrap: true,
          labelMaxWidth: m.size[0] - 14,
          // 行数按实际文本算出，标签不会被省略号截断
          labelMaxLines: m.lines,
          labelLineHeight: m.lineHeight,
        }
      },
      state: {
        hover: { lineWidth: 3.5, labelFontWeight: 700, shadowColor: 'rgba(0,0,0,0.15)', shadowBlur: 8 },
        highlight: { fill: '#fef9c3', lineWidth: 3 },
        inactive: { opacity: 0.25 },
      },
      animation: {
        appear: { animation: 'fade-in', duration: 400, easing: 'easeOutCubic' },
      },
    },
    edge: {
      type: (d: any) => EDGE_STYLE[(d.data?.kind || 'sub') as EdgeKind].type,
      style: (d: any) => {
        const kind = (d.data?.kind || 'sub') as EdgeKind
        const base = EDGE_STYLE[kind]
        // 关联边按关系类型分色分线型，并把关系名称标在线上
        const isRelation = kind === 'relation'
        const rel = isRelation ? RELATION_STYLE[(d.data?.relationType || 'progress') as RelationKind] : null
        const s = rel ? { ...base, ...rel } : base
        // 关联边跨的距离在总览里长、在单章视图里短，弧高分别设置，避免单章里绕成小圈
        const curve = isRelation && viewMode.value === 'chapter' ? 30 : base.curveOffset
        return {
          endArrow: true,
          endArrowSize: s.lineWidth >= 2.5 ? 10 : 8,
          stroke: s.stroke,
          lineWidth: s.lineWidth,
          lineDash: s.lineDash,
          curveOffset: curve,
          labelText: isRelation ? d.data?.label || '' : '',
          labelFill: s.stroke,
          labelFontSize: 10,
          labelFontWeight: 600,
          labelPlacement: 'middle',
          labelBackground: true,
          labelBackgroundFill: 'rgba(255,255,255,0.86)',
          labelBackgroundRadius: 4,
          labelPadding: [1, 3, 1, 3],
        }
      },
      state: {
        highlight: { lineWidth: 4, shadowColor: 'rgba(22,163,74,0.4)', shadowBlur: 6 },
        inactive: { opacity: 0.15 },
      },
    },
    combo: {
      // 用矩形（圆角）而不是 circle：组内成员横向排成一行时，circle 会按行宽撑成巨大的圆形
      type: 'rect',
      style: (d: any) => {
        // 节分组：极浅的虚线框，只用于提示"这些知识点属于同一节"，不显示标题（节节点本身已有标题）
        const isSectionGroup = d.data?.kind === 'section-group'
        return {
          fill: isSectionGroup ? 'rgba(37,99,235,0.03)' : 'rgba(22,163,74,0.05)',
          stroke: isSectionGroup ? '#cbd5e1' : COMBO_STROKE,
          lineWidth: isSectionGroup ? 1 : 2,
          lineDash: isSectionGroup ? [3, 4] : [8, 4],
          radius: isSectionGroup ? 18 : 26,
          padding: isSectionGroup ? 8 : 18,
          cursor: 'pointer',
          labelText: isSectionGroup ? '' : d.data?.label || '',
          labelFill: COMBO_LABEL,
          labelFontSize: 14,
          labelFontWeight: 700,
          labelPlacement: 'bottom',
          labelOffsetY: 4,
          labelBackground: false,
        }
      },
      state: {
        highlight: { lineWidth: 3, lineDash: undefined, stroke: '#15803d' },
      },
    },
  })

  // 节点点击（防抖，避免与拖拽误触）
  graph.on('node:click', (evt: any) => {
    debounceClick(() => {
      try {
        const nodeId = evt.target?.id
        if (!nodeId || !graph) return
        const nodeData = graph.getNodeData(nodeId)
        if (nodeData?.data) handleNodeClick(nodeData.data as ConceptNodeData)
      } catch (error) {
        // 图谱重新渲染期间可能出现节点不存在的情况，忽略
        console.debug('Node click error:', error)
      }
    })
  })

  // combo 点击：高亮组内节点及其出边
  graph.on('combo:click', (evt: any) => {
    debounceClick(() => {
      try {
        const comboId = evt.target?.id
        if (comboId && graph) toggleComboHighlight(comboId)
      } catch (error) {
        console.debug('Combo click error:', error)
      }
    })
  })

  // 悬停高亮 + 知识点关联卡片
  graph.on('node:pointerenter', (evt: any) => {
    try {
      const nodeId = evt.target?.id
      if (!nodeId || !graph) return
      graph.setElementState(nodeId, 'hover', true)
      showContextCard(nodeId, evt)
    } catch { /* 图谱重新渲染期间忽略 */ }
  })
  graph.on('node:pointerleave', (evt: any) => {
    try {
      const nodeId = evt.target?.id
      if (nodeId && graph) graph.setElementState(nodeId, 'hover', false)
      scheduleHideCard()
    } catch { /* 图谱重新渲染期间忽略 */ }
  })

  graph.setData(graphData)
  await graph.render()
  await fitOnRoot()

  // 跨章跳转过来的：定位并短暂高亮目标知识点，让读者一眼看到衔接点
  if (pendingFocusId) {
    const focusId = pendingFocusId
    pendingFocusId = null
    try {
      if (graph.getNodeData(focusId)) {
        graph.focusElement(focusId).catch(() => { /* 忽略 */ })
        graph.setElementState(focusId, 'highlight', true)
        setTimeout(() => {
          try {
            graph?.setElementState(focusId, 'highlight', false)
          } catch { /* 已重绘，忽略 */ }
        }, 2400)
      }
    } catch { /* 目标不在当前视图，忽略 */ }
  }
}

/** 点击 combo：高亮组内节点、出边及其指向的节点，其余元素淡化；再次点击恢复 */
function toggleComboHighlight(comboId: string) {
  if (!graph) return
  const { nodes = [], edges = [], combos = [] } = graph.getData()

  const reset = () => {
    const states: Record<string, string[]> = {}
    nodes.forEach((n: any) => { states[n.id] = [] })
    edges.forEach((e: any) => { states[e.id] = [] })
    combos.forEach((c: any) => { states[c.id] = [] })
    graph.setElementState(states)
  }

  if (activeCombo === comboId) {
    reset()
    activeCombo = null
    return
  }

  const memberIds = new Set<string>(nodes.filter((n: any) => n.combo === comboId).map((n: any) => n.id))
  const outEdges = edges.filter((e: any) => memberIds.has(e.source))
  const targetIds = new Set<string>(outEdges.map((e: any) => e.target))
  const outEdgeIds = new Set<string>(outEdges.map((e: any) => e.id))

  const states: Record<string, string[]> = {}
  nodes.forEach((n: any) => {
    states[n.id] = memberIds.has(n.id) || targetIds.has(n.id) ? ['highlight'] : ['inactive']
  })
  edges.forEach((e: any) => {
    states[e.id] = outEdgeIds.has(e.id) ? ['highlight'] : ['inactive']
  })
  combos.forEach((c: any) => {
    states[c.id] = c.id === comboId ? ['highlight'] : []
  })
  graph.setElementState(states)
  activeCombo = comboId
}

// 节点点击分发：按 kind 跳转到对应页面
function handleNodeClick(nodeData: ConceptNodeData) {
  if (!nodeData) return

  switch (nodeData.kind) {
    // 章节 → 切换到单章图谱
    case 'chapter':
      lastNodeLabel.value = nodeData.fullLabel || nodeData.label
      emit('nodeClick', {
        type: 'chapter',
        chapterId: nodeData.chapterId,
        chapterTitle: nodeData.fullLabel || nodeData.chapterTitle,
      })
      return

    // 节 / 小节 → 打开对应课程资料（与 CourseTree 点击一致）
    case 'section':
    case 'subsection':
      if (!nodeData.chapterTitle) return
      lastNodeLabel.value = nodeData.label
      window.dispatchEvent(
        new CustomEvent('course-selected', {
          detail: { chapter: nodeData.chapterTitle, lesson: nodeData.lesson || nodeData.label },
        })
      )
      message.success(`正在打开: ${nodeData.label}`)
      return

    // 前置知识 → 有路由则跳转，否则弹窗说明
    case 'prereq':
      if (nodeData.route) {
        router.push(nodeData.route)
      } else {
        prereqNode.value = nodeData
        prereqVisible.value = true
      }
      return

    // 应用 / 实验 → 电磁仿真页
    case 'application':
    case 'experiment':
      lastNodeLabel.value = nodeData.label
      if (nodeData.route) {
        router.push(nodeData.route)
      } else {
        router.push({
          path: '/course',
          query: {
            tab: 'emulation',
            topic: nodeData.topic || nodeData.label,
            ...(nodeData.emulationType ? { type: nodeData.emulationType } : {}),
          },
        })
      }
      message.info(`前往仿真: ${nodeData.label}`)
      return

    // 单章视图中心节点 → 返回课程总览
    case 'root':
      if (viewMode.value === 'chapter') returnToCourseView()
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
  background: #f4f6f9;
  border-radius: 0;
  padding: 24px;
  min-height: 980px;
  position: relative;
  overflow: hidden;
  border: 1px solid #dde1e7;
}

.graph-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #dde1e7;
  gap: 12px;
}

.back-btn {
  border-radius: 6px;
  background: #334155;
  color: #f1f5f9;
  border: none;
  font-size: 13px;
  transition: background 0.2s ease;
}

.back-btn:hover {
  background: #1e293b;
}

.graph-header-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

/* ===== 顶部工具栏 ===== */
.graph-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #dde1e7;
  gap: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* ===== 图例 ===== */
.legend {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #475569;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.legend-node {
  display: inline-block;
  width: 18px;
  height: 11px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid #94a3b8;
}

.legend-root   { border-color: #e53935; }
.legend-chapter{ border-color: #1e40af; }
.legend-sub    { border-color: #f59e0b; }
.legend-group  { border-color: #cbd5e1; border-style: dashed; background: #f1f5f9; }
.legend-prereq { border-color: #16a34a; border-style: dashed; }
.legend-app    { border-color: #c026d3; }
.legend-exp    { border-color: #111827; }

/* ===== 前置知识对话框 ===== */
.prereq-body {
  font-size: 13px;
}

.prereq-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
}

.prereq-desc {
  color: #334155;
  line-height: 1.7;
  margin-bottom: 12px;
}

.prereq-hint {
  font-size: 12px;
  color: #16a34a;
  background: #f0fdf4;
  border: 1px dashed #86efac;
  border-radius: 6px;
  padding: 8px 10px;
}

.codegen-icon {
  margin-right: 2px;
}

/* ===== 代码生成对话框 ===== */
.codegen-body {
  font-size: 13px;
}

.codegen-tip {
  margin-bottom: 12px;
}

.codegen-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.codegen-hint {
  font-size: 12px;
  color: #94a3b8;
}

.codegen-result-block {
  margin-top: 16px;
}

.result-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.copy-btn {
  margin-left: auto;
}

.result-hint {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
  font-family: ui-monospace, Menlo, Consolas, monospace;
}

.code-block {
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 8px;
  padding: 14px 16px;
  font-family: ui-monospace, "SFMono-Regular", Menlo, Consolas, "Liberation Mono", monospace;
  font-size: 12.5px;
  line-height: 1.6;
  white-space: pre;
  overflow: auto;
  max-height: 420px;
  margin: 0;
}

/* ===== 关系类型图例 ===== */
.rel-legend {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  margin: 0 0 10px 2px;
  font-size: 12px;
  color: #475569;
}

.rel-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.rel-line {
  display: inline-block;
  width: 22px;
  border-top-width: 3px;
  border-top-style: dashed;
}

.rel-hint {
  margin-left: auto;
  font-size: 11px;
  color: #94a3b8;
}

/* ===== 知识点关联卡片 ===== */
.kg-card {
  position: absolute;
  z-index: 20;
  width: 332px;
  max-height: 420px;
  overflow: auto;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.16);
  font-size: 12px;
  color: #334155;
}

.kg-card-title {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 4px;
}

.kg-card-pos {
  color: #64748b;
  margin-bottom: 6px;
}

.kg-card-note {
  color: #475569;
  line-height: 1.6;
  margin-bottom: 6px;
}

.kg-card-foundation {
  background: #f0fdf4;
  border: 1px dashed #86efac;
  border-radius: 6px;
  padding: 4px 8px;
  color: #15803d;
  margin-bottom: 8px;
}

.kg-card-section + .kg-card-section {
  margin-top: 8px;
  border-top: 1px dashed #e2e8f0;
  padding-top: 8px;
}

.kg-card-group {
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 4px;
}

.kg-card-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 0;
  color: #1d4ed8;
  cursor: pointer;
}

.kg-card-link:hover .kg-card-label {
  text-decoration: underline;
}

.kg-card-tag {
  flex: none;
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 10px;
  color: #fff;
}

.kg-tag-prereq { background: #16a34a; }
.kg-tag-progress { background: #2563eb; }
.kg-tag-compare { background: #f59e0b; }
.kg-tag-extend { background: #7c3aed; }
.kg-tag-cross { background: #0891b2; }
.kg-tag-apply { background: #c026d3; }

.kg-card-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kg-card-ch {
  flex: none;
  color: #94a3b8;
  font-size: 11px;
}

.kg-card-more,
.kg-card-tip {
  color: #94a3b8;
  font-size: 11px;
  line-height: 1.5;
  margin-top: 2px;
}

.g6-container {
  width: 100%;
  height: calc(980px - 80px);
  min-height: 900px;
  background: #f8fafc;
  border-radius: 8px;
  position: relative;
  border: 1px solid #e2e8f0;
}
</style>
