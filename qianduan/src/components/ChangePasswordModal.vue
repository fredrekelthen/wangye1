<template>
  <a-modal
    v-model:open="visible"
    title="修改密码"
    :footer="null"
    :mask-closable="false"
    width="440px"
  >
    <a-form :model="form" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }" @finish="onSubmit">
      <a-form-item label="原密码" name="oldPassword" :rules="[{ required: true, message: '请输入原密码' }]">
        <a-input-password v-model:value="form.oldPassword" placeholder="请输入原密码" />
      </a-form-item>
      <a-form-item label="新密码" name="newPassword" :rules="[{ required: true, message: '请输入新密码' }, { min: 6, message: '密码长度不能少于6位' }]">
        <a-input-password v-model:value="form.newPassword" placeholder="请输入新密码（至少6位）" />
      </a-form-item>
      <a-form-item label="确认密码" name="checkPassword" :rules="[{ required: true, message: '请再次输入新密码' }, { validator: validateCheck }]">
        <a-input-password v-model:value="form.checkPassword" placeholder="请再次输入新密码" />
      </a-form-item>
      <a-form-item :wrapper-col="{ offset: 5, span: 18 }">
        <a-button type="primary" html-type="submit" :loading="loading" block>确认修改</a-button>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { changeSelfPassword } from '@/api/user'

const props = defineProps({ open: { type: Boolean, default: false } })
const emit = defineEmits(['update:open'])

const visible = computed({
  get: () => props.open,
  set: (v) => emit('update:open', v)
})

const form = reactive({ oldPassword: '', newPassword: '', checkPassword: '' })
const loading = ref(false)

const validateCheck = async (_rule: any, value: string) => {
  if (value !== form.newPassword) {
    throw new Error('两次输入的密码不一致')
  }
}

const onSubmit = async () => {
  loading.value = true
  try {
    const res = await changeSelfPassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    if (res.data.code === 0) {
      message.success('密码修改成功')
      Object.assign(form, { oldPassword: '', newPassword: '', checkPassword: '' })
      visible.value = false
    }
  } catch (e) {
    // 错误提示已由全局拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
:deep(.ant-modal) { max-width: 92vw; }
</style>
