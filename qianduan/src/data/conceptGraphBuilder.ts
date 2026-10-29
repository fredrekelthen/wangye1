/**
 * 概念图数据构建器
 * 将 courseKnowledgeStructure（树形）与 knowledgeRelations（前置 / 应用 / 关联）
 * 合并为 G6 v5 所需的 { nodes, edges, combos } 数据
 */
import { courseKnowledgeStructure } from './knowledgeGraphData'
import { getChapterCoreStructure } from './knowledgeCoreStructure'
import { prereqGroups, applications, relations, chapterRelations, knowledgeLinks } from './knowledgeRelations'
import type { RelationKind } from './knowledgeRelations'

export type NodeKind = 'root' | 'chapter' | 'section' | 'subsection' | 'prereq' | 'application' | 'experiment'
export type EdgeKind = 'main' | 'sub' | 'prereq' | 'application' | 'experiment' | 'relation'

/** 悬浮卡片里的一条关联索引 */
export interface NodeRef {
  id: string
  label: string
  /** 关系类型（决定标签颜色与文字，如「前置依赖」「对比」） */
  type: RelationKind
  /** 逻辑衔接说明 */
  note?: string
  /** 目标所在章节，跨章时需要 */
  chapterId?: number
  /** 是否在当前视图内（视图内的可直接定位，视图外需切章） */
  inView: boolean
}

/** 知识点在整体知识体系中的位置与前后衔接 */
export interface NodeContext {
  /** 位置说明，如「第 2 章 · 2.6 静电场中的导体和介质」 */
  position: string
  /** 所在节 / 章节在课程中的位置（本章前置、后续章节、所在节的前后节） */
  outline: NodeRef[]
  /** 前置依赖（必须先掌握） */
  prereq: NodeRef[]
  /** 递进 / 后续（学完本点之后接着学） */
  next: NodeRef[]
  /** 对比 / 扩展 / 交叉引用 */
  cross: NodeRef[]
  /** 应用 / 实验 */
  apply: NodeRef[]
  /** 先修知识基础（章级前置知识组） */
  foundation: string[]
}

export interface ConceptNodeData {
  kind: NodeKind
  label: string
  fullLabel?: string
  description?: string
  /** 该知识点在知识网络中的位置与关联索引 */
  context?: NodeContext
  // 跳转所需字段
  chapterId?: number
  chapterTitle?: string
  sectionTitle?: string
  /** course-selected 事件的 detail.lesson */
  lesson?: string
  /** 知识点所属主题 id（单章提炼结构中，主题由若干教材小节合并而来） */
  themeId?: string
  route?: string
  topic?: string
  emulationType?: string
  /** 前置知识所属组名 */
  groupLabel?: string
}

export interface ConceptNode {
  id: string
  /** 所属 combo（G6 v5 字段名为 combo） */
  combo?: string
  /** 预设坐标：不再依赖 G6 布局算法，由 layoutConceptGraph 直接计算 */
  style?: { x: number; y: number }
  data: ConceptNodeData
}

export interface ConceptEdge {
  id: string
  source: string
  target: string
  data: {
    kind: EdgeKind
    label?: string
    /** relation 边的关系类型，用于分色、分线型与文字标注 */
    relationType?: RelationKind
  }
}

export interface ConceptCombo {
  id: string
  data: { label: string; kind: 'prereq-group' | 'section-group' }
}

export interface ConceptGraphData {
  nodes: ConceptNode[]
  edges: ConceptEdge[]
  combos: ConceptCombo[]
}

/** 节点基础尺寸与字号（渲染和布局共用，避免两处定义漂移） */
export const NODE_SIZE: Record<
  NodeKind,
  { width: number; height: number; fontSize: number; fontWeight: number; maxLines: number }
> = {
  root:        { width: 196, height: 72, fontSize: 17, fontWeight: 700, maxLines: 1 },
  chapter:     { width: 152, height: 54, fontSize: 13, fontWeight: 600, maxLines: 2 },
  section:     { width: 152, height: 54, fontSize: 13, fontWeight: 600, maxLines: 2 },
  subsection:  { width: 118, height: 44, fontSize: 11, fontWeight: 500, maxLines: 2 },
  prereq:      { width: 94, height: 42, fontSize: 11, fontWeight: 500, maxLines: 2 },
  application: { width: 130, height: 50, fontSize: 12, fontWeight: 600, maxLines: 2 },
  experiment:  { width: 116, height: 50, fontSize: 12, fontWeight: 700, maxLines: 2 },
}

/** 标签内边距与行高（渲染与布局都按这个算，保证文字既不溢出也不被截断） */
const LABEL_PAD_X = 18
const LINE_HEIGHT_RATIO = 1.34

export interface NodeMetrics {
  size: [number, number]
  /** 实际占用的行数，渲染时用它设置 labelMaxLines，杜绝省略号截断 */
  lines: number
  lineHeight: number
}

/**
 * 按标签长度计算节点尺寸：先按基础宽度折行，超过 maxLines 就加宽，
 * 再按行数计算高度，保证文字完整显示且不与相邻节点重叠。
 */
export function nodeMetrics(kind: NodeKind, label: string): NodeMetrics {
  const base = NODE_SIZE[kind] || NODE_SIZE.subsection
  const lineHeight = Math.round(base.fontSize * LINE_HEIGHT_RATIO)
  const textWidth = label.length * base.fontSize * 1.06
  let width = base.width
  let lines = Math.max(1, Math.ceil(textWidth / (width - LABEL_PAD_X)))
  if (lines > base.maxLines) {
    width = Math.ceil(textWidth / base.maxLines) + LABEL_PAD_X
    lines = Math.max(1, Math.ceil(textWidth / (width - LABEL_PAD_X)))
  }
  const height = Math.max(base.height, lines * lineHeight + 14)
  return { size: [width, height], lines, lineHeight }
}

/** 布局用的节点尺寸（与渲染完全一致） */
export function nodeSize(kind: NodeKind, label: string): [number, number] {
  return nodeMetrics(kind, label).size
}

const stripChapterPrefix = (title: string) => title.replace(/^第\d+章\s*/, '')
const stripNumberPrefix = (title: string) => title.replace(/^\d+(\.\d+)*\s*/, '')

/**
 * 课程总览：root → 章。
 * 总览只保留章 + 扩展元素（前置组 / 应用 / 实验），知识点放到单章视图展示：
 * 9 章 × 4 核心知识点共 60+ 节点挤在一个画布内会互相重叠、标签不可读（详见 docs/knowledge-graph-plan.md B 项）。
 */
function buildCourseTree(): ConceptGraphData {
  const nodes: ConceptNode[] = []
  const edges: ConceptEdge[] = []

  nodes.push({
    id: 'root',
    data: { kind: 'root', label: '电磁场与电磁波', description: '课程总览' },
  })

  courseKnowledgeStructure.chapters.forEach((chapter) => {
    const chapterNum = parseInt(chapter.id.replace('chapter-', ''), 10)
    nodes.push({
      id: chapter.id,
      data: {
        kind: 'chapter',
        label: stripChapterPrefix(chapter.title),
        fullLabel: chapter.title,
        description: chapter.description,
        chapterId: chapterNum,
        chapterTitle: chapter.title,
      },
    })
    edges.push({ id: `e-root-${chapter.id}`, source: 'root', target: chapter.id, data: { kind: 'main' } })
  })

  return { nodes, edges, combos: [] }
}

/**
 * 单章视图：root(章) → 主题 → 核心知识点。
 *
 * 节点不再照搬教材小节：若 `knowledgeCoreStructure` 中有该章的提炼结构，
 * 则按"主题（若干小节合并）→ 核心知识点（提炼命名）"建图；没有时回退到
 * "节 → 小节"的原始结构。提炼结构的每个知识点都保留了教材小节 id，
 * 因此 lesson（点击跳转）与悬浮卡片的出处说明不受影响。
 */
function buildChapterTree(chapterId: number): ConceptGraphData | null {
  const chapter = courseKnowledgeStructure.chapters.find((c) => c.id === `chapter-${chapterId}`)
  if (!chapter) return null

  const nodes: ConceptNode[] = []
  const edges: ConceptEdge[] = []
  const combos: ConceptCombo[] = []

  nodes.push({
    id: 'root',
    data: {
      kind: 'root',
      label: stripChapterPrefix(chapter.title),
      fullLabel: chapter.title,
      description: chapter.description,
      chapterId,
      chapterTitle: chapter.title,
    },
  })

  const core = getChapterCoreStructure(chapterId)
  if (core && core.length > 0) {
    // id → 教材小节标题 / 描述 / 所属节，用于回查 lesson 与补充说明
    const subIndex = new Map<string, { title: string; description?: string; sectionTitle: string }>()
    chapter.sections.forEach((section) => {
      subIndex.set(section.id, { title: section.title, description: section.description, sectionTitle: section.title })
      section.subsections.forEach((sub) => {
        subIndex.set(sub.id, { title: sub.title, description: sub.description, sectionTitle: section.title })
      })
    })

    core.forEach((theme) => {
      const firstInfo = theme.points.length > 0 ? subIndex.get(theme.points[0].id) : undefined
      nodes.push({
        id: theme.id,
        data: {
          kind: 'section',
          label: theme.label,
          fullLabel: theme.label,
          description: theme.from ? `涵盖：${theme.from}` : undefined,
          chapterId,
          chapterTitle: chapter.title,
          lesson: firstInfo?.sectionTitle || firstInfo?.title || chapter.title,
        },
      })
      edges.push({ id: `e-root-${theme.id}`, source: 'root', target: theme.id, data: { kind: 'main' } })

      if (theme.points.length > 0) {
        combos.push({ id: `section-${theme.id}`, data: { label: theme.label, kind: 'section-group' } })
      }

      theme.points.forEach((pt) => {
        const info = subIndex.get(pt.id)
        nodes.push({
          id: pt.id,
          combo: theme.points.length > 0 ? `section-${theme.id}` : undefined,
          data: {
            kind: 'subsection',
            label: pt.label,
            fullLabel: info?.title || pt.label,
            description: pt.note || info?.description,
            chapterId,
            chapterTitle: chapter.title,
            sectionTitle: theme.label,
            themeId: theme.id,
            lesson: info?.title || pt.label,
          },
        })
        edges.push({ id: `e-${theme.id}-${pt.id}`, source: theme.id, target: pt.id, data: { kind: 'sub' } })
      })
    })

    return { nodes, edges, combos }
  }

  chapter.sections.forEach((section) => {
    nodes.push({
      id: section.id,
      data: {
        kind: 'section',
        label: stripNumberPrefix(section.title),
        fullLabel: section.title,
        description: section.description,
        chapterId,
        chapterTitle: chapter.title,
        lesson: section.title,
      },
    })
    edges.push({ id: `e-root-${section.id}`, source: 'root', target: section.id, data: { kind: 'main' } })

    // 每个节的知识点归入一个"节分组" combo：视图里用极浅的虚线框把同一节的知识点圈在一起，
    // 让章节 / 节的划分一眼可辨（前置知识组用绿色框，两者样式区分）
    if (section.subsections.length > 0) {
      combos.push({ id: `section-${section.id}`, data: { label: section.title, kind: 'section-group' } })
    }

    section.subsections.forEach((sub) => {
      nodes.push({
        id: sub.id,
        combo: section.subsections.length > 0 ? `section-${section.id}` : undefined,
        data: {
          kind: 'subsection',
          label: stripNumberPrefix(sub.title),
          fullLabel: sub.title,
          description: sub.description,
          chapterId,
          chapterTitle: chapter.title,
          sectionTitle: section.title,
          lesson: sub.title,
        },
      })
      edges.push({ id: `e-${section.id}-${sub.id}`, source: section.id, target: sub.id, data: { kind: 'sub' } })
    })
  })

  return { nodes, edges, combos }
}

/**
 * 把关系配置中的目标 id 映射到当前视图中的实际节点 id。
 * 单章视图下，'chapter-N' 对应视图中的 'root'；其他 id 需在视图中存在才有效。
 */
function resolveTarget(id: string, present: Set<string>, chapterId?: number): string | null {
  if (chapterId !== undefined && id === `chapter-${chapterId}`) return 'root'
  return present.has(id) ? id : null
}

/**
 * 构建概念图数据
 * @param chapterId 未传或 <0 为课程总览，否则为单章视图
 */
export function buildConceptGraph(chapterId?: number): ConceptGraphData | null {
  const isCourse = chapterId === undefined || chapterId < 0
  const base = isCourse ? buildCourseTree() : buildChapterTree(chapterId as number)
  if (!base) return null

  const { nodes, edges, combos } = base
  const present = new Set(nodes.map((n) => n.id))
  const resolvedChapter = isCourse ? undefined : chapterId

  // ---- 前置知识组 ----
  prereqGroups.forEach((group) => {
    const targets = group.supports
      .map((id) => resolveTarget(id, present, resolvedChapter))
      .filter((t): t is string => !!t)
    if (targets.length === 0) return

    combos.push({ id: group.id, data: { label: group.label, kind: 'prereq-group' } })
    group.items.forEach((item) => {
      nodes.push({
        id: item.id,
        combo: group.id,
        data: {
          kind: 'prereq',
          label: item.label,
          description: item.description,
          route: item.route,
          groupLabel: group.label,
        },
      })
    })
    // 绿色虚线箭头从组内首个节点发出（G6 v5 布局中 combo 作为边端点不稳定）
    const anchor = group.items[0].id
    targets.forEach((t) => {
      edges.push({ id: `e-${group.id}-${t}`, source: anchor, target: t, data: { kind: 'prereq' } })
    })
  })

  // ---- 应用 / 实验 ----
  applications.forEach((app) => {
    const targets = app.targets
      .map((id) => resolveTarget(id, present, resolvedChapter))
      .filter((t): t is string => !!t)
    // 单章视图下 chapter 与其 subsection 都命中时会得到 'root' 与具体节点：
    // 这时只画指向具体知识点的连线（更短、更明确），不再拉一条横穿画布的线到章中心
    const uniq = Array.from(new Set(targets))
    if (uniq.length === 0) return
    const edgeTargets = uniq.length > 1 ? uniq.filter((t) => t !== 'root') : uniq

    nodes.push({
      id: app.id,
      data: {
        kind: app.kind,
        label: app.label,
        description: app.description,
        route: app.route,
        topic: app.topic || app.label,
        emulationType: app.emulationType,
      },
    })
    edgeTargets.forEach((t) => {
      edges.push({ id: `e-${app.id}-${t}`, source: app.id, target: t, data: { kind: app.kind } })
    })
  })

  // ---- 知识依赖关联：章节间（总览）+ 章内（单章视图），端点都在视图中才绘制 ----
  const relationList = isCourse ? relations : chapterRelations
  relationList.forEach((r) => {
    if (present.has(r.source) && present.has(r.target)) {
      edges.push({
        id: `e-rel-${r.source}-${r.target}`,
        source: r.source,
        target: r.target,
        data: { kind: 'relation', label: r.label, relationType: r.type || 'progress' },
      })
    }
  })

  // ---- 给每个节点补上"知识体系位置 + 前后衔接"的索引（悬浮卡片用） ----
  attachNodeContext({ nodes, edges, combos }, isCourse ? undefined : chapterId)

  // 单章视图节点更多、扇形更宽，用更大的纵向压缩比把布局摊成宽椭圆，提升自动缩放后的可读性
  return layoutConceptGraph({ nodes, edges, combos }, { aspect: isCourse ? 1.32 : 1.52 })
}

// ===== 知识体系索引 =====
// 关系类型的中文标注（边上与悬浮卡片都会用到）
export const RELATION_TEXT: Record<RelationKind, string> = {
  prereq: '前置依赖',
  progress: '递进关系',
  compare: '对比关系',
  extend: '扩展关系',
  cross: '交叉引用',
  apply: '应用关系',
}

interface StructureEntry {
  label: string
  full: string
  chapterId: number
  chapterTitle: string
  sectionTitle?: string
}

let structureIndex: Map<string, StructureEntry> | null = null

/** 'chapter-1' / '2-6' / '2-6-3' → 标题、所属章节等，供跨章引用生成标签 */
function getStructureIndex(): Map<string, StructureEntry> {
  if (structureIndex) return structureIndex
  const map = new Map<string, StructureEntry>()
  courseKnowledgeStructure.chapters.forEach((chapter) => {
    const chapterId = parseInt(chapter.id.replace('chapter-', ''), 10)
    const chapterTitle = chapter.title
    const shortChapter = stripChapterPrefix(chapterTitle)
    map.set(chapter.id, { label: shortChapter, full: shortChapter, chapterId, chapterTitle })
    chapter.sections.forEach((section) => {
      map.set(section.id, {
        label: stripNumberPrefix(section.title),
        full: section.title,
        chapterId,
        chapterTitle,
      })
      section.subsections.forEach((sub) => {
        map.set(sub.id, {
          label: stripNumberPrefix(sub.title),
          full: sub.title,
          chapterId,
          chapterTitle,
          sectionTitle: section.title,
        })
      })
    })
  })
  structureIndex = map
  return map
}

/** 给每个节点补上"位置 + 前置 / 递进 / 对比扩展 / 跨章引用 / 应用"索引 */
function attachNodeContext(data: ConceptGraphData, chapterId?: number): void {
  const { nodes, edges } = data
  const index = getStructureIndex()
  const byId = new Map(nodes.map((n) => [n.id, n]))

  const refOf = (id: string, type: RelationKind, note?: string): NodeRef | null => {
    const entry = index.get(id)
    const local = byId.get(id)
    if (!entry && !local) return null
    return {
      id,
      label: entry?.label || local?.data.label || id,
      type,
      note,
      chapterId: entry?.chapterId,
      inView: !!local,
    }
  }

  const groupNamesOf = (chapterNo?: number) =>
    chapterNo === undefined
      ? []
      : prereqGroups.filter((g) => g.supports.indexOf(`chapter-${chapterNo}`) >= 0).map((g) => g.label)

  nodes.forEach((node) => {
    const id = node.id
    const kind = node.data.kind
    const entry = index.get(id)
    const chapterNo =
      chapterId ??
      node.data.chapterId ??
      (kind === 'chapter' ? parseInt(id.replace('chapter-', ''), 10) : entry?.chapterId)

    const prereq: NodeRef[] = []
    const next: NodeRef[] = []
    const cross: NodeRef[] = []
    const apply: NodeRef[] = []
    const outline: NodeRef[] = []
    const push = (list: NodeRef[], ref: NodeRef | null) => {
      if (!ref || ref.id === id || list.some((x) => x.id === ref.id)) return
      list.push(ref)
    }

    edges.forEach((e) => {
      const t = e.data.relationType || 'progress'
      const isCompare = t === 'compare' || t === 'extend' || t === 'cross'
      if (e.data.kind === 'relation') {
        if (e.source === id) {
          const ref = refOf(e.target, t, e.data.label)
          if (isCompare) push(cross, ref)
          else if (t === 'apply') push(apply, ref)
          else push(next, ref)
        } else if (e.target === id) {
          const ref = refOf(e.source, t, e.data.label)
          if (isCompare) push(cross, ref)
          else if (t === 'apply') push(apply, ref)
          else push(prereq, ref)
        }
      } else if ((e.data.kind === 'application' || e.data.kind === 'experiment') && e.target === id) {
        push(apply, refOf(e.source, 'apply', e.data.kind === 'application' ? '工程应用' : '实验'))
      } else if (e.data.kind === 'prereq' && e.target === id) {
        const src = byId.get(e.source)
        push(prereq, refOf(e.source, 'prereq', src ? `先修基础 · ${src.data.groupLabel || ''}` : undefined))
      }
    })

    // 所在主题 / 章节在课程中的位置：让每个知识点都能说清"我在哪、前后是什么"
    const unitId =
      kind === 'subsection' ? node.data.themeId || id.split('-').slice(0, 2).join('-') : kind === 'section' ? id : undefined
    const chapterIdOfNode = chapterId ?? node.data.chapterId ?? entry?.chapterId
    if (unitId) {
      const chapterKey = chapterIdOfNode !== undefined ? `chapter-${chapterIdOfNode}` : ''
      relations.forEach((r) => {
        if (r.source === chapterKey && r.target !== id) {
          push(outline, refOf(r.target, r.type || 'progress', `本章的后续章节 · ${r.label || ''}`))
        } else if (r.target === chapterKey && r.source !== id) {
          push(outline, refOf(r.source, r.type || 'prereq', `本章的前置章节 · ${r.label || ''}`))
        }
      })
      chapterRelations.forEach((r) => {
        if (r.source === unitId && r.target !== id) {
          push(outline, refOf(r.target, r.type || 'progress', `同一主题的后续 · ${r.label || ''}`))
        } else if (r.target === unitId && r.source !== id) {
          push(outline, refOf(r.source, r.type || 'progress', `同一主题的前置 · ${r.label || ''}`))
        }
      })
    }

    // 跨章节显式引用：单章视图画不下别章节点，这里做成可跳转的索引
    knowledgeLinks.forEach((l) => {
      if (l.source === id) {
        push(cross, refOf(l.target, l.type || 'cross', l.note))
      } else if (l.target === id) {
        push(cross, refOf(l.source, l.type || 'cross', l.note))
      }
    })

    const position = (() => {
      if (kind === 'root') {
        return chapterId === undefined
          ? '课程总览 · 电磁场与电磁波'
          : `第 ${chapterId} 章 · ${node.data.fullLabel || node.data.label}`
      }
      if (kind === 'section' || kind === 'subsection') {
        const ch = node.data.chapterId
        const title = node.data.fullLabel || node.data.label
        const theme = node.data.sectionTitle
        if (kind === 'subsection') return `第 ${ch} 章 · ${theme ? theme + ' · ' : ''}${title}`
        return `第 ${ch} 章 · ${node.data.label}`
      }
      if (kind === 'chapter') {
        if (!entry) return node.data.label
        return `第 ${entry.chapterId} 章 · ${entry.full}`
      }
      if (kind === 'prereq') return `先修基础 · ${node.data.groupLabel || ''}`
      return kind === 'application' ? '工程应用节点' : '实验节点'
    })()

    node.data.context = { position, outline, prereq, next, cross, apply, foundation: groupNamesOf(chapterNo) }
  })
}

// ===== 布局 =====
// 不用 G6 radial：radial 只按拓扑距离定位，组内无连边的节点会被随意散落，
// combo 随之被拉成覆盖大半个画布的巨大椭圆（见 docs/knowledge-graph-plan.md A 项）。
// 这里改为确定性分层椭圆布局，坐标直接写进节点 style：
//   · 根节点居中；
//   · 一级节点（章 / 节）均匀分布在内圈，环半径由该圈节点宽度之和决定；
//   · 二级节点（知识点）围绕各自父节点成扇形展开，超过 3 个时内外两排交错；
//   · 前置知识组 / 应用 / 实验按目标节点方位排在外圈，同圈按角度贪心展开避免重叠；
//   · 最后按画布宽高比做纵向压缩，圆形布局变成椭圆，宽屏下更省空间、标签更大。

const TAU = Math.PI * 2
const NODE_GAP = 40        // 同圈相邻节点的最小间隙
const FAN_GAP = 40         // 扇形内同排节点的最小间隙
const RING_GAP = 152       // 相邻环之间的半径增量：需容纳父/子节点在半径方向上的半宽之和
const EXT_RING_GAP = 76    // 扩展节点相对其目标节点的半径增量（不足时按避让自动外移）
const INNER_RADIUS = 268   // 一级环的最小半径
const LAYOUT_ASPECT = 1.32 // 布局宽高比（单章视图会按需传入更大的值以适配宽画布）
const CLUSTER_GAP = 14     // 前置知识组内成员的横向间隙
const CLUSTER_ROW_H = 62   // 前置知识组内两行之间的行距

interface LayoutUnit {
  key: string
  members: string[]
  /** 组内成员的最大宽度 */
  width: number
  /** 单元外接矩形半宽 / 半高（包含组合框内边距的余量） */
  halfW: number
  halfH: number
  /** 是否为前置知识组（组合框比节点本身大） */
  grouped: boolean
  desired: number
  /** 组内成员的列数（按方位自适应） */
  cols?: number
  /** 最终排布角度与半径（半径缺省使用外环半径） */
  angle?: number
  radius?: number
}

function ringRadius(widths: number[], minRadius: number): number {
  const need = widths.reduce((sum, w) => sum + w + NODE_GAP, 0)
  return Math.max(minRadius, need / TAU)
}

/** 为所有节点计算预设坐标，返回同一份数据（节点上带 style.x / style.y） */
export function layoutConceptGraph(data: ConceptGraphData, options: { aspect?: number } = {}): ConceptGraphData {
  const { nodes, edges, combos } = data
  if (nodes.length === 0) return data
  const aspect = options.aspect && options.aspect > 1 ? options.aspect : LAYOUT_ASPECT

  const byId = new Map(nodes.map((n) => [n.id, n]))
  const widthOf = (id: string) => {
    const n = byId.get(id)
    return n ? nodeSize(n.data.kind, n.data.label || '')[0] : 120
  }

  // 只有树形边（main / sub）参与分层，前置 / 应用 / 关联边不参与
  const children = new Map<string, string[]>()
  edges.forEach((e) => {
    if (e.data.kind !== 'main' && e.data.kind !== 'sub') return
    const list = children.get(e.source) || []
    list.push(e.target)
    children.set(e.source, list)
  })

  const pos = new Map<string, [number, number]>()
  const polar = new Map<string, number>() // id -> 方位角
  const place = (id: string, radius: number, angle: number) => {
    polar.set(id, angle)
    pos.set(id, [radius * Math.cos(angle), radius * Math.sin(angle)])
  }

  // ---- 根节点居中 ----
  pos.set('root', [0, 0])
  polar.set('root', 0)

  // ---- 一级 / 二级节点 ----
  // 按每个扇区的实际需要分配角度：知识点多的节占更宽的角度，排列不再等分，
  // 因此密集章节不需要把扇形一路推到很远，节点之间也不会互相叠压。
  const level1 = children.get('root') || []
  const r1 = ringRadius(level1.map(widthOf), INNER_RADIUS)
  let maxRadius = r1
  const rowRadius = (row: number) => r1 + RING_GAP + row * (RING_GAP + 24)
  const rowsOf = (kids: string[]): string[][] => {
    const rowCount = kids.length <= 3 ? 1 : kids.length <= 8 ? 2 : 3
    const out: string[][] = Array.from({ length: rowCount }, () => [])
    kids.forEach((id, i) => out[i % rowCount].push(id))
    return out
  }
  const arcOf = (ids: string[]) => ids.reduce((sum, id) => sum + widthOf(id) + FAN_GAP, 0)

  const budgets = level1.map((pid) => {
    const kids = children.get(pid) || []
    const own = (widthOf(pid) + NODE_GAP + 24) / r1
    if (kids.length === 0) return own
    return Math.max(own, ...rowsOf(kids).map((rowKids, row) => arcOf(rowKids) / rowRadius(row)))
  })
  const totalBudget = budgets.reduce((sum, v) => sum + v, 0)
  const scale = totalBudget > TAU * 0.94 ? (TAU * 0.94) / totalBudget : 1
  // 一圈排不下时不压缩角度（压缩必然重叠），而是把整圈半径等比放大
  const radiusK = scale < 1 ? 1 / scale : 1
  const spanTotal = totalBudget * scale
  // 角预算没用满（主题较少）时，把剩余角度均分成间隔，让各扇区绕整圈均匀铺开：
  // 否则所有节点会挤在上半圈，整幅图的包围盒中心偏离根节点，"最大节点"看起来不在画面中心。
  const gap = level1.length > 0 && spanTotal < TAU * 0.98 ? (TAU - spanTotal) / level1.length : 0
  const totalArc = spanTotal + gap * level1.length
  let cursor = -Math.PI / 2 - totalArc / 2 + gap / 2

  level1.forEach((pid, idx) => {
    const span = budgets[idx] * scale
    const center = cursor + span / 2
    cursor += span + gap
    place(pid, r1 * radiusK, center)

    const kids = children.get(pid) || []
    if (kids.length === 0) return
    rowsOf(kids).forEach((rowKids, row) => {
      if (rowKids.length === 0) return
      const radius = rowRadius(row) * radiusK
      maxRadius = Math.max(maxRadius, radius)
      // 逐个累加偏移：宽窄标签混排时也能保证相邻节点不贴在一起
      const offsets: number[] = []
      let acc = 0
      rowKids.forEach((id, i) => {
        if (i > 0) acc += (widthOf(rowKids[i - 1]) + widthOf(id)) / 2 + FAN_GAP
        offsets.push(acc)
      })
      const total = offsets[offsets.length - 1] || 0
      const maxW = rowKids.reduce((m, id) => Math.max(m, widthOf(id)), 0)
      const needAngle = (total + maxW + FAN_GAP) / radius
      // 扇形只用 92% 的角度预算，剩下 8% 作为相邻"节分组"之间的留白
      const usedAngle = Math.min(needAngle, span * 0.92)
      const shrink = needAngle > 0 ? usedAngle / needAngle : 1
      rowKids.forEach((id, i) => {
        const arc = offsets[i] - total / 2
        place(id, radius, center + (arc / radius) * shrink)
      })
    })
  })

  // ---- 扩展节点：前置组 / 应用 / 实验排在外圈 ----
  const extIds = nodes
    .filter((n) => n.data.kind === 'prereq' || n.data.kind === 'application' || n.data.kind === 'experiment')
    .map((n) => n.id)

  if (extIds.length > 0) {
    const extSet = new Set(extIds)
    const outTargets = new Map<string, string[]>()
    edges.forEach((e) => {
      if (!extSet.has(e.source)) return
      const list = outTargets.get(e.source) || []
      list.push(e.target)
      outTargets.set(e.source, list)
    })

    // 一个前置组算一个单元（成员排成紧凑网格），应用 / 实验各自一个单元
    const units = new Map<string, LayoutUnit>()
    extIds.forEach((id) => {
      const node = byId.get(id) as ConceptNode
      const key = node.combo || id
      let unit = units.get(key)
      if (!unit) {
        unit = { key, members: [], width: 0, halfW: 0, halfH: 0, grouped: !!node.combo, desired: 0 }
        units.set(key, unit)
      }
      unit.members.push(id)
      const w = widthOf(id)
      const h = nodeSize(node.data.kind, node.data.label || '')[1]
      if (unit.members.length === 1) {
        unit.halfW = w / 2
        unit.halfH = h / 2
      }
      unit.width = Math.max(unit.width, w)
      unit.halfW = Math.max(unit.halfW, w / 2)
    })

    const list = Array.from(units.values()).map((u) => {
      // 期望方位 = 目标节点方位角的环形平均
      let sx = 0
      let sy = 0
      let hit = 0
      u.members.forEach((mid) => {
        const targets = outTargets.get(mid) || []
        targets.forEach((tid) => {
          // 章中心没有方位角，跳过，否则会把扩展节点拉偏
          if (tid === 'root') return
          const a = polar.get(tid)
          if (a === undefined) return
          sx += Math.cos(a)
          sy += Math.sin(a)
          hit += 1
        })
      })
      const desired = hit > 0 ? Math.atan2(sy, sx) : 0
      // 组内成员按方位自适应排布：左右两侧竖排（省横向空间，横向是画布的紧缺方向），
      // 上下方横排，斜向则排成 2 列网格。
      const ax = Math.abs(Math.cos(desired))
      const cols = u.members.length < 2 ? 1 : ax > 0.72 ? 1 : ax < 0.35 ? u.members.length : Math.min(2, u.members.length)
      const rows = Math.ceil(u.members.length / cols)
      const halfW = (Math.min(cols, u.members.length) * u.width + CLUSTER_GAP * (Math.min(cols, u.members.length) - 1)) / 2
      const halfH = ((rows - 1) * CLUSTER_ROW_H) / 2 + 24
      return { ...u, desired, cols, halfW: u.grouped ? halfW : u.halfW, halfH: u.grouped ? halfH : u.halfH }
    })
    list.sort((a, b) => a.desired - b.desired)

    // 让最大的空档落在首尾之间，避免贪心排布绕回起点
    if (list.length > 1) {
      let cut = 0
      let maxGap = -1
      list.forEach((u, i) => {
        const next = list[(i + 1) % list.length]
        const gap = (next.desired - u.desired + TAU) % TAU
        if (gap > maxGap) {
          maxGap = gap
          cut = (i + 1) % list.length
        }
      })
      list.push(...list.splice(0, cut))
    }

    // 环形角度松弛：以各自"目标方位"为初值，按占位角互相推开（对称推挤 + 首尾单向推挤），
    // 比单向贪心更均衡 —— 拥堵一侧不会把所有单元一路推到很远的地方。
    let rExt = maxRadius + EXT_RING_GAP
    let angles: number[] = []
    for (let attempt = 0; attempt < 8; attempt++) {
      // 占位角：把外接矩形投影到切线方向（纵向压缩系数一并计入），比用对角线更贴合实际
      const need = list.map((u) => {
        const a = Math.abs(u.desired)
        const tangent = u.halfW * Math.abs(Math.sin(a)) + u.halfH * aspect * Math.abs(Math.cos(a))
        return (tangent + (u.grouped ? 22 : 14)) / rExt
      })
      // 环形松弛：先把"目标方位"按环形顺序展开成一维单调序列（否则 atan2 的
      // (-π, π] 跳变会让相邻判断出现负间隙，把单元推到错误方位），再做线性松弛：
      // 相邻单元角距不得小于各自占位角，同时用弱弹簧拉回目标方位。
      const desired = list.map((u) => u.desired)
      const base: number[] = [desired[0]]
      for (let i = 1; i < list.length; i++) {
        const d = (((desired[i] - base[i - 1]) % TAU) + TAU) % TAU
        base.push(base[i - 1] + d)
      }
      angles = base.slice()
      for (let iter = 0; iter < 300; iter++) {
        let moved = 0
        for (let i = 0; i < list.length; i++) {
          const j = (i + 1) % list.length
          const b = j === 0 ? angles[0] + TAU : angles[j]
          const gap = b - angles[i]
          const minGap = need[i] + need[j]
          if (gap >= minGap) continue
          const delta = (minGap - gap) / 2
          moved += delta
          if (j === 0) {
            // 首尾相邻：把尾部整体前移（与"头部后移"等价），保持基准不变
            angles[i] += delta * 2
          } else {
            angles[i] -= delta
            angles[j] += delta
          }
        }
        // 弱弹簧回归目标方位，避免累积漂移
        for (let i = 0; i < list.length; i++) angles[i] += (base[i] - angles[i]) * 0.08
        if (moved < 0.002) break
      }
      const wrapGap = angles[0] + TAU - angles[angles.length - 1]
      if (list.length === 1 || wrapGap >= need[need.length - 1] + need[0] - 1e-6) break
      rExt *= 1.14
    }
    maxRadius = Math.max(maxRadius, rExt)

    // 已定位的树节点外接矩形（含纵向压缩），用于给外圈单元让位
    interface Box { x: number; y: number; hw: number; hh: number }
    const obstacles: Box[] = Array.from(pos.entries()).map(([id, p]) => {
      const n = byId.get(id) as ConceptNode
      const size = nodeSize(n.data.kind, n.data.label || '')
      return { x: p[0], y: p[1] / aspect, hw: size[0] / 2 + 10, hh: size[1] / 2 + 10 }
    })
    const hit = (a: Box, b: Box) => Math.abs(a.x - b.x) < a.hw + b.hw && Math.abs(a.y - b.y) < a.hh + b.hh

    // 写入坐标：组内成员排成紧凑网格（每行 2 个），外接矩形比一字排开更方、更小；
    // 若与已放置的节点外接矩形相交，则沿半径方向外移，直到让开为止。
    // 单元的目标半径：贴着"自己指向的最外层节点"外沿放，连线自然就短；
    // 只指向章中心（root）的单元（前置组、整章级应用）从一级环外沿起步，
    // 由下面的避让循环按需外推，不再一律推到全局最外圈。
    const targetRadius = (u: LayoutUnit) => {
      let r = 0
      u.members.forEach((mid) => {
        (outTargets.get(mid) || []).forEach((tid) => {
          const p = pos.get(tid)
          if (!p) return
          r = Math.max(r, Math.hypot(p[0], p[1]))
        })
      })
      return r
    }

    list.forEach((u, i) => {
      const angle = angles[i]
      const margin = u.grouped ? 26 : 8
      const boxAt = (r: number): Box => ({
        x: r * Math.cos(angle),
        y: (r * Math.sin(angle)) / aspect,
        hw: u.halfW + margin,
        hh: u.halfH + margin,
      })
      const want = Math.max(r1 * radiusK + EXT_RING_GAP, targetRadius(u) + EXT_RING_GAP)
      let radius = Math.min(Math.max(want, r1 * radiusK), rExt)
      let box = boxAt(radius)
      for (let k = 0; k < 20 && obstacles.some((o) => hit(o, box)); k++) {
        radius += 24
        box = boxAt(radius)
      }
      u.radius = radius
      obstacles.push(box)

      const cx = radius * Math.cos(angle)
      const cy = radius * Math.sin(angle)
      if (u.members.length === 1) {
        pos.set(u.members[0], [cx, cy])
        polar.set(u.members[0], angle)
        return
      }
      const cols = u.cols || Math.min(2, u.members.length)
      const rows: string[][] = []
      u.members.forEach((id, j) => {
        const r = Math.floor(j / cols)
        rows[r] = rows[r] || []
        rows[r].push(id)
      })
      rows.forEach((rowIds, r) => {
        const rowWidth = rowIds.reduce((sum, id) => sum + widthOf(id) + CLUSTER_GAP, -CLUSTER_GAP)
        let x = cx - rowWidth / 2
        const y = cy + (r - (rows.length - 1) / 2) * CLUSTER_ROW_H
        rowIds.forEach((id) => {
          const w = widthOf(id)
          pos.set(id, [x + w / 2, y])
          polar.set(id, angle)
          x += w + CLUSTER_GAP
        })
      })
    })
  }

  // ---- 纵向压缩，把圆形布局拉成适配宽画布的椭圆 ----
  nodes.forEach((n) => {
    const p = pos.get(n.id)
    if (!p) return
    n.style = { x: Math.round(p[0]), y: Math.round(p[1] / aspect) }
  })

  return { nodes, edges, combos }
}
