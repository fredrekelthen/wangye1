/**
 * 电路分析基础 · 核心知识结构
 *
 * 由《电路分析基础》8 张二级架构图（.vsdx）解析并人工校正生成。
 * 结构：章主题(topic) → 知识模块(theme) → 知识点(points)，另附三类外圈关联：
 *   - prereq  前置课程 / 数学工具（高等数学、大学物理、时变电磁场、傅里叶级数等）
 *   - depend  本课程其他章节的依赖引用（电路基础知识、电路定理、正弦稳态电路、分析方法等）
 *   - apply   工程应用 / 实验 / 产品案例
 *
 * 说明：原 vsdx 无 Visio Connects 记录，连线为独立线段，故层级以「中心节点为极点的
 * 半径分环 + 角度扇区」重建，外圈三类按关键词启发式划分；以下为校正后的最终结构，
 * 已修正原文的丢字/错字（π 型、KCL/KVL 括号、叠加定理、等效变换法等）与重复/空标签。
 */

export interface CircuitPoint {
  id: string
  label: string
}

export interface CircuitTheme {
  id: string
  label: string
  points: CircuitPoint[]
  prereq?: string[]
  depend?: string[]
  apply?: string[]
}

export interface CircuitTopic {
  id: string
  label: string
  themes: CircuitTheme[]
}

export const circuitCoreStructure: CircuitTopic[] = [
  {
    id: 'ckt1',
    label: '电路基础知识',
    themes: [
      {
        id: 'ckt1-t1',
        label: '电路和电路模型',
        points: [
          { id: 'ckt1-t1-p1', label: '理想电路元件' },
          { id: 'ckt1-t1-p2', label: '电路' },
          { id: 'ckt1-t1-p3', label: '电路模型' },
        ],
        prereq: ['高等数学', '微分和积分'],
      },
      {
        id: 'ckt1-t2',
        label: '电路变量',
        points: [
          { id: 'ckt1-t2-p1', label: '电压' },
          { id: 'ckt1-t2-p2', label: '电流' },
          { id: 'ckt1-t2-p3', label: '关联参考方向' },
          { id: 'ckt1-t2-p4', label: '电功率' },
        ],
        apply: ['充电宝'],
      },
      {
        id: 'ckt1-t3',
        label: '电路名词',
        points: [
          { id: 'ckt1-t3-p1', label: '支路' },
          { id: 'ckt1-t3-p2', label: '节点' },
          { id: 'ckt1-t3-p3', label: '回路' },
          { id: 'ckt1-t3-p4', label: '网孔' },
          { id: 'ckt1-t3-p5', label: '端口' },
          { id: 'ckt1-t3-p6', label: '单口网络与多口网络' },
        ],
        apply: ['实验'],
      },
      {
        id: 'ckt1-t4',
        label: '电路元件',
        points: [
          { id: 'ckt1-t4-p1', label: '电阻元件' },
          { id: 'ckt1-t4-p2', label: '电容元件' },
          { id: 'ckt1-t4-p3', label: '电感元件' },
          { id: 'ckt1-t4-p4', label: '电压源' },
          { id: 'ckt1-t4-p5', label: '电流源' },
        ],
      },
      {
        id: 'ckt1-t5',
        label: '实际电源模型',
        points: [
          { id: 'ckt1-t5-p1', label: '实际电压源模型' },
          { id: 'ckt1-t5-p2', label: '实际电流源模型' },
        ],
      },
      {
        id: 'ckt1-t6',
        label: '基尔霍夫定律',
        points: [
          { id: 'ckt1-t6-p1', label: '基尔霍夫电流定律 KCL' },
          { id: 'ckt1-t6-p2', label: '基尔霍夫电压定律 KVL' },
        ],
        apply: ['安全用电'],
      },
      {
        id: 'ckt1-t7',
        label: '运算放大器',
        points: [{ id: 'ckt1-t7-p1', label: '受控源' }],
        apply: ['电扇转速控制'],
      },
    ],
  },
  {
    id: 'ckt2',
    label: '电路定理',
    themes: [
      {
        id: 'ckt2-t1',
        label: '叠加定理',
        points: [{ id: 'ckt2-t1-p1', label: '叠加定理的内容' }],
        depend: ['电路元件', '电路变量', '电路基础知识'],
        apply: ['功率问题'],
      },
      {
        id: 'ckt2-t2',
        label: '齐性定律',
        points: [
          { id: 'ckt2-t2-p1', label: '齐性定律的内容' },
          { id: 'ckt2-t2-p2', label: '激励' },
          { id: 'ckt2-t2-p3', label: '响应' },
        ],
      },
      {
        id: 'ckt2-t3',
        label: '替代定理',
        points: [{ id: 'ckt2-t3-p1', label: '替代定理的内容' }],
        depend: ['等效变换法', '电路分析方法'],
        apply: ['实验'],
      },
      {
        id: 'ckt2-t4',
        label: '戴维南定理和诺顿定理',
        points: [
          { id: 'ckt2-t4-p1', label: '开路电压' },
          { id: 'ckt2-t4-p2', label: '戴维南等效电阻' },
          { id: 'ckt2-t4-p3', label: '短路电流' },
        ],
        depend: ['电路名词', '电路基础知识', '电路元件', '实际电源模型'],
        apply: ['能量传输与天线设计'],
      },
      {
        id: 'ckt2-t5',
        label: '特勒根定理',
        points: [
          { id: 'ckt2-t5-p1', label: '定理1：功率守恒' },
          { id: 'ckt2-t5-p2', label: '定理2：似功率守恒' },
        ],
        prereq: ['图论的初步知识'],
        depend: ['电路变量', '基尔霍夫定律', '电路基础知识', '电路名词', '电路分析方法'],
      },
      {
        id: 'ckt2-t6',
        label: '互易定理',
        points: [
          { id: 'ckt2-t6-p1', label: '互易定理的内容' },
          { id: 'ckt2-t6-p2', label: '形式1' },
          { id: 'ckt2-t6-p3', label: '形式2' },
          { id: 'ckt2-t6-p4', label: '形式3' },
        ],
      },
      {
        id: 'ckt2-t7',
        label: '对偶原理',
        points: [{ id: 'ckt2-t7-p1', label: '线性电路' }],
      },
      {
        id: 'ckt2-t8',
        label: '最大功率传输定理',
        points: [
          { id: 'ckt2-t8-p1', label: '直流电阻电路' },
          { id: 'ckt2-t8-p2', label: '阻抗匹配' },
        ],
        depend: ['正弦稳态电路'],
      },
    ],
  },
  {
    id: 'ckt3',
    label: '正弦稳态电路',
    themes: [
      {
        id: 'ckt3-t1',
        label: '正弦量及其相量',
        points: [
          { id: 'ckt3-t1-p1', label: '正弦量' },
          { id: 'ckt3-t1-p2', label: '相量图' },
          { id: 'ckt3-t1-p3', label: '正弦量的相量' },
        ],
        prereq: ['高等数学', '复数', '三角函数'],
      },
      {
        id: 'ckt3-t2',
        label: '电路元件VCR的相量形式',
        points: [
          { id: 'ckt3-t2-p1', label: '电阻元件' },
          { id: 'ckt3-t2-p2', label: '电感元件' },
          { id: 'ckt3-t2-p3', label: '电容元件' },
        ],
        apply: ['实验（相位差测量）'],
      },
      {
        id: 'ckt3-t3',
        label: '电路定律的相量形式',
        points: [
          { id: 'ckt3-t3-p1', label: '欧姆定律' },
          { id: 'ckt3-t3-p2', label: '基尔霍夫定律' },
        ],
        depend: ['电路元件', '电路基础知识', '电路变量'],
      },
      {
        id: 'ckt3-t4',
        label: '阻抗和导纳',
        points: [
          { id: 'ckt3-t4-p1', label: '阻抗' },
          { id: 'ckt3-t4-p2', label: '导纳' },
          { id: 'ckt3-t4-p3', label: '元件的阻抗特性' },
        ],
      },
      {
        id: 'ckt3-t5',
        label: '正弦稳态电路的相量分析',
        points: [
          { id: 'ckt3-t5-p1', label: '相量法' },
          { id: 'ckt3-t5-p2', label: '电路的相量模型' },
          { id: 'ckt3-t5-p3', label: '正弦稳态电路的等效' },
        ],
        depend: ['网孔电流法', '节点电压法', '电路分析方法', '等效变换法'],
      },
      {
        id: 'ckt3-t6',
        label: '正弦稳态电路的功率',
        points: [
          { id: 'ckt3-t6-p1', label: '瞬时功率' },
          { id: 'ckt3-t6-p2', label: '有功功率' },
          { id: 'ckt3-t6-p3', label: '无功功率' },
          { id: 'ckt3-t6-p4', label: '视在功率' },
          { id: 'ckt3-t6-p5', label: '复功率' },
          { id: 'ckt3-t6-p6', label: '功率因数' },
          { id: 'ckt3-t6-p7', label: '功率因数提高' },
        ],
        depend: ['电路定理', '戴维南定理', '最大功率传输定理'],
      },
    ],
  },
  {
    id: 'ckt4',
    label: '动态电路的瞬态分析',
    themes: [
      {
        id: 'ckt4-t1',
        label: '动态电路及电路方程',
        points: [
          { id: 'ckt4-t1-p1', label: '动态电路的概念' },
          { id: 'ckt4-t1-p2', label: '动态电路的电路方程' },
          { id: 'ckt4-t1-p3', label: '动态电路方程的求解' },
        ],
        prereq: ['高等数学', '微分和积分', '线性常系数微分方程求解'],
      },
      {
        id: 'ckt4-t2',
        label: '动态电路的初始条件',
        points: [
          { id: 'ckt4-t2-p1', label: '状态变量' },
          { id: 'ckt4-t2-p2', label: '换路' },
          { id: 'ckt4-t2-p3', label: '换路定则' },
          { id: 'ckt4-t2-p4', label: '初始值的确定' },
        ],
        depend: ['电路元件', '电路基础知识', '基尔霍夫定律', '等效变换法', '电路变量', '电路分析方法', '节点电压法', '网孔电流法'],
      },
      {
        id: 'ckt4-t3',
        label: '一阶动态电路及其响应',
        points: [
          { id: 'ckt4-t3-p1', label: '零输入响应' },
          { id: 'ckt4-t3-p2', label: '零状态响应' },
          { id: 'ckt4-t3-p3', label: '全响应' },
          { id: 'ckt4-t3-p4', label: '时间常数' },
          { id: 'ckt4-t3-p5', label: '稳态值' },
        ],
        depend: ['戴维南定理', '替代定理', '电路定理', '叠加定理'],
      },
      {
        id: 'ckt4-t4',
        label: '一阶动态电路的三要素法',
        points: [
          { id: 'ckt4-t4-p1', label: '三要素' },
          { id: 'ckt4-t4-p2', label: '三要素法公式' },
        ],
        apply: ['计算机最大时钟速率确定', '闪光灯、闪烁灯设计'],
      },
      {
        id: 'ckt4-t5',
        label: '二阶动态电路',
        points: [
          { id: 'ckt4-t5-p1', label: '二阶动态电路的几种过渡过程' },
          { id: 'ckt4-t5-p2', label: '二阶动态电路的分析' },
        ],
      },
      {
        id: 'ckt4-t6',
        label: '阶跃响应',
        points: [
          { id: 'ckt4-t6-p1', label: '单位阶跃信号' },
          { id: 'ckt4-t6-p2', label: '阶跃响应' },
        ],
      },
      {
        id: 'ckt4-t7',
        label: '冲激响应',
        points: [
          { id: 'ckt4-t7-p1', label: '单位冲激信号' },
          { id: 'ckt4-t7-p2', label: '冲激响应' },
        ],
      },
      {
        id: 'ckt4-t8',
        label: '微分电路和积分电路',
        points: [
          { id: 'ckt4-t8-p1', label: '微分电路' },
          { id: 'ckt4-t8-p2', label: '积分电路' },
        ],
        apply: ['实验', '电火花加工'],
      },
    ],
  },
  {
    id: 'ckt5',
    label: '电路的频率特性',
    themes: [
      {
        id: 'ckt5-t1',
        label: '频率特性',
        points: [
          { id: 'ckt5-t1-p1', label: '幅频特性' },
          { id: 'ckt5-t1-p2', label: '相频特性' },
        ],
        depend: ['电路基础知识', '电路变量'],
      },
      {
        id: 'ckt5-t2',
        label: '网络函数',
        points: [
          { id: 'ckt5-t2-p1', label: '网络函数的定义' },
          { id: 'ckt5-t2-p2', label: '网络函数的类型' },
        ],
        depend: ['电路定理', '正弦稳态电路', '电路名词', '齐性定理', '正弦量及其相量', '阻抗和导纳'],
      },
      {
        id: 'ckt5-t3',
        label: 'RC电路的频率特性',
        points: [
          { id: 'ckt5-t3-p1', label: '一阶RC低通电路' },
          { id: 'ckt5-t3-p2', label: '一阶RC高通电路' },
          { id: 'ckt5-t3-p3', label: '截止频率' },
          { id: 'ckt5-t3-p4', label: '通频带' },
        ],
      },
      {
        id: 'ckt5-t4',
        label: 'RLC串联电路的谐振',
        points: [
          { id: 'ckt5-t4-p1', label: '谐振条件' },
          { id: 'ckt5-t4-p2', label: '谐振频率' },
          { id: 'ckt5-t4-p3', label: '特性阻抗' },
          { id: 'ckt5-t4-p4', label: '谐振时的电路特点' },
        ],
      },
      {
        id: 'ckt5-t5',
        label: 'RLC并联电路的谐振',
        points: [
          { id: 'ckt5-t5-p1', label: '谐振频率' },
          { id: 'ckt5-t5-p2', label: '品质因数' },
          { id: 'ckt5-t5-p3', label: '特性阻抗' },
          { id: 'ckt5-t5-p4', label: '谐振时的电路特点' },
        ],
        apply: ['无线接收机'],
      },
      {
        id: 'ckt5-t6',
        label: '谐振与选择性',
        points: [
          { id: 'ckt5-t6-p1', label: '谐振条件' },
          { id: 'ckt5-t6-p2', label: '谐振曲线' },
          { id: 'ckt5-t6-p3', label: '品质因数' },
          { id: 'ckt5-t6-p4', label: 'RC带通电路' },
        ],
        depend: ['正弦稳态电路', '电路元件VCR的相量形式', '电路定律的相量形式'],
        apply: ['按键电话', '实验'],
      },
    ],
  },
  {
    id: 'ckt6',
    label: '耦合电感电路',
    themes: [
      {
        id: 'ckt6-t1',
        label: '互感与互感电压',
        points: [
          { id: 'ckt6-t1-p1', label: '互感' },
          { id: 'ckt6-t1-p2', label: '同名端' },
          { id: 'ckt6-t1-p3', label: '互感电压' },
        ],
        prereq: ['大学物理', '时变电磁场', '电磁感应'],
        depend: ['电路基础知识', '电路元件'],
      },
      {
        id: 'ckt6-t2',
        label: '耦合电感元件',
        points: [
          { id: 'ckt6-t2-p1', label: '耦合系数' },
          { id: 'ckt6-t2-p2', label: '电压电流关系(VCR)' },
        ],
        depend: ['电路变量'],
        apply: ['实验'],
      },
      {
        id: 'ckt6-t3',
        label: '耦合电感的等效',
        points: [
          { id: 'ckt6-t3-p1', label: '去耦等效' },
          { id: 'ckt6-t3-p2', label: '受控源等效' },
          { id: 'ckt6-t3-p3', label: '功率和能量' },
        ],
        apply: ['无线充电', '无线射频识别'],
      },
      {
        id: 'ckt6-t4',
        label: '耦合电感电路的分析',
        points: [
          { id: 'ckt6-t4-p1', label: '去耦等效法' },
          { id: 'ckt6-t4-p2', label: '直接列写方程法' },
        ],
        depend: ['网孔电流法', '基尔霍夫定律', '等效变换法', '电路基础知识', '电路分析方法'],
      },
      {
        id: 'ckt6-t5',
        label: '线性变压器电路',
        points: [
          { id: 'ckt6-t5-p1', label: '线性变压器的定义' },
          { id: 'ckt6-t5-p2', label: '反映阻抗分析法' },
          { id: 'ckt6-t5-p3', label: '回路分析法' },
        ],
        depend: ['戴维南定理', '节点电压法', '电路定理', '电路分析方法', '网孔电流法'],
        apply: ['电源适配器'],
      },
      {
        id: 'ckt6-t6',
        label: '理想变压器电路',
        points: [
          { id: 'ckt6-t6-p1', label: '理想变压器的定义' },
          { id: 'ckt6-t6-p2', label: '理想变压器的VCR' },
          { id: 'ckt6-t6-p3', label: '理想变压器的变比' },
          { id: 'ckt6-t6-p4', label: '理想变压器的阻抗变换性质' },
          { id: 'ckt6-t6-p5', label: '分析方法' },
        ],
      },
      {
        id: 'ckt6-t7',
        label: '全耦合变压器',
        points: [
          { id: 'ckt6-t7-p1', label: '全耦合变压器的定义' },
          { id: 'ckt6-t7-p2', label: '全耦合变压器的VCR' },
        ],
      },
    ],
  },
  {
    id: 'ckt7',
    label: '非正弦周期稳态电路',
    themes: [
      {
        id: 'ckt7-t1',
        label: '非正弦周期信号',
        points: [
          { id: 'ckt7-t1-p1', label: '信号的分解' },
          { id: 'ckt7-t1-p2', label: '信号的平均值' },
          { id: 'ckt7-t1-p3', label: '信号的有效值' },
        ],
        prereq: ['高等数学', '傅里叶级数分解'],
      },
      {
        id: 'ckt7-t2',
        label: '非正弦周期稳态电路的分析',
        points: [{ id: 'ckt7-t2-p1', label: '正弦量及其相量' }],
        depend: ['叠加定理', '正弦稳态电路', '正弦稳态电路的分析'],
        apply: ['实验', '整流、滤波'],
      },
      {
        id: 'ckt7-t3',
        label: '非正弦周期稳态电路的功率',
        points: [],
        depend: ['叠加定理', '电路定理', '正弦稳态电路的功率'],
      },
    ],
  },
  {
    id: 'ckt8',
    label: '二端口网络',
    themes: [
      {
        id: 'ckt8-t1',
        label: '二端口网络的VCR',
        points: [
          { id: 'ckt8-t1-p1', label: 'Z参数方程' },
          { id: 'ckt8-t1-p2', label: 'Y参数方程' },
          { id: 'ckt8-t1-p3', label: 'H参数方程' },
        ],
        depend: ['电路定理', '电路变量', '基尔霍夫定律', '叠加定理'],
      },
      {
        id: 'ckt8-t2',
        label: '各参数间的关系',
        points: [{ id: 'ckt8-t2-p1', label: 'A参数方程' }],
        depend: ['电路定理', '互易定理', '正弦稳态电路', '阻抗和导纳'],
        apply: ['实验'],
      },
      {
        id: 'ckt8-t3',
        label: '二端口网络的互连',
        points: [
          { id: 'ckt8-t3-p1', label: '串联' },
          { id: 'ckt8-t3-p2', label: '并联' },
          { id: 'ckt8-t3-p3', label: '级联' },
        ],
        depend: ['电路基础知识', '电路名词'],
      },
      {
        id: 'ckt8-t4',
        label: '二端口网络的等效',
        points: [
          { id: 'ckt8-t4-p1', label: 'T型等效电路' },
          { id: 'ckt8-t4-p2', label: 'π型等效电路' },
        ],
        depend: ['等效变换法', '电路分析方法'],
      },
      {
        id: 'ckt8-t5',
        label: '端接的二端口网络',
        points: [
          { id: 'ckt8-t5-p1', label: '无端接' },
          { id: 'ckt8-t5-p2', label: '单端接' },
          { id: 'ckt8-t5-p3', label: '双端接' },
          { id: 'ckt8-t5-p4', label: '网络函数' },
        ],
        depend: ['戴维南定理', '电路定理', '电路的频率特性'],
      },
      {
        id: 'ckt8-t6',
        label: '二端口网络的特性阻抗',
        points: [
          { id: 'ckt8-t6-p1', label: '特性阻抗' },
          { id: 'ckt8-t6-p2', label: '影像阻抗' },
          { id: 'ckt8-t6-p3', label: '阻抗变换性质' },
        ],
      },
      {
        id: 'ckt8-t7',
        label: '对称二端口网络',
        points: [
          { id: 'ckt8-t7-p1', label: '定义' },
          { id: 'ckt8-t7-p2', label: '参数关系' },
        ],
      },
      {
        id: 'ckt8-t8',
        label: '互易二端口网络',
        points: [
          { id: 'ckt8-t8-p1', label: '定义' },
          { id: 'ckt8-t8-p2', label: '参数关系' },
        ],
      },
    ],
  },
]
