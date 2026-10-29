/**
 * 课程知识图谱数据
 * 三级层级体系：章节(Chapter) -> 节(Section) -> 小节(Subsection)
 */

// 颜色配置
const colors = {
  root: '#1e3a5f',
  chapter: '#3b82f6',
  section: '#0d9488',
  subsection: '#6366f1'
};

// 课程知识结构定义
export const courseKnowledgeStructure = {
  chapters: [
    {
      id: 'chapter-1',
      title: '第1章 矢量分析',
      description: '矢量代数、坐标系、梯度、散度、旋度等基础理论',
      color: colors.chapter,
      sections: [
        {
          id: '1-1',
          title: '1.1 矢量代数',
          description: '标量和矢量、矢量的加减法与乘法',
          subsections: [
            { id: '1-1-1', title: '1.1.1 标量和矢量', description: '标量和矢量的定义与区别' },
            { id: '1-1-2', title: '1.1.2 矢量的加法和减法', description: '矢量的加减法运算规则' },
            { id: '1-1-3', title: '1.1.3 矢量的乘法', description: '矢量的乘法运算：标积与矢积' }
          ]
        },
        {
          id: '1-2',
          title: '1.2 三种常用的坐标系',
          description: '正交坐标系、直角坐标系、圆柱坐标系、球坐标系',
          subsections: [
            { id: '1-2-1', title: '1.2.1 正交坐标系', description: '正交坐标系的概念与性质' },
            { id: '1-2-2', title: '1.2.2 直角坐标系', description: '直角坐标系及其性质' },
            { id: '1-2-3', title: '1.2.3 圆柱坐标系', description: '圆柱坐标系及其性质' },
            { id: '1-2-4', title: '1.2.4 球坐标系', description: '球坐标系及其性质' }
          ]
        },
        {
          id: '1-3',
          title: '1.3 标量场的梯度',
          description: '标量场的等值面、方向导数、梯度',
          subsections: [
            { id: '1-3-1', title: '1.3.1 标量场的等值面', description: '等值面的概念与性质' },
            { id: '1-3-2', title: '1.3.2 方向导数', description: '方向导数的定义与计算' },
            { id: '1-3-3', title: '1.3.3 梯度', description: '梯度的定义与性质' }
          ]
        },
        {
          id: '1-4',
          title: '1.4 矢量场的通量与散度',
          description: '通量、散度、散度定理',
          subsections: [
            { id: '1-4-1', title: '1.4.1 通量', description: '通量的概念与计算' },
            { id: '1-4-2', title: '1.4.2 散度', description: '散度的定义与性质' },
            { id: '1-4-3', title: '1.4.3 散度定理', description: '散度定理及其应用' }
          ]
        },
        {
          id: '1-5',
          title: '1.5 矢量场的环量与旋度',
          description: '环量、旋度、斯托克斯定理',
          subsections: [
            { id: '1-5-1', title: '1.5.1 环量', description: '环量的概念与计算' },
            { id: '1-5-2', title: '1.5.2 旋度', description: '旋度的定义与性质' },
            { id: '1-5-3', title: '1.5.3 斯托克斯定理', description: '斯托克斯定理及其应用' }
          ]
        },
        {
          id: '1-6',
          title: '1.6 无旋场与无散场',
          description: '无旋场与无散场的定义与特性',
          subsections: [
            { id: '1-6-1', title: '1.6.1 无旋场', description: '无旋场的定义与性质' },
            { id: '1-6-2', title: '1.6.2 无散场', description: '无散场的定义与性质' }
          ]
        },
        {
          id: '1-7',
          title: '1.7 拉普拉斯运算与格林定理',
          description: '拉普拉斯运算、格林定理',
          subsections: [
            { id: '1-7-1', title: '1.7.1 拉普拉斯运算', description: '拉普拉斯运算的定义与性质' },
            { id: '1-7-2', title: '1.7.2 格林定理', description: '格林定理及其应用' }
          ]
        },
        {
          id: '1-8',
          title: '1.8 亥姆霍兹定理',
          description: '矢量场的分解定理',
          subsections: []
        },
        {
          id: '1-9',
          title: '1.9 冲激函数及其性质',
          description: '冲激函数的定义与性质',
          subsections: []
        }
      ]
    },
    {
      id: 'chapter-2',
      title: '第2章 静电场与恒定电场',
      description: '库仑定律、电场强度、电位函数、导体与介质',
      color: colors.chapter,
      sections: [
        {
          id: '2-1',
          title: '2.1 库仑定律与电场强度',
          description: '库仑定律、电场强度及其叠加原理',
          subsections: [
            { id: '2-1-1', title: '2.1.1 库仑定律', description: '库仑定律的内容与应用' },
            { id: '2-1-2', title: '2.1.2 电场强度及其叠加原理', description: '电场强度的定义与叠加原理' }
          ]
        },
        {
          id: '2-2',
          title: '2.2 电场强度的通量和散度',
          description: '电场强度的通量与散度',
          subsections: [
            { id: '2-2-1', title: '2.2.1 电场强度的通量', description: '电场强度通量的计算' },
            { id: '2-2-2', title: '2.2.2 电场强度的散度', description: '电场强度散度的计算与性质' }
          ]
        },
        {
          id: '2-3',
          title: '2.3 电场强度的环量及旋度',
          description: '电场强度的环量与旋度',
          subsections: [
            { id: '2-3-1', title: '2.3.1 电场强度的环量', description: '电场强度环量的计算' },
            { id: '2-3-2', title: '2.3.2 电场强度的旋度', description: '电场强度旋度的计算与性质' }
          ]
        },
        {
          id: '2-4',
          title: '2.4 静电场的电位函数',
          description: '电场强度与电位函数、电位函数的表达式',
          subsections: [
            { id: '2-4-1', title: '2.4.1 电场强度与电位函数', description: '电场强度与电位函数的关系' },
            { id: '2-4-2', title: '2.4.2 电位函数的表达式', description: '电位函数的表达式推导' }
          ]
        },
        {
          id: '2-5',
          title: '2.5 电偶极子',
          description: '电偶极子的电位函数、电场强度、等位面和电场线',
          subsections: [
            { id: '2-5-1', title: '2.5.1 电偶极子的电位函数', description: '电偶极子电位函数的推导' },
            { id: '2-5-2', title: '2.5.2 电偶极子静电场的电场强度', description: '电偶极子电场强度的计算' },
            { id: '2-5-3', title: '2.5.3 电偶极子静电场的等位面和电场线', description: '等位面与电场线的性质' }
          ]
        },
        {
          id: '2-6',
          title: '2.6 静电场中的导体和介质',
          description: '静电场中的导体、介质、电位移矢量',
          subsections: [
            { id: '2-6-1', title: '2.6.1 静电场中的导体', description: '导体在静电场中的性质' },
            { id: '2-6-2', title: '2.6.2 静电场中的介质', description: '介质在静电场中的性质' },
            { id: '2-6-3', title: '2.6.3 介质中电位移矢量的通量和散度', description: '电位移矢量的性质' },
            { id: '2-6-4', title: '2.6.4 电位移矢量与电场强度的关系', description: 'D与E的关系' }
          ]
        },
        {
          id: '2-7',
          title: '2.7 泊松方程与拉普拉斯方程',
          description: '电位方程的建立与求解',
          subsections: []
        },
        {
          id: '2-8',
          title: '2.8 静电场的边界条件',
          description: '电位移矢量、电场强度、电位函数的边界条件',
          subsections: [
            { id: '2-8-1', title: '2.8.1 电位移矢量的法向边界条件', description: 'D的法向边界条件' },
            { id: '2-8-2', title: '2.8.2 电场强度的切向边界条件', description: 'E的切向边界条件' },
            { id: '2-8-3', title: '2.8.3 电位函数的边界条件', description: '电位的边界条件' }
          ]
        },
        {
          id: '2-9',
          title: '2.9 导体系统的电容',
          description: '双导体及孤立导体的电容、多导体的电容系数与部分电容',
          subsections: [
            { id: '2-9-1', title: '2.9.1 双导体及孤立导体的电容', description: '电容的计算方法' },
            { id: '2-9-2', title: '2.9.2 多导体的电容系数与部分电容', description: '多导体系统的电容' }
          ]
        },
        {
          id: '2-10',
          title: '2.10 静电场的能量与静电力',
          description: '静电场的能量、能量密度、静电力',
          subsections: [
            { id: '2-10-1', title: '2.10.1 静电场的能量', description: '静电场能量的计算' },
            { id: '2-10-2', title: '2.10.2 静电场的能量密度', description: '能量密度的定义与计算' },
            { id: '2-10-3', title: '2.10.3 静电力', description: '静电力的计算' }
          ]
        },
        {
          id: '2-11',
          title: '2.11 恒定电场',
          description: '电流密度矢量、恒定电场性质、边界条件、比拟法',
          subsections: [
            { id: '2-11-1', title: '2.11.1 电流与电流密度矢量', description: '电流密度矢量的定义' },
            { id: '2-11-2', title: '2.11.2 恒定电场的基本性质', description: '恒定电场的基本方程' },
            { id: '2-11-3', title: '2.11.3 恒定电场的边界条件', description: '恒定电场的边界条件' },
            { id: '2-11-4', title: '2.11.4 静电场比拟法与电导', description: '比拟法的应用' },
            { id: '2-11-5', title: '2.11.5 损耗功率与焦耳定律', description: '焦耳定律的应用' }
          ]
        }
      ]
    },
    {
      id: 'chapter-3',
      title: '第3章 恒定磁场',
      description: '安培定律、毕奥-萨伐尔定律、磁偶极子、电感',
      color: colors.chapter,
      sections: [
        {
          id: '3-1',
          title: '3.1 恒定磁场的基本定律',
          description: '安培力定律、毕奥-萨伐尔定律',
          subsections: [
            { id: '3-1-1', title: '3.1.1 安培力定律', description: '安培力定律的内容' },
            { id: '3-1-2', title: '3.1.2 毕奥-萨伐尔定律', description: '毕奥-萨伐尔定律的内容与应用' }
          ]
        },
        {
          id: '3-2',
          title: '3.2 真空中的恒定磁场方程',
          description: '散度、磁通连续性、矢量磁位、旋度、安培环路定理、标量磁位',
          subsections: [
            { id: '3-2-1', title: '3.2.1 恒定磁场的散度及磁通连续性原理', description: '磁通连续性原理' },
            { id: '3-2-2', title: '3.2.2 矢量磁位及其方程', description: '矢量磁位的定义与方程' },
            { id: '3-2-3', title: '3.2.3 恒定磁场的旋度及安培环路定理', description: '安培环路定理' },
            { id: '3-2-4', title: '3.2.4 标量磁位', description: '标量磁位的定义与应用' }
          ]
        },
        {
          id: '3-3',
          title: '3.3 磁偶极子与介质的磁化',
          description: '磁偶极子及其矢量磁位、介质的磁化、介质中的恒定磁场方程',
          subsections: [
            { id: '3-3-1', title: '3.3.1 磁偶极子及其矢量磁位', description: '磁偶极子与矢量磁位' },
            { id: '3-3-2', title: '3.3.2 介质的磁化', description: '介质磁化的机理' },
            { id: '3-3-3', title: '3.3.3 介质中的恒定磁场方程', description: '介质中的磁场方程' }
          ]
        },
        {
          id: '3-4',
          title: '3.4 恒定磁场的边界条件',
          description: '磁感应强度、磁场强度的边界条件',
          subsections: [
            { id: '3-4-1', title: '3.4.1 磁感应强度的法向边界条件', description: 'B的法向边界条件' },
            { id: '3-4-2', title: '3.4.2 磁场强度的切向边界条件', description: 'H的切向边界条件' },
            { id: '3-4-3', title: '3.4.3 恒定磁场位函数的边界条件', description: '位函数的边界条件' }
          ]
        },
        {
          id: '3-5',
          title: '3.5 电感',
          description: '自电感、互电感、电感的计算',
          subsections: [
            { id: '3-5-1', title: '3.5.1 自电感', description: '自电感的定义与计算' },
            { id: '3-5-2', title: '3.5.2 互电感', description: '互电感的定义与计算' },
            { id: '3-5-3', title: '3.5.3 电感的计算', description: '电感计算的方法' }
          ]
        },
        {
          id: '3-6',
          title: '3.6 恒定磁场的能量和磁场力',
          description: '恒定磁场的能量及能量密度、磁场力',
          subsections: [
            { id: '3-6-1', title: '3.6.1 恒定磁场的能量及能量密度', description: '磁场能量与能量密度' },
            { id: '3-6-2', title: '3.6.2 恒定磁场的磁场力', description: '磁场力的计算' }
          ]
        }
      ]
    },
    {
      id: 'chapter-4',
      title: '第4章 静态场的边值问题及其解法',
      description: '边值问题、分离变量法、镜像法、有限差分法',
      color: colors.chapter,
      sections: [
        {
          id: '4-1',
          title: '4.1 边值问题的类型及唯一性定理',
          description: '边值问题的分类、静电场解的唯一性定理',
          subsections: [
            { id: '4-1-1', title: '4.1.1 边值问题的分类', description: '边值问题的类型' },
            { id: '4-1-2', title: '4.1.2 静电场解的唯一性定理', description: '唯一性定理的内容' }
          ]
        },
        {
          id: '4-2',
          title: '4.2 分离变量法',
          description: '直角坐标系、圆柱坐标系、球坐标系中的分离变量法',
          subsections: [
            { id: '4-2-1', title: '4.2.1 直角坐标系中的分离变量法', description: '直角坐标系分离变量' },
            { id: '4-2-2', title: '4.2.2 圆柱坐标系中的分离变量法', description: '圆柱坐标系分离变量' },
            { id: '4-2-3', title: '4.2.3 球坐标系中的分离变量法', description: '球坐标系分离变量' }
          ]
        },
        {
          id: '4-3',
          title: '4.3 镜像法',
          description: '平面镜像、球面镜像与柱面镜像',
          subsections: [
            { id: '4-3-1', title: '4.3.1 平面镜像', description: '平面镜像法' },
            { id: '4-3-2', title: '4.3.2 球面镜像与柱面镜像', description: '球面与柱面镜像法' }
          ]
        },
        {
          id: '4-4',
          title: '4.4 有限差分法',
          description: '有限差分法基本原理与基本实现方法',
          subsections: [
            { id: '4-4-1', title: '4.4.1 有限差分法基本原理', description: '差分法的基本原理' },
            { id: '4-4-2', title: '4.4.2 有限差分法的基本实现方法', description: '差分法的实现' }
          ]
        }
      ]
    },
    {
      id: 'chapter-5',
      title: '第5章 时变电磁场',
      description: '麦克斯韦方程组、边界条件、时谐电磁场、坡印亭定理',
      color: colors.chapter,
      sections: [
        {
          id: '5-1',
          title: '5.1 麦克斯韦方程组',
          description: '麦克斯韦四个方程与本构方程',
          subsections: [
            { id: '5-1-1', title: '5.1.1 麦克斯韦第一方程', description: '第一方程内容' },
            { id: '5-1-2', title: '5.1.2 麦克斯韦第二方程', description: '第二方程内容' },
            { id: '5-1-3', title: '5.1.3 麦克斯韦第三方程', description: '第三方程内容' },
            { id: '5-1-4', title: '5.1.4 麦克斯韦第四方程', description: '第四方程内容' },
            { id: '5-1-5', title: '5.1.5 麦克斯韦方程组的形式', description: '方程组的形式' },
            { id: '5-1-6', title: '5.1.6 媒质的本构方程', description: '本构方程' }
          ]
        },
        {
          id: '5-2',
          title: '5.2 时变电磁场的边界条件',
          description: '法向场与切向场的边界条件',
          subsections: [
            { id: '5-2-1', title: '5.2.1 法向场的边界条件', description: '法向边界条件' },
            { id: '5-2-2', title: '5.2.2 切向场的边界条件', description: '切向边界条件' }
          ]
        },
        {
          id: '5-3',
          title: '5.3 时谐电磁场及麦克斯韦方程组的复数形式',
          description: '时谐电磁场的复数形式、麦克斯韦方程组的复数形式',
          subsections: [
            { id: '5-3-1', title: '5.3.1 时谐电磁场的复数形式', description: '复数形式表示' },
            { id: '5-3-2', title: '5.3.2 麦克斯韦方程组的复数形式', description: '复数形式的方程组' }
          ]
        },
        {
          id: '5-4',
          title: '5.4 时变电磁场的能量及功率',
          description: '坡印亭定理、复坡印亭矢量、平均坡印亭矢量、功率',
          subsections: [
            { id: '5-4-1', title: '5.4.1 坡印亭定理', description: '坡印亭定理内容' },
            { id: '5-4-2', title: '5.4.2 复坡印亭矢量及平均坡印亭矢量', description: '坡印亭矢量' },
            { id: '5-4-3', title: '5.4.3 功率', description: '功率的计算' }
          ]
        },
        {
          id: '5-5',
          title: '5.5 时变电磁场的唯一性定理',
          description: '时变电磁场的唯一性定理',
          subsections: []
        },
        {
          id: '5-6',
          title: '5.6 电磁场的位函数及波动方程',
          description: '电磁场的位函数与波动方程',
          subsections: []
        }
      ]
    },
    {
      id: 'chapter-6',
      title: '第6章 无界媒质中的均匀平面波',
      description: '均匀平面波、极化、导电媒质、趋肤效应',
      color: colors.chapter,
      sections: [
        {
          id: '6-1',
          title: '6.1 理想介质中的均匀平面波',
          description: '亥姆霍兹方程与均匀平面波、波的特性与一般表达式',
          subsections: [
            { id: '6-1-1', title: '6.1.1 亥姆霍兹方程与均匀平面波', description: '亥姆霍兹方程' },
            { id: '6-1-2', title: '6.1.2 理想介质中均匀平面波的特性', description: '平面波特性' },
            { id: '6-1-3', title: '6.1.3 理想介质中均匀平面波的一般表达式', description: '一般表达式' }
          ]
        },
        {
          id: '6-2',
          title: '6.2 电磁波的极化',
          description: '线极化、圆极化、椭圆极化、极化波的合成与分解',
          subsections: [
            { id: '6-2-1', title: '6.2.1 线极化', description: '线极化的特性' },
            { id: '6-2-2', title: '6.2.2 圆极化', description: '圆极化的特性' },
            { id: '6-2-3', title: '6.2.3 椭圆极化', description: '椭圆极化的特性' },
            { id: '6-2-4', title: '6.2.4 极化波的合成与分解', description: '极化波的合成分解' },
            { id: '6-2-5', title: '6.2.5 电磁波的极化与天线的极化方式', description: '天线极化方式' }
          ]
        },
        {
          id: '6-3',
          title: '6.3 导电媒质中的均匀平面波',
          description: '导电媒质中的波动方程、波的特性、趋肤效应、表面阻抗',
          subsections: [
            { id: '6-3-1', title: '6.3.1 导电媒质中的波动方程与均匀平面波', description: '导电媒质中的波动方程' },
            { id: '6-3-2', title: '6.3.2 导电媒质中均匀平面波的特性', description: '导电媒质中波的特性' },
            { id: '6-3-3', title: '6.3.3 良介质与良导体', description: '良介质与良导体的特性' },
            { id: '6-3-4', title: '6.3.4 趋肤效应', description: '趋肤效应原理' },
            { id: '6-3-5', title: '6.3.5 表面阻抗、交流电阻', description: '表面阻抗的概念' },
            { id: '6-3-6', title: '6.3.6 损耗功率', description: '损耗功率的计算' }
          ]
        },
        {
          id: '6-4',
          title: '6.4 时域有限差分法',
          description: '麦克斯韦方程的差分格式、UPML吸收边界条件',
          subsections: [
            { id: '6-4-1', title: '6.4.1 麦克斯韦方程的差分格式', description: '差分格式推导' },
            { id: '6-4-2', title: '6.4.2 UPML吸收边界条件', description: 'UPML边界条件' }
          ]
        }
      ]
    },
    {
      id: 'chapter-7',
      title: '第7章 均匀平面波在不同媒质分界面的反射与折射',
      description: '波的反射、折射、全反射、人工电磁材料',
      color: colors.chapter,
      sections: [
        {
          id: '7-1',
          title: '7.1 平面波垂直入射到理想导体表面',
          description: '平面波垂直入射到理想导体表面的反射',
          subsections: []
        },
        {
          id: '7-2',
          title: '7.2 平面波垂直入射到理想介质间的分界面',
          description: '平面波垂直入射到理想介质分界面的反射与折射',
          subsections: []
        },
        {
          id: '7-3',
          title: '7.3 平面波斜入射到理想导体表面',
          description: '垂直极化波与平行极化波斜入射到理想导体',
          subsections: [
            { id: '7-3-1', title: '7.3.1 垂直极化波斜入射', description: '垂直极化波斜入射' },
            { id: '7-3-2', title: '7.3.2 平行极化波斜入射', description: '平行极化波斜入射' }
          ]
        },
        {
          id: '7-4',
          title: '7.4 平面波斜入射到理想介质间的分界面',
          description: '平行极化波与垂直极化波斜入射、全折射、全反射与表面波',
          subsections: [
            { id: '7-4-1', title: '7.4.1 平行极化波斜入射', description: '平行极化波斜入射' },
            { id: '7-4-2', title: '7.4.2 垂直极化波斜入射', description: '垂直极化波斜入射' },
            { id: '7-4-3', title: '7.4.3 全折射、全反射与表面波', description: '全折射、全反射与表面波' }
          ]
        },
        {
          id: '7-5',
          title: '7.5 平面波在导电媒质分界面的反射与折射',
          description: '平面波在导电媒质分界面的反射与折射',
          subsections: []
        },
        {
          id: '7-6',
          title: '7.6 平面波在多层媒质分界面的垂直入射',
          description: '平面波在多层媒质分界面的垂直入射',
          subsections: []
        },
        {
          id: '7-7',
          title: '7.7 人工电磁材料',
          description: '负折射效应、完美透镜、负相速度、逆多普勒频移、完美吸波材料',
          subsections: [
            { id: '7-7-1', title: '7.7.1 负折射效应', description: '负折射效应' },
            { id: '7-7-2', title: '7.7.2 完美透镜效应', description: '完美透镜效应' },
            { id: '7-7-3', title: '7.7.3 负相速度', description: '负相速度' },
            { id: '7-7-4', title: '7.7.4 逆多普勒频移', description: '逆多普勒频移' },
            { id: '7-7-5', title: '7.7.5 逆切伦科夫辐射', description: '逆切伦科夫辐射' },
            { id: '7-7-6', title: '7.7.6 完美吸波材料', description: '完美吸波材料' }
          ]
        }
      ]
    },
    {
      id: 'chapter-8',
      title: '第8章 导行电磁波',
      description: 'TEM、TM、TE波、传输线、波导、谐振腔',
      color: colors.chapter,
      sections: [
        {
          id: '8-1',
          title: '8.1 导行电磁波传播模式及其传播特性',
          description: 'TEM波、TM波、TE波的传播模式与特性',
          subsections: [
            { id: '8-1-1', title: '8.1.1 TEM波', description: 'TEM波的特性' },
            { id: '8-1-2', title: '8.1.2 TM波', description: 'TM波的特性' },
            { id: '8-1-3', title: '8.1.3 TE波', description: 'TE波的特性' }
          ]
        },
        {
          id: '8-2',
          title: '8.2 双导体传输线',
          description: '平行双线传输系统、同轴传输线、微带线',
          subsections: [
            { id: '8-2-1', title: '8.2.1 平行双线传输系统', description: '平行双线传输线' },
            { id: '8-2-2', title: '8.2.2 同轴传输线', description: '同轴传输线' },
            { id: '8-2-3', title: '8.2.3 微带线', description: '微带线' }
          ]
        },
        {
          id: '8-3',
          title: '8.3 矩形波导',
          description: '矩形波导中的TM波与TE波、简并模、主模及单模传输',
          subsections: [
            { id: '8-3-1', title: '8.3.1 矩形波导中的TM波', description: '矩形波导TM波' },
            { id: '8-3-2', title: '8.3.2 矩形波导中的TE波', description: '矩形波导TE波' },
            { id: '8-3-3', title: '8.3.3 简并模、主模及单模传输', description: '简并模与主模' },
            { id: '8-3-4', title: '8.3.4 矩形波导的传播特性参数及传输功率', description: '传播特性参数' }
          ]
        },
        {
          id: '8-4',
          title: '8.4 圆波导',
          description: '圆波导中的TM波与TE波、传播特性、主要波形',
          subsections: [
            { id: '8-4-1', title: '8.4.1 圆波导中的TM波', description: '圆波导TM波' },
            { id: '8-4-2', title: '8.4.2 圆波导中的TE波', description: '圆波导TE波' },
            { id: '8-4-3', title: '8.4.3 圆波导的传播特性', description: '圆波导传播特性' },
            { id: '8-4-4', title: '8.4.4 圆波导的几种主要波形', description: '圆波导主要波形' }
          ]
        },
        {
          id: '8-5',
          title: '8.5 谐振腔',
          description: '谐振腔的基本参数、矩形谐振腔、圆谐振腔',
          subsections: [
            { id: '8-5-1', title: '8.5.1 谐振腔的基本参数', description: '谐振腔参数' },
            { id: '8-5-2', title: '8.5.2 矩形谐振腔', description: '矩形谐振腔' },
            { id: '8-5-3', title: '8.5.3 圆谐振腔', description: '圆谐振腔' }
          ]
        },
        {
          id: '8-6',
          title: '8.6 基片集成波导',
          description: '基片集成波导原理与应用',
          subsections: []
        }
      ]
    },
    {
      id: 'chapter-9',
      title: '第9章 电磁辐射',
      description: '滞后位、电偶极子辐射、磁偶极子辐射、天线理论',
      color: colors.chapter,
      sections: [
        {
          id: '9-1',
          title: '9.1 滞后位',
          description: '滞后位的概念与性质',
          subsections: []
        },
        {
          id: '9-2',
          title: '9.2 电偶极子的辐射',
          description: '电偶极子电磁场的激发与辐射、辐射场',
          subsections: [
            { id: '9-2-1', title: '9.2.1 电偶极子电磁场的激发与辐射', description: '电偶极子激发与辐射' },
            { id: '9-2-2', title: '9.2.2 电偶极子的辐射场', description: '电偶极子辐射场' }
          ]
        },
        {
          id: '9-3',
          title: '9.3 磁偶极子的辐射',
          description: '磁偶极子的辐射场',
          subsections: []
        },
        {
          id: '9-4',
          title: '9.4 电与磁的对偶原理',
          description: '电磁对偶原理',
          subsections: []
        },
        {
          id: '9-5',
          title: '9.5 对称振子天线',
          description: '对称振子天线上的电流分布、远区场',
          subsections: [
            { id: '9-5-1', title: '9.5.1 对称振子天线上的电流分布', description: '电流分布' },
            { id: '9-5-2', title: '9.5.2 对称振子天线的远区场', description: '远区场特性' }
          ]
        },
        {
          id: '9-6',
          title: '9.6 天线的基本参数',
          description: '方向性函数、方向图、增益、阻抗、极化、效率、波瓣宽度等参数',
          subsections: [
            { id: '9-6-1', title: '9.6.1 方向性函数、方向图与方向性系数', description: '方向性参数' },
            { id: '9-6-2', title: '9.6.2 输入阻抗与驻波比', description: '阻抗参数' },
            { id: '9-6-3', title: '9.6.3 极化', description: '极化参数' },
            { id: '9-6-4', title: '9.6.4 效率', description: '效率参数' },
            { id: '9-6-5', title: '9.6.5 增益', description: '增益参数' },
            { id: '9-6-6', title: '9.6.6 波瓣宽度', description: '波瓣宽度' },
            { id: '9-6-7', title: '9.6.7 前后比和副瓣电平', description: '前后比与副瓣' },
            { id: '9-6-8', title: '9.6.8 有效长度与频带宽度', description: '有效长度与带宽' }
          ]
        }
      ]
    }
  ]
};

// 将课程数据转换为知识图谱格式
export function convertToKnowledgeGraph(chapterId?: number): any[] | null {
  if (chapterId !== undefined) {
    // 生成单个章节的知识图谱
    const chapter = courseKnowledgeStructure.chapters.find(ch => ch.id === `chapter-${chapterId}`);
    if (!chapter) return null;

    const nodes: any[] = [
      {
        id: 'root',
        data: { label: chapter.title, description: chapter.description, color: chapter.color, level: 0 }
      }
    ];

    // 遍历sections
    chapter.sections.forEach((section, sectionIndex) => {
      nodes.push({
        id: section.id,
        data: {
          label: section.title,
          description: section.description,
          color: colors.section,
          level: 1,
          chapter: chapter.title,
          section: section.title
        }
      });

      // 添加section到root的边
      nodes.push({
        id: `edge-section-${sectionIndex}`,
        source: 'root',
        target: section.id
      });

      // 遍历subsections
      section.subsections.forEach((subsection, subsectionIndex) => {
        nodes.push({
          id: subsection.id,
          data: {
            label: subsection.title,
            description: subsection.description,
            color: colors.subsection,
            level: 2,
            chapter: chapter.title,
            section: section.title,
            subsection: subsection.title
          }
        });

        // 添加subsection到section的边
        nodes.push({
          id: `edge-subsection-${sectionIndex}-${subsectionIndex}`,
          source: section.id,
          target: subsection.id
        });
      });
    });

    return nodes;
  } else {
    // 生成整个课程的知识图谱
    const nodes: any[] = [
      {
        id: 'root',
        data: { label: '电磁场与电磁波', description: '课程总览', color: colors.root, level: 0 }
      }
    ];

    courseKnowledgeStructure.chapters.forEach((chapter, chapterIndex) => {
      nodes.push({
        id: chapter.id,
        data: {
          label: chapter.title,
          description: chapter.description,
          color: chapter.color,
          level: 1,
          sectionCount: chapter.sections.length
        }
      });

      // 添加章节到根的边
      nodes.push({
        id: `edge-chapter-${chapterIndex}`,
        source: 'root',
        target: chapter.id
      });

      // 为每个章节添加section节点
      chapter.sections.forEach((section, sectionIndex) => {
        nodes.push({
          id: section.id,
          data: {
            label: section.title,
            description: section.description,
            color: colors.section,
            level: 2,
            chapter: chapter.title,
            section: section.title
          }
        });

        // 添加section到chapter的边
        nodes.push({
          id: `edge-section-${chapterIndex}-${sectionIndex}`,
          source: chapter.id,
          target: section.id
        });

        // 添加subsection节点
        section.subsections.forEach((subsection, subsectionIndex) => {
          nodes.push({
            id: subsection.id,
            data: {
              label: subsection.title,
              description: subsection.description,
              color: colors.subsection,
              level: 3,
              chapter: chapter.title,
              section: section.title,
              subsection: subsection.title
            }
          });

          // 添加subsection到section的边
          nodes.push({
            id: `edge-subsection-${chapterIndex}-${sectionIndex}-${subsectionIndex}`,
            source: section.id,
            target: subsection.id
          });
        });
      });
    });

    return nodes;
  }
}
