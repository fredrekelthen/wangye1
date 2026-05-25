<template>
  <div class="page-navigator">
    <a-card title="页面导航" style="width: 100%">
      <a-input-search
        v-model:value="searchKeyword"
        placeholder="搜索页面或路由..."
        style="margin-bottom: 16px"
        @search="onSearch"
      />
      
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="routes" tab="可用路由">
          <a-list :data-source="filteredRoutes" bordered>
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta
                  :title="item.path"
                  :description="`组件: ${item.component}`"
                >
                  <template #avatar>
                    <a-avatar :style="{ backgroundColor: '#1890ff' }">
                      <template #icon><LinkOutlined /></template>
                    </a-avatar>
                  </template>
                </a-list-item-meta>
                <template #actions>
                  <a-button type="link" @click="navigateToPage(item.path)">
                    导航
                  </a-button>
                  <a-button type="link" @click="analyzePage(item)">
                    分析
                  </a-button>
                </template>
              </a-list-item>
            </template>
          </a-list>
        </a-tab-pane>

        <a-tab-pane key="structure" tab="项目结构">
          <a-tree
            :tree-data="projectStructure"
            :selectedKeys="selectedKeys"
            @select="onTreeSelect"
          >
            <template #title="{ title, key }">
              <span>
                <FileTextOutlined v-if="key.includes('.')" />
                <FolderOutlined v-else />
                {{ title }}
              </span>
            </template>
          </a-tree>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal
      v-model:open="detailModalVisible"
      title="页面详情"
      width="800px"
      @ok="handleOk"
    >
      <a-descriptions bordered column="1">
        <a-descriptions-item label="路由路径">
          {{ currentPage?.path }}
        </a-descriptions-item>
        <a-descriptions-item label="组件名称">
          {{ currentPage?.component }}
        </a-descriptions-item>
        <a-descriptions-item label="路由名称">
          {{ currentPage?.name }}
        </a-descriptions-item>
        <a-descriptions-item label="功能描述">
          {{ currentPage?.description || '暂无描述' }}
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { LinkOutlined, FileTextOutlined, FolderOutlined } from '@ant-design/icons-vue';
import { agentApi } from '../api/agentApi';

const searchKeyword = ref('');
const activeTab = ref('routes');
const selectedKeys = ref<string[]>([]);
const detailModalVisible = ref(false);
const currentPage = ref<any>(null);

const routes = ref([
  { path: '/', name: 'home', component: 'HomeView.vue', description: '首页' },
  { path: '/course', name: 'course', component: 'CourseView.vue', description: '课程页面' },
  { path: '/user-center', name: 'user-center', component: 'UserCenter.vue', description: '用户中心' },
  { path: '/admin/material-manage', name: 'admin-material-manage', component: 'MaterialManage.vue', description: '资料管理' },
  { path: '/admin/user-manage', name: 'admin-user-manage', component: 'UserManage.vue', description: '用户管理' },
]);

const projectStructure = ref([
  {
    title: 'qianduan (前端)',
    key: 'frontend',
    children: [
      { title: 'src/views', key: 'frontend-views', children: [
        { title: 'HomeView.vue', key: 'HomeView' },
        { title: 'CourseView.vue', key: 'CourseView' },
        { title: 'UserCenter.vue', key: 'UserCenter' },
      ]},
      { title: 'src/pages', key: 'frontend-pages', children: [
        { title: 'home-page.vue', key: 'home-page' },
        { title: 'admin/MaterialManage.vue', key: 'MaterialManage' },
        { title: 'admin/UserManage.vue', key: 'UserManage' },
      ]},
      { title: 'src/components', key: 'frontend-components', children: [
        { title: 'course/', key: 'components-course' },
        { title: 'AuthModal.vue', key: 'AuthModal' },
        { title: 'GlobalHeader.vue', key: 'GlobalHeader' },
        { title: 'MaterialModal.vue', key: 'MaterialModal' },
      ]},
    ],
  },
]);

const filteredRoutes = ref(routes.value);

const onSearch = (value: string) => {
  if (!value) {
    filteredRoutes.value = routes.value;
  } else {
    filteredRoutes.value = routes.value.filter(
      (route) =>
        route.path.toLowerCase().includes(value.toLowerCase()) ||
        route.name.toLowerCase().includes(value.toLowerCase()) ||
        route.component.toLowerCase().includes(value.toLowerCase())
    );
  }
};

const navigateToPage = async (path: string) => {
  try {
    await agentApi.navigateToPage(path);
    message.success(`已导航到页面: ${path}`);
    window.open(path, '_blank');
  } catch (error) {
    message.error('导航失败');
  }
};

const analyzePage = (page: any) => {
  currentPage.value = page;
  detailModalVisible.value = true;
};

const onTreeSelect = (keys: string[]) => {
  selectedKeys.value = keys;
};

const handleOk = () => {
  detailModalVisible.value = false;
};

onMounted(async () => {
  try {
    const response = await agentApi.getRoutes();
    if (response.data) {
      routes.value = response.data;
      filteredRoutes.value = response.data;
    }
  } catch (error) {
    console.error('获取路由信息失败', error);
  }
});
</script>

<style scoped>
.page-navigator {
  padding: 20px;
}
</style>
