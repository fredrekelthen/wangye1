# 概念图式知识图谱 — 实施方案

> 目标：把课程页的知识图谱从当前的"左右对称思维导图"升级为 `模板.png` 所示的**概念图**样式（中心辐射 + 前置知识分组 + 应用/实验节点 + 跨知识点关联），并让每类节点点击后跳转到对应页面。
>
> 本文是实施计划，由 Claude Code 直接完成，不依赖外部模型生成代码。

---

## 1. 模板图结构拆解

模板图共 5 类视觉元素，逐一映射到项目数据：

| # | 视觉元素 | 样式 | 语义 | 项目数据来源 |
|---|---|---|---|---|
| 1 | 中心节点 | 红边红字大椭圆 | 课程 / 章主题 | `root`（已有） |
| 2 | 一级节点 | 蓝边黑字椭圆，**红色实线箭头**连中心 | 章 / 节 | `chapter` / `section`（已有） |
| 3 | 二级节点 | 橙边黑字小椭圆，**蓝色实线箭头**连一级 | 知识点 | `subsection`（已有） |
| 4 | 前置知识组 | **绿色虚线大椭圆**包裹若干小椭圆，红字组名，**绿色虚线箭头**指向被支撑节点 | 先修知识（高等数学 / 电路定理…） | **新增** `prereqGroups` |
| 5 | 应用 / 实验节点 | 紫色椭圆（工程应用）、黑色椭圆（实验），**紫 / 黑粗实线箭头**指向相关知识点 | 工程应用与实验 | **新增** `applications` |
| 6 | 跨节点关联 | **蓝色粗虚线箭头**（一级节点之间） | 知识依赖 | **新增** `relations` |

现有 `KnowledgeGraph.vue` 使用 `mindmap` 水平布局，只能表达树形结构，无法画出 4/5/6。核心改动：

- 布局：`mindmap` → `radial`（以 root 为中心辐射）
- 分组：使用 G6 v5 **Combo** 表达前置知识组
- 多类边：按 `data.kind` 区分颜色 / 线型 / 粗细
- 数据：新增 `knowledgeRelations.ts` 承载前置 / 应用 / 关联三类扩展数据

---

## 2. 现状梳理（已核实）

| 项 | 现状 |
|---|---|
| 技术栈 | Vue 3 + TS `<script setup>`、ant-design-vue 4、vue-router 4、`@antv/g6 ^5.0.51` |
| 图谱组件 | `qianduan/src/components/course/KnowledgeGraph.vue`，由 `CourseContent.vue` 挂载，`chapterId` 控制课程总览 / 单章视图 |
| 课程数据 | `qianduan/src/data/knowledgeGraphData.ts` → `courseKnowledgeStructure.chapters[] → sections[] → subsections[]`，id 形如 `chapter-1` / `1-2` / `1-2-2` |
| 知识点跳转 | `window.dispatchEvent(new CustomEvent('course-selected', { detail: { chapter, lesson } }))`，`CourseContent.vue:357` 监听并切到资料视图 |
| 章节跳转 | `emit('nodeClick', { type: 'chapter', chapterId, chapterTitle })`，`CourseContent.vue:300` 处理并切换到单章视图 |
| 页签路由 | `CourseView.vue` 读取 `?tab=xxx`（`content / discuss / notice / test / grade / emulation`），可用 `router.push('/course?tab=emulation')` 跳仿真页 |
| 独立页面 | `/exam`（测试）、`/user-center` 等 |

结论：**跳转链路已具备**，只需为新节点类型补充分发逻辑，不改动 `CourseContent.vue` 监听端。

---

## 3. 文件改动清单

| 文件 | 操作 | 说明 |
|---|---|---|
| `qianduan/src/data/knowledgeRelations.ts` | **新增** | 前置知识组、应用 / 实验、跨节点关联配置（纯数据） |
| `qianduan/src/data/conceptGraphBuilder.ts` | **新增** | 类型定义 + `buildConceptGraph(chapterId?)`，把课程结构和关系配置合并成 G6 数据 |
| `qianduan/src/components/course/KnowledgeGraph.vue` | **改写** | 布局、节点 / 边 / combo 样式、点击分发；保留代码生成对话框、hover、防抖、销毁逻辑 |
| `qianduan/src/components/course/CourseContent.vue` | 不改 | 现有 `course-selected` 监听与 `handleGraphNodeClick` 继续复用 |

不新增 npm 依赖。

---

## 4. 数据模型设计

### 4.1 关系配置 `knowledgeRelations.ts`

```ts
export interface PrereqGroup {
  id: string            // 'prereq-math'
  label: string         // '高等数学'
  items: { id: string; label: string; route?: string }[]
  supports: string[]    // 被支撑的节点 id（chapter-x / x-y / x-y-z）
}

export interface ApplicationNode {
  id: string
  label: string
  kind: 'application' | 'experiment'
  targets: string[]     // 指向的知识点 id
  route?: string        // 点击跳转路径，默认 '/course?tab=emulation'
  topic?: string        // 传给仿真页的主题
}

export interface Relation {
  source: string
  target: string
  label?: string
}

export const prereqGroups: PrereqGroup[]
export const applications: ApplicationNode[]
export const relations: Relation[]
```

首批数据（电磁场与电磁波课程，按模板图风格）：

- 前置知识组：`高等数学`（矢量微积分 / 偏微分方程 / 复变函数）→ 支撑第 1、4 章；`大学物理`（静电场 / 恒定磁场 / 电磁感应）→ 支撑第 2、3、5 章；`电路基础`（电路元件 / 基尔霍夫定律）→ 支撑第 8 章
- 应用 / 实验：`天线设计`→第 9 章；`微波传输线`→第 8 章；`电磁屏蔽`→第 6、7 章；`静电场仿真实验`→ 4-4-1；`平面波传播实验`→ 6-1-2
- 关联：`chapter-1 → chapter-2`、`chapter-2 → chapter-4`、`chapter-3 → chapter-5`、`chapter-5 → chapter-6`、`chapter-6 → chapter-7`、`chapter-6 → chapter-8`、`chapter-5 → chapter-9`

单章视图下：只保留 `supports` / `targets` / `relations` 命中该章节点的项；
章内节与知识点之间的依赖放在 `chapterRelations`，仅在单章视图绘制（样式同 `relations`）。
关系均带 `type`（前置依赖 / 递进 / 对比 / 扩展 / 交叉引用 / 应用），连线分色并标注名称；
跨章节的知识点引用放在 `knowledgeLinks`，由节点悬浮卡片以可点击索引的形式给出
（含位置说明与逻辑衔接，点击可切章并聚焦目标知识点）。

### 4.2 G6 数据 `conceptGraphBuilder.ts`

```ts
type NodeKind = 'root' | 'chapter' | 'section' | 'subsection' | 'prereq' | 'application' | 'experiment'
type EdgeKind = 'main' | 'sub' | 'prereq' | 'application' | 'experiment' | 'relation'

interface ConceptNode {
  id: string
  combo?: string                       // 所属前置知识组（G6 v5 字段名为 combo）
  data: {
    kind: NodeKind
    label: string
    // 跳转所需字段
    chapterId?: number
    chapterTitle?: string
    lesson?: string                    // = course-selected 的 detail.lesson
    route?: string
    topic?: string
    description?: string
  }
}

interface ConceptEdge { id: string; source: string; target: string; data: { kind: EdgeKind; label?: string } }
interface ConceptCombo { id: string; data: { label: string } }

export function buildConceptGraph(chapterId?: number): { nodes: ConceptNode[]; edges: ConceptEdge[]; combos: ConceptCombo[] }
```

`buildConceptGraph` 逻辑：

1. 复用现有 `buildCourseData` / `buildChapterData` 的树形部分（root → chapter → 核心 subsection，或 root → section → subsection），边 `kind` 分别为 `main` / `sub`
2. 遍历 `prereqGroups`：每组生成一个 combo，组内 items 生成 `kind: 'prereq'` 节点并设 `combo`；对每个 `supports` 命中当前视图节点的，生成 `kind: 'prereq'` 边（源为 combo 内第一个节点，G6 v5 combo 本身不能作为边端点时的兜底；若版本支持 combo 作边端点则直接用 combo id）
3. 遍历 `applications`：生成 `kind: application | experiment` 节点，对每个命中的 `targets` 生成同 kind 边
4. 遍历 `relations`：两端都在当前视图时生成 `kind: 'relation'` 边
5. 只保留至少有一条边的扩展节点，避免孤立节点

---

## 5. 渲染方案（G6 v5）

### 5.1 布局

```ts
layout: {
  type: 'radial',
  focusNode: 'root',
  unitRadius: 130,
  linkDistance: 120,
  preventOverlap: true,
  nodeSize: 90,
  strictRadial: false,
}
```

`radial` 让树形部分呈同心圆分布；前置 / 应用节点因只与少数节点相连，会被推到外圈，视觉上接近模板图的外围位置。若 combo 与 radial 配合效果不佳，备选 `force` 布局（`preventOverlap: true, linkDistance: 110, nodeStrength: -300`）。

### 5.2 节点样式（按 `data.kind`）

| kind | fill | stroke | labelFill | 尺寸 |
|---|---|---|---|---|
| root | `#ffffff` | `#e53935`（2.5px） | `#e53935` | 200×80，17px 加粗 |
| chapter / section | `#ffffff` | `#1e40af`（2px） | `#111827` | 160×56，13px |
| subsection | `#ffffff` | `#f59e0b`（1.5px） | `#111827` | 120×48，11px |
| prereq | `#ffffff` | `#1e40af`（1.5px） | `#111827` | 100×44，11px |
| application | `#ffffff` | `#c026d3`（2px） | `#111827` | 130×50，12px |
| experiment | `#ffffff` | `#111827`（2.5px） | `#111827` | 110×50，12px 加粗 |

统一 `type: 'ellipse'`，`labelWordWrap: true`、`labelMaxWidth: '90%'`、`labelMaxLines: 3` 处理长标签。

### 5.3 边样式（按 `data.kind`）

| kind | stroke | lineWidth | lineDash | 曲线 |
|---|---|---|---|---|
| main | `#e53935` | 2 | — | `line` |
| sub | `#2563eb` | 1.5 | — | `line` |
| prereq | `#16a34a` | 1.5 | `[8,4]` | `quadratic` |
| application | `#c026d3` | 2.5 | — | `quadratic` |
| experiment | `#111827` | 2.5 | — | `quadratic` |
| relation | `#2563eb` | 3 | `[10,5]` | `quadratic` |

全部 `endArrow: true`。

### 5.4 Combo 样式

```ts
combo: {
  type: 'circle',
  style: {
    fill: 'transparent',
    stroke: '#16a34a',
    lineWidth: 2,
    lineDash: [8, 4],
    padding: 24,
    labelText: d => d.data.label,
    labelFill: '#e53935',
    labelFontSize: 14,
    labelFontWeight: 700,
    labelPlacement: 'bottom',
  },
}
```

### 5.5 交互 behaviors

沿用 `drag-element`、`zoom-canvas`、`drag-canvas`；移除 `collapse-expand`（radial 布局下折叠意义不大，且会与 combo 点击冲突）。新增 `drag-element` 对 combo 生效（v5 默认支持）。

---

## 6. 点击跳转分发

`handleNodeClick(nodeData)` 按 `kind` 分发：

| kind | 行为 | 落点 |
|---|---|---|
| chapter | `emit('nodeClick', { type: 'chapter', chapterId, chapterTitle })` | `CourseContent` 切到单章图谱（现有） |
| section / subsection | 派发 `course-selected` `{ chapter: chapterTitle, lesson: label }` | `CourseContent` 切到该知识点资料（现有） |
| prereq | 有 `route` → `router.push(route)`；否则弹 `a-modal` 展示说明与所属组 | — |
| application / experiment | `router.push({ path: '/course', query: { tab: 'emulation', topic } })` | 电磁仿真页 |
| root（单章视图） | `emit('nodeClick', { type: 'course-view' })` | 返回课程总览 |
| combo | 高亮组内节点及其 `prereq` 出边（`setElementState(id, 'highlight', true)`），再次点击取消 | 视觉反馈 |

事件层要区分节点与 combo：`graph.on('node:click')` 与 `graph.on('combo:click')` 分开绑定；取 id 用 `evt.target.id`，节点数据用 `graph.getNodeData(id)`。

---

## 7. 实施步骤

1. **数据层**：新建 `knowledgeRelations.ts`（数据）与 `conceptGraphBuilder.ts`（类型 + 构建函数），单元验证：课程视图与 `chapterId=6` 视图各生成的 nodes / edges / combos 数量合理、无悬空边
2. **渲染层**：改写 `KnowledgeGraph.vue` 的 `renderGraph()`，替换布局、样式、behaviors，接入 `buildConceptGraph`；保留代码生成对话框相关代码不动
3. **交互层**：重写 `handleNodeClick` 分发；新增 `combo:click` 高亮；引入 `useRouter`
4. **样式微调**：根据实际渲染效果调整 `unitRadius` / `linkDistance` / combo padding，确保总览视图在 1280px 宽度下无明显重叠
5. **自检**：见第 8 节

---

## 8. 验收清单

- [x] `npm run serve` 后打开 `/course?tab=content` → "查看知识图谱"，Console 无 G6 报错 / 警告
- [x] 课程总览：中心红色节点、9 个章节蓝色节点、3 个绿色虚线组、紫 / 黑应用实验节点、章节间蓝色粗虚线全部可见（总览只保留章 + 扩展元素；橙色知识点在单章视图展示，见 `knowledge-graph-plan.md` B 项）
- [x] 点击章节节点 → 进入单章视图（已验证第 6 章，23 个节点）
- [x] 单章视图只显示与该章相关的前置组 / 应用节点
- [x] 点击知识点 → `CourseContent` 切到该知识点资料（已验证 6-2-1）
- [x] 点击应用 / 实验节点 → `/course?tab=emulation&type=zxb`（已验证来源标签）
- [x] 点击前置知识节点 → 弹出说明（已验证"偏微分方程"）
- [x] 点击 combo → 组内节点与出边高亮，再次点击恢复（组合框加淡填充，整块可点）
- [ ] 拖拽节点 / combo、缩放、平移、hover 高亮正常
- [ ] 视图切换（课程 ↔ 章）多次后无内存泄漏迹象（`graph.destroy()` 正常执行，事件监听已解绑）
- [x] `npm run lint` 通过（0 error），production build 通过

---

## 9. 风险与备选

| 风险 | 应对 |
|---|---|
| G6 v5 `radial` 布局对 combo 支持不完整，组内节点被布局拉散 | 改用 `force` 布局，或在 `buildConceptGraph` 中为 prereq 节点预设 `style.x/y`（相对 combo 中心做小偏移）并在 layout 中对它们 `fixed` |
| Combo 不能作为边端点 | prereq 边源端使用组内第一个节点；视觉上箭头仍从组内发出 |
| 总览视图节点过多（9 章 × 4 知识点 + 扩展节点 ≈ 60 个）导致拥挤 | 总览视图仅显示章节 + 扩展元素，核心知识点只在单章视图展示；或增大 `unitRadius` 并默认 `autoFit: 'view'` |
| 长标签换行后椭圆溢出 | 按 `label.length` 动态放大 `size`，与现有实现一致 |
| `combo:click` 与 `drag-element` 冲突 | 点击判定加 4px 位移阈值，拖动不触发高亮 |
