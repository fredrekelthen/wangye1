<template>
  <div class="database-explorer">
    <a-card title="数据库分析" style="width: 100%">
      <a-space direction="vertical" style="width: 100%">
        <a-space>
          <a-button type="primary" @click="loadCollections">
            加载集合列表
          </a-button>
          <a-button @click="loadSchema">
            查看数据库架构
          </a-button>
        </a-space>

        <a-tabs v-model:activeKey="activeTab">
          <a-tab-pane key="collections" tab="集合列表">
            <a-list :data-source="collections" bordered>
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta
                    :title="item.name"
                    :description="`文档数: ${item.count}`"
                  >
                    <template #avatar>
                      <a-avatar :style="{ backgroundColor: '#faad14' }">
                        <template #icon><DatabaseOutlined /></template>
                      </a-avatar>
                    </template>
                  </a-list-item-meta>
                  <template #actions>
                    <a-button type="link" @click="viewCollection(item.name)">
                      查看
                    </a-button>
                    <a-button type="link" @click="analyzeCollection(item.name)">
                      分析
                    </a-button>
                  </template>
                </a-list-item>
              </template>
            </a-list>
          </a-tab-pane>

          <a-tab-pane key="schema" tab="数据库架构" v-if="schema">
            <a-card title="MongoDB数据库架构">
              <a-descriptions bordered column="1">
                <a-descriptions-item label="数据库类型">
                  MongoDB
                </a-descriptions-item>
                <a-descriptions-item label="连接地址">
                  {{ schema.uri }}
                </a-descriptions-item>
                <a-descriptions-item label="集合数量">
                  {{ schema.collections?.length || 0 }}
                </a-descriptions-item>
              </a-descriptions>

              <a-divider>集合详情</a-divider>
              <a-list :data-source="schema.collections" bordered size="small">
                <template #renderItem="{ item }">
                  <a-list-item>
                    <strong>{{ item.name }}</strong>
                    <span style="margin-left: 16px; color: #888;">
                      字段: {{ item.fields?.join(', ') }}
                    </span>
                  </a-list-item>
                </template>
              </a-list>
            </a-card>
          </a-tab-pane>

          <a-tab-pane key="query" tab="数据查询">
            <a-textarea
              v-model:value="queryInput"
              placeholder="输入查询语句，例如：db.users.find({})"
              :rows="6"
              style="margin-bottom: 16px"
            />
            <a-space>
              <a-button type="primary" @click="executeQuery">
                执行查询
              </a-button>
              <a-button @click="clearQuery">
                清空
              </a-button>
            </a-space>

            <a-divider>查询结果</a-divider>
            <pre v-if="queryResult">{{ JSON.stringify(queryResult, null, 2) }}</pre>
            <a-empty v-else description="暂无查询结果" />
          </a-tab-pane>

          <a-tab-pane key="analysis" tab="分析报告" v-if="analysisReport">
            <a-card>
              <div v-html="analysisReport"></div>
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
import { DatabaseOutlined } from '@ant-design/icons-vue';
import { agentApi } from '../api/agentApi';

const activeTab = ref('collections');
const collections = ref([
  { name: 'users', count: 0, description: '用户表' },
  { name: 'courses', count: 0, description: '课程表' },
  { name: 'course_structures', count: 0, description: '课程结构表' },
  { name: 'materials', count: 0, description: '学习资料表' },
  { name: 'user_progress', count: 0, description: '用户进度表' },
  { name: 'user_grades', count: 0, description: '用户成绩表' },
]);
const schema = ref<any>(null);
const queryInput = ref('');
const queryResult = ref<any>(null);
const analysisReport = ref('');

const loadCollections = async () => {
  try {
    const response = await agentApi.getCollections();
    if (response.data) {
      collections.value = response.data;
      message.success('加载成功');
    }
  } catch (error) {
    message.error('加载失败');
  }
};

const loadSchema = async () => {
  try {
    const response = await agentApi.getDatabaseSchema();
    schema.value = response.data;
    activeTab.value = 'schema';
    message.success('架构加载成功');
  } catch (error) {
    message.error('加载失败');
  }
};

const viewCollection = (name: string) => {
  queryInput.value = `db.${name}.find({})`;
  activeTab.value = 'query';
};

const analyzeCollection = async (name: string) => {
  try {
    const response = await agentApi.queryDatabase(`分析集合 ${name} 的结构和用途`);
    analysisReport.value = response.data;
    activeTab.value = 'analysis';
    message.success('分析完成');
  } catch (error) {
    message.error('分析失败');
  }
};

const executeQuery = async () => {
  if (!queryInput.value) {
    message.warning('请输入查询语句');
    return;
  }
  
  try {
    const response = await agentApi.queryDatabase(queryInput.value);
    queryResult.value = response.data;
    message.success('查询成功');
  } catch (error) {
    message.error('查询失败');
  }
};

const clearQuery = () => {
  queryInput.value = '';
  queryResult.value = null;
};
</script>

<style scoped>
.database-explorer {
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
