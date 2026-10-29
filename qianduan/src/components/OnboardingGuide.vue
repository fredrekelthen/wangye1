<template>
  <a-modal
    v-model:open="open"
    :footer="null"
    :closable="false"
    :mask-closable="false"
    width="520px"
    title="欢迎使用电磁场智能学习平台 🎓"
  >
    <a-steps :current="step" size="small" class="steps">
      <a-step title="知识图谱" />
      <a-step title="AI 答疑" />
      <a-step title="测评自检" />
    </a-steps>

    <div class="step-body">
      <template v-if="step === 0">
        <p>在「课程学习」页，通过<strong>知识图谱</strong>和章节树浏览知识点，点击节点即可查看内容。</p>
      </template>
      <template v-else-if="step === 1">
        <p>点击右上角<strong>AI 助手</strong>，用自然语言提问，系统会结合课程资料给出带引用的解答。</p>
      </template>
      <template v-else>
        <p>在课程内完成<strong>测评</strong>检验掌握程度，个人中心可查看学习趋势与成绩。</p>
      </template>
    </div>

    <div class="step-actions">
      <a-button v-if="step > 0" @click="step--">上一步</a-button>
      <a-button v-if="step < 2" type="primary" @click="step++">下一步</a-button>
      <a-button v-else type="primary" @click="finish">开始学习</a-button>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const open = ref(false)
const step = ref(0)

function show() {
  // 已完成过引导则不再弹出
  if (localStorage.getItem('onboardingDone')) return
  step.value = 0
  open.value = true
}

function finish() {
  localStorage.setItem('onboardingDone', '1')
  open.value = false
}

defineExpose({ show })
</script>

<style scoped>
.steps { margin-bottom: 20px; }
.step-body { min-height: 90px; font-size: 14px; line-height: 1.9; color: #333; }
.step-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
</style>
