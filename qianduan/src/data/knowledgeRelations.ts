/**
 * 知识图谱扩展关系数据
 * 在 courseKnowledgeStructure 的树形结构之外，补充概念图所需的三类关系：
 *  - prereqGroups  前置知识组（绿色虚线椭圆包裹，绿色虚线箭头指向被支撑知识点）
 *  - applications  工程应用 / 实验节点（紫 / 黑色椭圆，粗实线箭头指向相关知识点）
 *  - relations     章节间的知识依赖（蓝色粗虚线箭头）
 *
 * 节点 id 约定与 knowledgeGraphData.ts 一致：'chapter-1' / '1-2' / '1-2-2'
 */

export interface PrereqItem {
  id: string
  label: string
  description?: string
  /** 可选：点击跳转的路由；未设置时弹窗展示说明 */
  route?: string
}

export interface PrereqGroup {
  id: string
  label: string
  items: PrereqItem[]
  /** 被该前置知识组支撑的节点 id 列表 */
  supports: string[]
}

export interface ApplicationNode {
  id: string
  label: string
  kind: 'application' | 'experiment'
  description?: string
  /** 指向的知识点 id 列表 */
  targets: string[]
  /** 点击跳转路径，默认跳转到电磁仿真页 */
  route?: string
  /** 传给仿真页的主题关键字 */
  topic?: string
  /** 仿真页预选的仿真类型（对应 emulationOptions.value） */
  emulationType?: string
}

/**
 * 知识点之间的关系类型（边上会以文字标注，颜色/线型也按类型区分）：
 *  - prereq   前置依赖：目标知识是当前知识的先修内容
 *  - progress 递进关系：同一主线上的下一步，通常承接当前结论
 *  - compare  对比关系：并列概念之间的异同（如理想介质 / 导电媒质）
 *  - extend   扩展关系：结论向更一般或更深入的方向推广
 *  - cross    交叉引用：跨章节的相互引用，需回看或前瞻
 *  - apply    应用关系：知识点的工程应用 / 实验
 */
export type RelationKind = 'prereq' | 'progress' | 'compare' | 'extend' | 'cross' | 'apply'

export interface Relation {
  source: string
  target: string
  label?: string
  /** 关系类型，缺省按 progress 处理 */
  type?: RelationKind
}

/** 跨章节的知识点引用：源知识点在别章还有哪些相关内容，构成完整的知识网络 */
export interface KnowledgeLink extends Relation {
  /** 说明这条引用的逻辑衔接（因果 / 对比 / 扩展 …） */
  note?: string
}

// ===== 前置知识组 =====
export const prereqGroups: PrereqGroup[] = [
  {
    id: 'prereq-math',
    label: '高等数学',
    items: [
      { id: 'prereq-math-vector', label: '矢量微积分', description: '梯度、散度、旋度的数学基础' },
      { id: 'prereq-math-pde', label: '偏微分方程', description: '拉普拉斯方程、泊松方程、波动方程的求解' },
      { id: 'prereq-math-complex', label: '复变函数', description: '相量法与复数运算' },
    ],
    supports: ['chapter-1', 'chapter-4', 'chapter-5'],
  },
  {
    id: 'prereq-physics',
    label: '大学物理',
    items: [
      { id: 'prereq-phys-estatic', label: '静电场', description: '库仑定律、高斯定理' },
      { id: 'prereq-phys-mag', label: '恒定磁场', description: '安培环路定理、毕奥-萨伐尔定律' },
      { id: 'prereq-phys-induction', label: '电磁感应', description: '法拉第电磁感应定律' },
    ],
    supports: ['chapter-2', 'chapter-3', 'chapter-5'],
  },
  {
    id: 'prereq-circuit',
    label: '电路基础',
    items: [
      { id: 'prereq-circ-element', label: '电路元件', description: '电阻、电容、电感的电路模型' },
      { id: 'prereq-circ-kirchhoff', label: '基尔霍夫定律', description: '节点电流与回路电压定律' },
      { id: 'prereq-circ-impedance', label: '阻抗与匹配', description: '交流电路的阻抗概念' },
    ],
    supports: ['chapter-8', '9-6-2'],
  },
]

// ===== 工程应用 / 实验节点 =====
export const applications: ApplicationNode[] = [
  {
    id: 'app-antenna',
    label: '天线设计',
    kind: 'application',
    description: '偶极子天线、阵列天线的辐射特性设计',
    targets: ['chapter-9', '9-6-1'],
    topic: '天线辐射方向图',
    emulationType: 'doublet',
  },
  {
    id: 'app-microwave',
    label: '微波传输线',
    kind: 'application',
    description: '同轴线、微带线、波导的工程应用',
    targets: ['chapter-8', '8-1-1'],
    topic: '微波传输线',
    emulationType: 'zxb',
  },
  {
    id: 'app-shielding',
    label: '电磁屏蔽与吸波',
    kind: 'application',
    description: '屏蔽材料与吸波材料的设计原理',
    targets: ['chapter-6', 'chapter-7', '7-7-6'],
    topic: '电磁屏蔽',
    emulationType: 'qfs',
  },
  {
    id: 'exp-estatic',
    label: '静电场仿真实验',
    kind: 'experiment',
    description: '有限差分法求解二维静电场分布',
    targets: ['chapter-4', '4-4-1'],
    topic: '静电场有限差分仿真',
    emulationType: 'gs',
  },
  {
    id: 'exp-planewave',
    label: '平面波传播实验',
    kind: 'experiment',
    description: '均匀平面波在有耗媒质中的传播仿真',
    targets: ['chapter-6', '6-1-2'],
    topic: '平面波传播',
    emulationType: 'zxb',
  },
  {
    id: 'exp-reflection',
    label: '反射与折射实验',
    kind: 'experiment',
    description: '平面波斜入射分界面的反射系数测量',
    targets: ['chapter-7', '7-3-1'],
    topic: '平面波反射折射',
    emulationType: 'ybfs',
  },
]

// ===== 知识依赖（章节间 + 章内） =====
// 章节间的关联只在课程总览中绘制；章内的节 / 知识点关联在对应单章视图中绘制，
// 两者都用同一套 relation 边样式，让单章图谱和总览一样有交叉依赖关系。
export const relations: Relation[] = [
  // ---- 章节间（课程总览） ----
  // 依据教材组织逻辑：数学工具 → 静态场（电/磁平行）→ 边值问题（方法论）
  // → 麦克斯韦方程组（分水岭）→ 无界传播 → 界面/导行 → 辐射
  { source: 'chapter-1', target: 'chapter-2', label: '梯度·散度·旋度', type: 'prereq' },
  { source: 'chapter-1', target: 'chapter-5', label: '矢量恒等式与亥姆霍兹定理', type: 'prereq' },
  { source: 'chapter-2', target: 'chapter-3', label: '电与磁的对偶', type: 'compare' },
  { source: 'chapter-2', target: 'chapter-4', label: '泊松方程→边值问题', type: 'prereq' },
  { source: 'chapter-2', target: 'chapter-5', label: '静电场方程', type: 'prereq' },
  { source: 'chapter-3', target: 'chapter-5', label: '恒定磁场方程', type: 'prereq' },
  { source: 'chapter-5', target: 'chapter-6', label: '波动方程', type: 'progress' },
  { source: 'chapter-6', target: 'chapter-7', label: '平面波入射分界面', type: 'progress' },
  { source: 'chapter-6', target: 'chapter-8', label: '导行波', type: 'progress' },
  { source: 'chapter-5', target: 'chapter-9', label: '时变场与辐射源', type: 'progress' },
]

/** 章内知识依赖：source / target 为节（x-y）或知识点（x-y-z），仅在单章视图绘制 */
export const chapterRelations: Relation[] = [
  // 端点均为图谱中的核心知识点（提炼后的节点）
  // 第 1 章 矢量分析：坐标系 → 梯度 → 散度 → 旋度 → 场的性质 → 亥姆霍兹定理
  { source: '1-2-3', target: '1-3-3', label: '坐标系 → 梯度', type: 'progress' },
  { source: '1-3-3', target: '1-4-2', label: '梯度 → 散度', type: 'progress' },
  { source: '1-4-2', target: '1-5-2', label: '通量散度 → 环量旋度', type: 'progress' },
  { source: '1-5-2', target: '1-6-1', label: '旋度 → 无旋·无散', type: 'progress' },
  { source: '1-6-1', target: '1-8', label: '场性质 → 亥姆霍兹定理', type: 'prereq' },
  // 第 2 章 静电场：库仑 → 高斯 → 电位 → 泊松 → 边界 → 电容 → 能量 → 恒定电场
  { source: '2-1-2', target: '2-2-2', label: '库仑定律 → 高斯定理', type: 'progress' },
  { source: '2-2-2', target: '2-4-1', label: '无旋场 → 电位函数', type: 'prereq' },
  { source: '2-4-1', target: '2-7', label: '电位 → 泊松方程', type: 'prereq' },
  { source: '2-7', target: '2-8-2', label: '泊松方程 → 边界条件', type: 'prereq' },
  { source: '2-8-2', target: '2-9-1', label: '边界条件 → 电容', type: 'progress' },
  { source: '2-9-1', target: '2-10-2', label: '电容 → 电场能量', type: 'progress' },
  { source: '2-10-2', target: '2-11-4', label: '静电场 → 恒定电场（静电比拟）', type: 'extend' },
  // 第 3 章 恒定磁场：与第 2 章对称
  { source: '3-1-2', target: '3-2-3', label: '毕奥-萨伐尔 → 安培环路', type: 'progress' },
  { source: '3-2-3', target: '3-2-2', label: '场方程 → 矢量磁位', type: 'prereq' },
  { source: '3-2-2', target: '3-2-4', label: '矢量磁位 ⇄ 标量磁位', type: 'compare' },
  { source: '3-2-3', target: '3-3-2', label: '场方程 → 介质磁化', type: 'progress' },
  { source: '3-3-2', target: '3-4-2', label: '磁化 → 边界条件', type: 'progress' },
  { source: '3-5-1', target: '3-6-1', label: '电感 → 磁场能量', type: 'progress' },
  // 第 4 章 边值问题：唯一性定理是三种解法的共同前提
  { source: '4-1-2', target: '4-2-1', label: '唯一性定理 → 分离变量法', type: 'prereq' },
  { source: '4-1-2', target: '4-3-1', label: '唯一性定理 → 镜像法', type: 'prereq' },
  { source: '4-1-2', target: '4-4-1', label: '唯一性定理 → 有限差分法', type: 'prereq' },
  { source: '4-2-1', target: '4-3-1', label: '解析解法：分离变量 ⇄ 镜像', type: 'compare' },
  { source: '4-3-1', target: '4-4-1', label: '解析解 → 数值解', type: 'extend' },
  // 第 5 章 麦克斯韦方程组：方程组 → 本构/边界 → 时谐与能量 → 波动方程
  { source: '5-1-1', target: '5-1-5', label: '位移电流 → 方程组', type: 'progress' },
  { source: '5-1-5', target: '5-1-6', label: '方程组 → 本构方程', type: 'progress' },
  { source: '5-1-6', target: '5-2-2', label: '本构 → 边界条件', type: 'progress' },
  { source: '5-1-5', target: '5-3-2', label: '时域形式 ⇄ 复数形式', type: 'compare' },
  { source: '5-3-2', target: '5-4-1', label: '时谐场 → 坡印亭定理', type: 'progress' },
  { source: '5-1-5', target: '5-6', label: '方程组 → 波动方程', type: 'prereq' },
  // 第 6 章 无界媒质中的均匀平面波
  { source: '6-1-1', target: '6-1-2', label: '波解 → 传播特性', type: 'progress' },
  { source: '6-1-2', target: '6-2-4', label: '平面波 → 极化', type: 'progress' },
  { source: '6-1-2', target: '6-3-1', label: '理想介质 → 导电媒质', type: 'progress' },
  { source: '6-3-1', target: '6-3-4', label: '衰减常数 → 趋肤效应', type: 'progress' },
  { source: '6-3-1', target: '6-4-1', label: '解析解 → FDTD', type: 'extend' },
  // 第 7 章 反射与折射
  { source: '7-1', target: '7-2', label: '垂直入射：导体 ⇄ 介质', type: 'compare' },
  { source: '7-2', target: '7-3-1', label: '垂直入射 → 斜入射', type: 'progress' },
  { source: '7-3-1', target: '7-4-3', label: '斜入射：导体 ⇄ 介质', type: 'compare' },
  { source: '7-4-3', target: '7-5', label: '理想介质 → 导电媒质分界面', type: 'progress' },
  { source: '7-5', target: '7-6', label: '单界面 → 多层媒质', type: 'extend' },
  { source: '7-6', target: '7-7-1', label: '多层媒质 → 人工电磁材料', type: 'extend' },
  // 第 8 章 导行电磁波
  { source: '8-1-1', target: '8-1-3', label: 'TEM ⇄ TE/TM', type: 'compare' },
  { source: '8-1-1', target: '8-2-2', label: '模式 → 双导体传输线', type: 'progress' },
  { source: '8-1-3', target: '8-3-3', label: 'TE 波 → 矩形波导主模', type: 'apply' },
  { source: '8-3-3', target: '8-3-4', label: '主模 → 传播特性与功率', type: 'progress' },
  { source: '8-3-4', target: '8-5-1', label: '波导 → 谐振腔', type: 'progress' },
  { source: '8-2-2', target: '8-6', label: '微带线 → 基片集成波导', type: 'progress' },
  // 第 9 章 电磁辐射
  { source: '9-1', target: '9-2-2', label: '滞后位 → 电偶极子辐射', type: 'progress' },
  { source: '9-2-2', target: '9-4', label: '电偶极子 ⇄ 磁偶极子', type: 'compare' },
  { source: '9-2-2', target: '9-5-2', label: '元天线 → 对称振子', type: 'extend' },
  { source: '9-5-2', target: '9-6-1', label: '振子 → 方向性', type: 'progress' },
  { source: '9-6-1', target: '9-6-5', label: '方向性 → 增益', type: 'progress' },
]

/**
 * 跨章节知识点引用：单章视图里画不下别章的节点，因此把这些关系做成
 * 显式索引——悬浮卡片会列出该知识点在前 / 后章节的位置与逻辑衔接，并可点击跳转。
 */
export const knowledgeLinks: KnowledgeLink[] = [
  // ---- 第 1 章 矢量分析 → 后续章节（算子 → 场方程） ----
  { source: '1-3-3', target: '2-4-1', type: 'prereq', label: '梯度→电位', note: 'E = −∇φ：电位是电场强度的负梯度，第 2 章据此引入电位函数' },
  { source: '1-4-2', target: '2-2-2', type: 'prereq', label: '散度→高斯定理', note: '散度描述通量源；配合散度定理即得高斯定理的微分与积分形式' },
  { source: '1-5-2', target: '3-2-3', type: 'prereq', label: '旋度→安培环路定理', note: '对 B 的旋度做面积分并用斯托克斯定理化为环路积分，即得安培环路定理' },
  { source: '1-6-1', target: '2-4-1', type: 'prereq', label: '无旋场→电位函数', note: '无旋场可表示为某标量场的梯度，这是引入电位的数学前提' },
  { source: '1-6-1', target: '3-2-3', type: 'prereq', label: '无散场→磁通连续', note: '无散场对应磁通连续性原理，并可据此引入矢量磁位 A' },
  { source: '1-8', target: '4-1-2', type: 'prereq', label: '亥姆霍兹定理→唯一性定理', note: '场由散度、旋度与边界条件唯一确定，是边值问题唯一性定理的数学依据' },
  { source: '1-4-2', target: '5-4-1', type: 'prereq', label: '散度定理→坡印亭定理', note: '对坡印亭矢量取散度并积分，用散度定理化为面积分，即得坡印亭定理' },
  { source: '1-8', target: '5-6', type: 'extend', label: '亥姆霍兹定理→位函数', note: '任意矢量场由散度与旋度唯一确定，是引入动态位 A、φ 的理论依据' },
  // ---- 第 2 章 静电场 → 后续章节（与磁场对称 / 汇入麦克斯韦方程组） ----
  { source: '2-6-3', target: '3-3-2', type: 'compare', label: '介质极化与磁化', note: '电介质的极化与磁介质的磁化是电、磁两侧的平行概念' },
  { source: '2-8-2', target: '3-4-2', type: 'compare', label: '电与磁的边界条件', note: '法向连续 / 切向连续的形式一致，可对照记忆' },
  { source: '2-9-1', target: '3-5-1', type: 'compare', label: '电容与电感', note: '电容储存电场能、电感储存磁场能，二者互为对偶' },
  { source: '2-10-2', target: '3-6-1', type: 'compare', label: '电场能与磁场能', note: '能量密度表达式形式相同，只差媒质参数' },
  { source: '2-7', target: '4-1-2', type: 'progress', label: '泊松方程→边值问题', note: '泊松方程加上边界条件，就是第 4 章要解的边值问题' },
  { source: '2-2-2', target: '5-1-5', type: 'progress', label: '高斯定理→第三方程', note: '静电场的高斯定理在时变场中形式不变，即麦克斯韦第三方程' },
  { source: '2-4-1', target: '5-1-5', type: 'prereq', label: '静电场无旋→第二方程', note: '静电场旋度为零只是特例；时变时旋度由 −∂B/∂t 提供，即法拉第电磁感应定律' },
  { source: '2-6-3', target: '5-1-6', type: 'extend', label: '本构关系', note: '介质中 D 与 E 的关系在第 5 章统一写成本构方程' },
  { source: '2-5-1', target: '9-2-2', type: 'extend', label: '静电偶极子→辐射元', note: '第 9 章的电偶极子辐射是同一模型在时变条件下的推广' },
  { source: '2-9-1', target: '8-2-2', type: 'apply', label: '电容→传输线分布电容', note: '传输线的分布电容是电容概念在导行波中的体现' },
  // ---- 第 3 章 恒定磁场 → 后续章节（逐条推广为时变形式） ----
  { source: '3-2-3', target: '5-1-1', type: 'prereq', label: '安培环路定理→第一方程', note: '时变情形下安培环路定理须补上位移电流项，即麦克斯韦第一方程' },
  { source: '3-2-3', target: '5-1-5', type: 'progress', label: '磁通连续→第四方程', note: '磁通连续性原理在时变场中形式不变，即麦克斯韦第四方程' },
  { source: '3-2-2', target: '5-6', type: 'extend', label: '矢量磁位→动态位', note: '恒定场的矢量磁位在时变场中扩展为动态位 A 与标量位 φ' },
  { source: '3-3-1', target: '9-4', type: 'extend', label: '磁偶极子→对偶原理', note: '磁偶极子模型在时变条件下产生辐射场，其与电偶极子的对应关系即对偶原理' },
  { source: '3-4-2', target: '5-2-2', type: 'extend', label: '切向边界条件', note: '时变场的切向边界条件与恒定场形式相同' },
  { source: '3-5-1', target: '8-2-2', type: 'apply', label: '电感→传输线分布电感', note: '传输线的分布电感是电感概念在导行波中的体现' },
  { source: '3-6-1', target: '5-4-1', type: 'extend', label: '磁场能→坡印亭定理', note: '能量守恒的普适形式统一了电场能、磁场能与辐射功率' },
  // ---- 第 4 章 边值问题 → 后续章节 ----
  { source: '4-1-2', target: '5-6', type: 'extend', label: '唯一性定理', note: '时变场的唯一性定理与静电场同一思路，只是多给了初始条件' },
  { source: '4-2-1', target: '8-3-4', type: 'apply', label: '分离变量法→波导场解', note: '矩形波导中的场分布正是直角坐标分离变量法的结果' },
  { source: '4-3-1', target: '7-1', type: 'compare', label: '镜像法→导体面反射', note: '平面镜像与理想导体表面的反射都可用镜像电荷 / 镜像波源理解' },
  { source: '4-4-1', target: '6-4-1', type: 'extend', label: '差分法→FDTD', note: '时域有限差分是有限差分法在时变麦克斯韦方程上的推广' },
  // ---- 第 5 章 麦克斯韦方程组 → 后续章节（波与能量的源头） ----
  { source: '5-6', target: '6-1-1', type: 'prereq', label: '波动方程→亥姆霍兹方程', note: '无源波动方程在时谐条件下化为亥姆霍兹方程，是第 6 章求解的出发点' },
  { source: '5-2-2', target: '7-1', type: 'apply', label: '边界条件→垂直入射', note: '平面波在分界面上的反射、折射由边界条件直接给出' },
  { source: '5-3-2', target: '6-3-1', type: 'progress', label: '复数形式→导电媒质波', note: '复数形式让导电媒质中的传播常数、衰减常数可以统一处理' },
  { source: '5-4-1', target: '8-3-4', type: 'apply', label: '坡印亭定理→传输功率', note: '波导传输功率由坡印亭矢量沿截面积分得到' },
  { source: '5-4-1', target: '9-6-5', type: 'apply', label: '平均坡印亭矢量→增益', note: '天线增益定义为远区功率密度与理想点源之比' },
  { source: '5-6', target: '9-1', type: 'extend', label: '波动方程→滞后位', note: '考虑源的时间滞后，位函数的解就是第 9 章的滞后位' },
  // ---- 第 6 章 无界媒质中的均匀平面波 → 后续章节 ----
  { source: '6-1-1', target: '7-2', type: 'progress', label: '平面波→入射到分界面', note: '把平面波解代入边界条件，即进入第 7 章的反射与折射' },
  { source: '6-1-2', target: '9-2-2', type: 'compare', label: '平面波与球面波', note: '天线远区辐射场是球面波，可与平面波的传播特性对照' },
  { source: '6-2-5', target: '9-6-3', type: 'progress', label: '极化→天线极化', note: '天线极化方式与来波极化的匹配程度决定接收效率' },
  { source: '6-3-1', target: '7-5', type: 'progress', label: '导电媒质中的波→导电媒质分界面', note: '第 7.5 节把反射折射推广到有耗媒质，沿用复介电常数与复波阻抗' },
  { source: '6-3-4', target: '8-2-2', type: 'apply', label: '趋肤效应→同轴损耗', note: '高频下导体损耗按表面电阻计算，同轴线的衰减即由此而来' },
  { source: '6-3-5', target: '8-3-4', type: 'apply', label: '表面阻抗→波导损耗', note: '波导壁损耗由表面阻抗与壁电流共同决定' },
  { source: '6-4-1', target: '4-4-1', type: 'cross', label: '吸收边界→数值方法', note: 'UPML 属于数值方法范畴，回看第 4 章的差分思想更易理解' },
  // ---- 第 7 章 反射与折射 → 后续章节 ----
  { source: '7-2', target: '8-1-1', type: 'progress', label: '分界面反射→导行波', note: '波在两面导体间的多次反射叠加形成导行波，进入第 8 章' },
  { source: '7-4-3', target: '8-1-1', type: 'extend', label: '全反射→表面波', note: '全反射产生的表面波是导行波的一种极端情形' },
  { source: '7-6', target: '8-6', type: 'extend', label: '多层媒质→基片集成波导', note: '多层媒质分析方法是理解基片集成波导结构的基础' },
  { source: '7-7-6', target: '6-3-5', type: 'cross', label: '吸波材料→表面阻抗', note: '完美吸波依赖有耗媒质中的损耗机制，可回看表面阻抗与交流电阻' },
  // ---- 第 8 章 导行电磁波 → 后续章节 ----
  { source: '8-2-2', target: '9-6-2', type: 'apply', label: '传输线→输入阻抗', note: '天线的输入阻抗与驻波比用传输线理论分析' },
  { source: '8-1-1', target: '6-1-2', type: 'cross', label: 'TEM 波→理想介质平面波', note: 'TEM 波无截止、波阻抗与平面波相同，可直接对照第 6 章' },
  { source: '8-5-1', target: '9-5-2', type: 'cross', label: '谐振腔→天线的谐振', note: '谐振腔的谐振概念与天线的谐振长度、谐振频率相通' },
  // ---- 第 9 章 电磁辐射（回看与横向关联） ----
  { source: '9-2-2', target: '6-1-2', type: 'cross', label: '辐射场→平面波', note: '远区场局部近似为平面波，可用第 6 章的结论分析' },
  { source: '9-4', target: '3-3-1', type: 'prereq', label: '对偶原理→磁偶极子', note: '对偶原理建立在磁偶极子与电偶极子的对应关系上' },
  { source: '9-6-2', target: '5-4-1', type: 'prereq', label: '输入阻抗→复坡印亭', note: '天线输入阻抗的实部即辐射功率，与复坡印亭矢量一致' },
]
