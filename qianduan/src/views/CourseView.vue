<template>
  <div class="course-page">
    <CourseHeader />
    <CourseTabs :activeTab="activeTab" @update:activeTab="handleTabChange" />

    <main class="course-main">
      <aside class="course-sidebar">
        <CourseTree v-if="activeTab === 'content'" />
      </aside>
      <section class="course-content" :class="{ 'full-width': activeTab !== 'content' }">
        <!-- 学习内容 -->
        <CourseContent v-if="activeTab === 'content'" />
        
        <!-- 讨论区 -->
        <CourseDiscuss v-if="activeTab === 'discuss'" />
        
        <!-- 公告 -->
        <CourseNotice v-if="activeTab === 'notice'" />
        
        <!-- 测试 -->
        <CourseTest v-if="activeTab === 'test'" />
        
        <!-- 成绩单 -->
        <CourseGrade v-if="activeTab === 'grade'" />

        <!-- 电磁仿真 -->
        <CourseEmulation v-if="activeTab === 'emulation'" />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import CourseHeader from '@/components/course/CourseHeader.vue'
import CourseTabs from '@/components/course/CourseTabs.vue'
import CourseTree from '@/components/course/CourseTree.vue'
import CourseContent from '@/components/course/CourseContent.vue'
import CourseDiscuss from '@/components/course/CourseDiscuss.vue'
import CourseNotice from '@/components/course/CourseNotice.vue'
import CourseTest from '@/components/course/CourseTest.vue'
import CourseGrade from '@/components/course/CourseGrade.vue'
import CourseEmulation from '@/components/course/CourseEmulation.vue'

const router = useRouter();
const route = useRoute();
const activeTab = ref('content');

const handleTabChange = (tabKey: string) => {
  activeTab.value = tabKey;
  localStorage.setItem('courseActiveTab', tabKey);
  router.replace({ path: router.currentRoute.value.path, query: { tab: tabKey } });
};

onMounted(() => {
  const q = (route.query.tab as string) || localStorage.getItem('courseActiveTab') || 'content';
  activeTab.value = q;
  router.replace({ path: route.path, query: { ...route.query, tab: q } });
});

// 页面内通过 router.push 切换 tab（如知识图谱跳转到仿真页）时同步页签
watch(() => route.query.tab, (tab) => {
  if (typeof tab === 'string' && tab && tab !== activeTab.value) {
    activeTab.value = tab;
    localStorage.setItem('courseActiveTab', tab);
  }
});
</script>

<style scoped>
.course-page{padding:20px;font-family: "Microsoft YaHei", Arial, sans-serif;color:#333}
.course-main{display:flex;margin-top:18px}
.course-sidebar{width:360px;margin-right:20px}
.course-content{flex:1;background:#fff;border:1px solid #e6e6e6;padding:20px;border-radius:4px;min-height:420px;transition:all 0.3s}
.course-content.full-width{flex:1;background:#fff;border:1px solid #e6e6e6;padding:20px;border-radius:4px;min-height:420px}
</style>
