<template>
  <div class="user-center">
    <a-card title="个人中心" class="user-card">
      <!-- 账号信息 -->
      <a-row :gutter="16" class="stats-row">
        <a-col :span="8">
          <a-statistic title="账号名称" :value="displayAccount" />
        </a-col>
        <a-col :span="8">
          <a-statistic
            title="学习进度"
            :value="stats.learningProgress?.rate || 0"
            suffix="%"
            :precision="1"
          />
        </a-col>
        <a-col :span="8">
          <a-statistic title="学习次数" :value="stats.studyCount || 0" />
        </a-col>
      </a-row>

      <!-- 学习进度详情 -->
      <a-divider>学习进度详情</a-divider>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-statistic title="已完成小节" :value="stats.learningProgress?.completed || 0" />
        </a-col>
        <a-col :span="12">
          <a-statistic title="总小节数" :value="stats.learningProgress?.total || 0" />
        </a-col>
      </a-row>

      <!-- 学习趋势图 -->
      <a-divider>学习趋势（最近7天）</a-divider>
      <div ref="chartRef" style="width: 100%; height: 300px;"></div>

      <!-- 学习资料分布饼图 -->
      <a-divider>学习资料分布</a-divider>
      <div ref="pieChartRef" style="width: 100%; height: 300px;"></div>

      <!-- 近期查看资料 -->
      <a-divider>近期查看资料</a-divider>
      <a-list
        :data-source="stats.recentMaterials || []"
        :grid="{ gutter: 16, xs: 1, sm: 2, md: 4, lg: 4, xl: 4, xxl: 3 }"
      >
        <template #renderItem="{ item }">
          <a-list-item>
            <a-card hoverable>
              <template #cover>
                <div class="material-icon">{{ getMaterialIcon(item.type) }}</div>
              </template>
              <a-card-meta :title="item.name" :description="item.chapterTitle" />
              <template #actions>
                <span>{{ formatDate(item.studyTime) }}</span>
              </template>
            </a-card>
          </a-list-item>
        </template>
      </a-list>

      <!-- 个人成绩 -->
      <a-divider>个人成绩</a-divider>
      <a-table
        :columns="gradeColumns"
        :data-source="stats.gradeStats?.recent || []"
        :pagination="{ pageSize: 5 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'isPassed'">
            <a-tag :color="record.isPassed ? 'green' : 'red'">
              {{ record.isPassed ? '通过' : '未通过' }}
            </a-tag>
          </template>
        </template>
      </a-table>

      <!-- 成绩统计 -->
      <a-row :gutter="16" style="margin-top: 20px;">
        <a-col :span="12">
          <a-statistic title="平均分" :value="stats.gradeStats?.average || 0" :precision="1" />
        </a-col>
        <a-col :span="12">
          <a-statistic title="考试次数" :value="stats.gradeStats?.count || 0" />
        </a-col>
      </a-row>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { init, use } from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { ECharts } from 'echarts/core'
import { useUserLogin, getGlobalCurrentUser } from '@/store/useUesrLogin'

// 注册必需的组件
use([
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  LineChart,
  PieChart,
  CanvasRenderer
])

const stats = ref<any>({})
const chartRef = ref<HTMLDivElement>()
const pieChartRef = ref<HTMLDivElement>()

const userStore = useUserLogin()

const currentUser = computed(() => {
  const storeUser = userStore.loginUser.value
  if (storeUser && storeUser.id) return storeUser
  const globalUser = getGlobalCurrentUser()
  if (globalUser && globalUser.id) return globalUser
  return null
})

const displayAccount = computed(() => {
  if (stats.value.userAccount && stats.value.userAccount !== '未登录') return stats.value.userAccount
  if (currentUser.value) return currentUser.value.userAccount || currentUser.value.userName || '已登录'
  return '未登录'
})

const gradeColumns = [
  { title: '考试名称', dataIndex: 'examName', key: 'examName' },
  { title: '类型', dataIndex: 'examType', key: 'examType' },
  { title: '分数', dataIndex: 'score', key: 'score' },
  { title: '满分', dataIndex: 'fullScore', key: 'fullScore' },
  { title: '状态', dataIndex: 'isPassed', key: 'isPassed' },
]

// 获取个人中心统计数据
const fetchStats = async () => {
  try {
    const res = await fetch('http://localhost:8080/api/user-center/stats', {
      method: 'GET',
      credentials: 'include',
    })
    const data = await res.json()
    if (data.code === 0) {
      stats.value = data.data
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 获取学习进度数据
const fetchLearningProgress = async () => {
  try {
    const res = await fetch('http://localhost:8080/api/user-center/learning-progress?days=7', {
      method: 'GET',
      credentials: 'include',
    })
    const data = await res.json()
    if (data.code === 0) {
      renderChart(data.data)
      renderPieChart()
    }
  } catch (error) {
    console.error('获取学习进度失败:', error)
  }
}

// 渲染折线图
const renderChart = (data: any) => {
  nextTick(() => {
    if (!chartRef.value) return

    const chart: ECharts = init(chartRef.value)
    const option = {
      title: {
        text: '学习趋势',
        left: 'center',
      },
      tooltip: {
        trigger: 'axis',
      },
      xAxis: {
        type: 'category',
        data: data.dates || [],
        axisLabel: {
          rotate: 45,
        },
      },
      yAxis: {
        type: 'value',
        name: '学习次数',
      },
      series: [
        {
          name: '学习次数',
          type: 'line',
          data: data.counts || [],
          smooth: true,
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: '#83bff6' },
                { offset: 0.5, color: '#188df0' },
                { offset: 1, color: '#188df0' },
              ],
            },
          },
        },
      ],
    }

    chart.setOption(option)
    window.addEventListener('resize', () => chart.resize())
  })
}

// 渲染饼图（模拟数据）
const renderPieChart = () => {
  nextTick(() => {
    if (!pieChartRef.value) return

    const chart: ECharts = init(pieChartRef.value)
    const materials = stats.value.recentMaterials || []
    const materialTypes = materials.reduce((acc: any, item: any) => {
      acc[item.type] = (acc[item.type] || 0) + 1
      return acc
    }, {})

    const option = {
      title: {
        text: '学习资料类型分布',
        left: 'center',
      },
      tooltip: {
        trigger: 'item',
      },
      legend: {
        orient: 'vertical',
        left: 'left',
      },
      series: [
        {
          name: '资料类型',
          type: 'pie',
          radius: '50%',
          data: Object.entries(materialTypes).map(([name, value]) => ({
            name,
            value,
          })),
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)',
            },
          },
        },
      ],
    }

    chart.setOption(option)
    window.addEventListener('resize', () => chart.resize())
  })
}

// 获取资料图标
const getMaterialIcon = (type: string) => {
  const icons: any = {
    PDF: '📄',
    DOCX: '📝',
    视频: '🎬',
    PPT: '📊',
  }
  return icons[type] || '📄'
}

// 格式化日期
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

onMounted(() => {
  fetchStats()
  fetchLearningProgress()
})
</script>

<style scoped>
.user-center {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.user-card {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.stats-row {
  margin-bottom: 20px;
}

.material-icon {
  height: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 64px;
  background: #f5f5f5;
}
</style>
