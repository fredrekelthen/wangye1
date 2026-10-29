<template>
  <div class="course-tabs">
    <ul role="tablist">
      <li 
        v-for="t in tabs" 
        :key="t.key" 
        :class="{active: activeTab === t.key}"
        @click="handleTabClick(t.key)"
      >
        {{ t.label }}
      </li>
    </ul>
  </div>
</template>

<script setup>
const tabs = [{key:'content',label:'学习内容'},{key:'discuss',label:'讨论区'},{key:'notice',label:'公告'},{key:'test',label:'测试'},{key:'grade',label:'成绩单'},{key:'emulation',label:'电磁仿真'}]
const props = defineProps({ activeTab: String })
const emit = defineEmits(['update:activeTab'])
const handleTabClick = k => emit('update:activeTab', k)
</script>

<style scoped>
.course-tabs{margin-top:12px}
.course-tabs ul{display:flex;gap:12px;list-style:none;padding:0;margin:0}
.course-tabs li{padding:10px 14px;border-radius:4px;cursor:pointer;color:#555;transition:all 0.3s}
.course-tabs li:hover{background:#F0F9F7;color:#007BFF}
.course-tabs li.active{background:linear-gradient(135deg, #007BFF 0%, #00B894 100%);color:#fff;font-weight:600;border-bottom:3px solid #00B894}
</style>
