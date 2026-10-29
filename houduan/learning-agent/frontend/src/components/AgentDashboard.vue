<template>
  <div class="agent-dashboard">
    <a-layout style="min-height: 100vh">
      <a-layout-sider v-model:collapsed="collapsed" collapsible>
        <div class="logo">
          <BookOutlined v-if="collapsed" />
          <span v-else>电磁场智能学习助手</span>
        </div>
        <a-menu v-model:selectedKeys="selectedKeys" theme="dark" mode="inline">
          <a-menu-item key="dashboard" @click="navigateTo('/agent')">
            <DashboardOutlined />
            <span>概览</span>
          </a-menu-item>
          <a-menu-item key="navigator" @click="navigateTo('/agent/navigator')">
            <AimOutlined />
            <span>页面导航</span>
          </a-menu-item>
          <a-menu-item key="components" @click="navigateTo('/agent/components')">
            <AppstoreOutlined />
            <span>组件分析</span>
          </a-menu-item>
          <a-menu-item key="database" @click="navigateTo('/agent/database')">
            <DatabaseOutlined />
            <span>数据库分析</span>
          </a-menu-item>
          <a-menu-item key="chat" @click="navigateTo('/agent/chat')">
            <MessageOutlined />
            <span>AI对话</span>
          </a-menu-item>
        </a-menu>
      </a-layout-sider>

      <a-layout>
        <a-layout-header style="background: #fff; padding: 0 24px">
          <h2>电磁场智能学习助手</h2>
        </a-layout-header>
        <a-layout-content style="margin: 16px">
          <div style="padding: 24px; background: #fff; min-height: 360px">
            <router-view />
          </div>
        </a-layout-content>
      </a-layout>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  DashboardOutlined,
  AimOutlined,
  AppstoreOutlined,
  DatabaseOutlined,
  MessageOutlined,
  BookOutlined,
} from '@ant-design/icons-vue';

const collapsed = ref(false);
const selectedKeys = ref(['dashboard']);
const router = useRouter();

const navigateTo = (path: string) => {
  router.push(path);
};
</script>

<style scoped>
.logo {
  height: 32px;
  margin: 16px;
  color: white;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
