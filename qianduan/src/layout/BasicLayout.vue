<template>
  <div id="basic-layout">
    <a-layout class="layout-container">
      <!-- 固定顶部栏 -->
      <a-layout-header class="header">
        <GlobalHeader />
      </a-layout-header>

      <!-- 可自适应滚动的中间内容区 -->
      <a-layout-content class="content">
        <RouterView />
      </a-layout-content>

      <!-- 废弃的底部栏
      <a-layout-footer class="footer">
        <a href="https://www.xuexi.cn/" target="_blank">底部栏，暂时跳转百度</a>
        <a>电磁场与电磁波</a>
      </a-layout-footer> -->
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';
import GlobalHeader from '@/components/GlobalHeader.vue';

const handler = (e: Event) => {
  const mode = (e as CustomEvent).detail?.mode || 'login';
  localStorage.setItem('openAuthModalMode', mode);
  window.dispatchEvent(new Event('storage'));
};
onMounted(()=> window.addEventListener('openAuthModal', handler));
onUnmounted(()=> window.removeEventListener('openAuthModal', handler));
</script>

<style lang="scss" scoped>
#basic-layout {
  height: 100vh; /* 确保布局容器占满整个视口高度 */

  .layout-container {
    display: flex;
    flex-direction: column;
    height: 100%;
  }

  .header {
    background: linear-gradient(135deg, #007BFF 0%, #00B894 100%);
    color: white;
    text-align: center;
    line-height: 60px; /* 垂直居中 */
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 1000; /* 确保顶部栏在最前 */
    flex-shrink: 0; /* 防止缩放 */
  }

  .content {
    flex: 1; /* 关键：占据剩余所有空间 */
    overflow-y: auto; /* 内容过长时允许滚动 */
    margin-top: 60px; /* 为固定顶部栏留出空间 */
    margin-bottom: 50px; /* 为固定底部栏留出空间 */
    padding: 16px; /* 内容区内边距 */
    box-sizing: border-box;
    background: #F0F9F7;
  }

  /* 废弃的底部栏 .footer {
    background: linear-gradient(90deg, #007BFF 0%, #00B894 100%);
    text-align: center;
    line-height: 50px;
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 999;
    flex-shrink: 0; 
    color: white;
    a {
      color: white;
      text-decoration: none;
    }
    a:hover {
      text-decoration: underline;
    }
  }*/
}
</style>