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
        
        <!-- 分组 -->
        <CourseGroup v-if="activeTab === 'group'" />
        
        <!-- 成绩单 -->
        <CourseGrade v-if="activeTab === 'grade'" />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import CourseHeader from '@/components/course/CourseHeader.vue'
import CourseTabs from '@/components/course/CourseTabs.vue'
import CourseTree from '@/components/course/CourseTree.vue'
import CourseContent from '@/components/course/CourseContent.vue'
import CourseDiscuss from '@/components/course/CourseDiscuss.vue'
import CourseNotice from '@/components/course/CourseNotice.vue'
import CourseGroup from '@/components/course/CourseGroup.vue'
import CourseGrade from '@/components/course/CourseGrade.vue'

const router = useRouter();
const activeTab = ref('content');

const handleTabChange = (tabKey: string) => {
  activeTab.value = tabKey;
  localStorage.setItem('courseActiveTab', tabKey);
  router.replace({ path: router.currentRoute.value.path, query: { tab: tabKey } });
};

onMounted(() => {
  const q = (router.currentRoute.value.query.tab as string) || localStorage.getItem('courseActiveTab') || 'content';
  activeTab.value = q;
  router.replace({ path: router.currentRoute.value.path, query: { tab: q } });
});
</script>

<style scoped>
.course-page{padding:20px;font-family: "Microsoft YaHei", Arial, sans-serif;color:#333}
.course-main{display:flex;margin-top:18px}
.course-sidebar{width:360px;margin-right:20px}
.course-content{flex:1;background:#fff;border:1px solid #e6e6e6;padding:20px;border-radius:4px;min-height:420px;transition:all 0.3s}
.course-content.full-width{flex:1;background:#fff;border:1px solid #e6e6e6;padding:20px;border-radius:4px;min-height:420px}
</style>

