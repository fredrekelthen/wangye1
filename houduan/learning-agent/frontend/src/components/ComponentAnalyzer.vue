<template>
  <div class="component-analyzer">
    <a-card title="组件分析器" style="width: 100%">
      <a-space direction="vertical" style="width: 100%">
        <a-input-search
          v-model:value="searchPath"
          placeholder="输入组件路径，例如：components/course/CourseContent.vue"
          enter-button="分析"
          size="large"
          @search="analyzeComponent"
        />

        <a-tabs v-model:activeKey="activeTab">
          <a-tab-pane key="list" tab="组件列表">
            <a-list :data-source="components" bordered>
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta
                    :title="item.name"
                    :description="item.path"
                  >
                    <template #avatar>
                      <a-avatar :style="{ backgroundColor: '#52c41a' }">
                        <template #icon><FileTextOutlined /></template>
                      </a-avatar>
                    </template>
                  </a-list-item-meta>
                  <template #actions>
                    <a-button type="link" @click="openComponent(item.path)">
                      打开
                    </a-button>
                    <a-button type="link" @click="analyzeComponentByItem(item)">
                      分析
                    </a-button>
                  </template>
                </a-list-item>
              </template>
            </a-list>
          </a-tab-pane>

          <a-tab-pane key="analysis" tab="分析结果" v-if="analysisResult">
            <a-card title="组件详情">
              <a-descriptions bordered column="1">
                <a-descriptions-item label="文件路径">
                  {{ analysisResult.path }}
                </a-descriptions-item>
                <a-descriptions-item label="组件类型">
                  {{ analysisResult.type }}
                </a-descriptions-item>
                <a-descriptions-item label="行数">
                  {{ analysisResult.lines }}
                </a-descriptions-item>
              </a-descriptions>
              
              <a-divider>代码预览</a-divider>
              <pre>{{ analysisResult.code || '暂无代码预览' }}</pre>
              
              <a-divider>功能说明</a-divider>
              <div>{{ analysisResult.description || '暂无说明' }}</div>
              
              <a-divider>依赖关系</a-divider>
              <a-tag v-for="dep in analysisResult.dependencies" :key="dep" color="blue">
                {{ dep }}
              </a-tag>
            </a-card>
          </a-tab-pane>
        </a-tabs>
      </a-space>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { message } from 'ant-design-vue';
import { FileTextOutlined } from '@ant-design/icons-vue';
import { agentApi } from '../api/agentApi';

const searchPath = ref('');
const activeTab = ref('list');
const analysisResult = ref<any>(null);

const components = ref([
  { name: 'CourseContent', path: 'components/course/CourseContent.vue' },
  { name: 'CourseDiscuss', path: 'components/course/CourseDiscuss.vue' },
  { name: 'CourseGrade', path: 'components/course/CourseGrade.vue' },
  { name: 'CourseGroup', path: 'components/course/CourseGroup.vue' },
  { name: 'CourseHeader', path: 'components/course/CourseHeader.vue' },
  { name: 'CourseNotice', path: 'components/course/CourseNotice.vue' },
  { name: 'CourseTabs', path: 'components/course/CourseTabs.vue' },
  { name: 'CourseTree', path: 'components/course/CourseTree.vue' },
  { name: 'KnowledgeGraph', path: 'components/course/KnowledgeGraph.vue' },
  { name: 'AuthModal', path: 'components/AuthModal.vue' },
  { name: 'GlobalHeader', path: 'components/GlobalHeader.vue' },
  { name: 'MaterialModal', path: 'components/MaterialModal.vue' },
  { name: 'HomeView', path: 'views/HomeView.vue' },
  { name: 'CourseView', path: 'views/CourseView.vue' },
  { name: 'UserCenter', path: 'views/UserCenter.vue' },
  { name: 'MaterialManage', path: 'pages/admin/MaterialManage.vue' },
  { name: 'UserManage', path: 'pages/admin/UserManage.vue' },
]);

const analyzeComponent = async (path?: string) => {
  const targetPath = path || searchPath.value;
  if (!targetPath) {
    message.warning('请输入组件路径');
    return;
  }
  
  try {
    const response = await agentApi.analyzeComponent(targetPath);
    analysisResult.value = response.data;
    activeTab.value = 'analysis';
    message.success('分析完成');
  } catch (error) {
    message.error('分析失败');
  }
};

const analyzeComponentByItem = (item: any) => {
  searchPath.value = item.path;
  analyzeComponent(item.path);
};

const openComponent = async (path: string) => {
  try {
    await agentApi.openComponent(path);
    message.success(`已在编辑器中打开: ${path}`);
  } catch (error) {
    message.error('打开失败');
  }
};
</script>

<style scoped>
.component-analyzer {
  padding: 20px;
}

pre {
  background: #f5f5f5;
  padding: 16px;
  border-radius: 4px;
  overflow-x: auto;
  max-height: 400px;
}
</style>
