<template>
  <a-modal
    v-model:open="modalVisible"
    :title="activeTab === 'login' ? '用户登录' : '用户注册'"
    :footer="null"
    width="500px"
    :destroyOnClose="true"
    @cancel="handleCancel"
  >
    <!-- 登录类型选择 -->
    <div class="login-type-selector">
      <a-radio-group v-model:value="loginType" button-style="solid" size="large">
        <a-radio-button value="student">学生登录</a-radio-button>
        <a-radio-button value="admin">管理员登录</a-radio-button>
      </a-radio-group>
    </div>

    <a-tabs v-model:activeKey="activeTab" centered v-if="loginType === 'student'">
      <a-tab-pane key="login" tab="登录">
        <a-form
          :model="loginFormState"
          name="login"
          autocomplete="off"
          @finish="handleLogin"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 18 }"
        >
          <a-form-item label="账号" name="userAccount" :rules="[{ required: true, message: '请输入您的账号' }]">
            <a-input v-model:value="loginFormState.userAccount" placeholder="请输入您的账号" />
          </a-form-item>

          <a-form-item label="密码" name="userPassword"
            :rules="[{ required: true, message: '请输入您的密码' }, { min: 6, message: '密码长度不能少于6位' }]">
            <a-input-password v-model:value="loginFormState.userPassword" placeholder="请输入您的密码" />
          </a-form-item>

          <a-form-item :wrapper-col="{ offset: 6, span: 18 }">
            <a-button type="primary" html-type="submit" :loading="loginLoading" block>登录</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>

      <a-tab-pane key="register" tab="注册">
        <a-form
          :model="registerFormState"
          name="register"
          autocomplete="off"
          @finish="handleRegister"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 18 }"
        >
          <a-form-item label="编号" name="planetCode" :rules="[{ required: true, message: '请输入您的编号' }]">
            <a-input v-model:value="registerFormState.planetCode" placeholder="请输入您的编号" />
          </a-form-item>

          <a-form-item label="账号" name="userAccount" :rules="[{ required: true, message: '请输入您的账号' }]">
            <a-input v-model:value="registerFormState.userAccount" placeholder="请输入您的账号" />
          </a-form-item>

          <a-form-item label="密码" name="userPassword"
            :rules="[{ required: true, message: '请输入您的密码' }, { min: 6, message: '密码长度不能少于6位' }]">
            <a-input-password v-model:value="registerFormState.userPassword" placeholder="请输入您的密码" />
          </a-form-item>

          <a-form-item label="确认密码" name="checkPassword"
            :rules="[{ required: true, message: '请确认您的密码' }, { min: 6, message: '密码长度不能少于6位' }]">
            <a-input-password v-model:value="registerFormState.checkPassword" placeholder="请确认您的密码" />
          </a-form-item>

          <a-form-item :wrapper-col="{ offset: 6, span: 18 }">
            <a-button type="primary" html-type="submit" :loading="registerLoading" block>注册</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
    </a-tabs>

    <!-- 管理员登录表单 -->
    <div v-else class="admin-login-form">
      <a-form
        :model="loginFormState"
        name="admin-login"
        autocomplete="off"
        @finish="handleLogin"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-form-item label="账号" name="userAccount" :rules="[{ required: true, message: '请输入管理员账号' }]">
          <a-input v-model:value="loginFormState.userAccount" placeholder="请输入管理员账号" />
        </a-form-item>

        <a-form-item label="密码" name="userPassword" :rules="[{ required: true, message: '请输入管理员密码' }]">
          <a-input-password v-model:value="loginFormState.userPassword" placeholder="请输入管理员密码" />
        </a-form-item>

        <a-form-item :wrapper-col="{ offset: 6, span: 18 }">
          <a-button type="primary" html-type="submit" :loading="loginLoading" block>管理员登录</a-button>
        </a-form-item>
      </a-form>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import { ref, computed, reactive, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { userLogin, userRegister } from '@/api/user'
import { useUserLogin, getGlobalCurrentUser } from '@/store/useUesrLogin'

const props = defineProps({ visible: { type: Boolean, default: false } })
const emit = defineEmits(['update:visible'])
const modalVisible = computed({ get: () => props.visible, set: v => emit('update:visible', v) })

const activeTab = ref('login')
const loginType = ref('student')
const loginLoading = ref(false)
const registerLoading = ref(false)
const router = useRouter()
const userStore = useUserLogin()

const loginFormState = reactive({ userAccount: '', userPassword: '' })
const registerFormState = reactive({ planetCode: '', userAccount: '', userPassword: '', checkPassword: '' })

watch(loginType, (newType) => {
  if (newType === 'admin') activeTab.value = 'login'
  Object.assign(loginFormState, { userAccount: '', userPassword: '' })
})

const handleCancel = () => {
  modalVisible.value = false
  Object.assign(loginFormState, { userAccount: '', userPassword: '' })
  Object.assign(registerFormState, { planetCode: '', userAccount: '', userPassword: '', checkPassword: '' })
}

const handleLogin = async (values: any) => {
  if (loginType.value === 'admin' && values.userAccount !== 'admin') {
    return message.error('管理员账号必须是admin')
  }

  loginLoading.value = true
  try {
    const res = await userLogin(values)

    if (res.data.code === 0 && res.data.data) {
      const userData = res.data.data
      const userRole = userData.role || userData.userRole

      if (loginType.value === 'admin' && userRole !== 'ADMIN') {
        return message.error('该账号不是管理员账号')
      }
      if (loginType.value === 'student' && userRole === 'ADMIN') {
        return message.error('管理员账号请使用管理员登录')
      }

      // ========== 第一步：写入 Store（三重保障）==========
      userStore.setLoginUser(userData)

      // ========== 第二步：立即验证写入结果 ==========
      const verifyStore = userStore.loginUser.value
      const verifyGlobal = getGlobalCurrentUser()
      console.log('[AuthModal] 写入后验证:')
      console.log('  [Pinia] id=', verifyStore?.id)
      console.log('  [全局] id=', verifyGlobal?.id)
      
      if (!verifyStore?.id && !verifyGlobal?.id) {
        console.error('[AuthModal] ⚠️ 所有数据源都为空！尝试直接回填 localStorage...')
        // 极端情况：手动回填
        localStorage.setItem('userInfo', JSON.stringify(userData))
        // 触发 storage 事件让其他组件感知
        window.dispatchEvent(new Event('storage'))
      }

      // ========== 第三步：通知全局 + 等 DOM 更新 ==========
      window.dispatchEvent(new CustomEvent('user-logged-in', { detail: userData }))
      await nextTick()

      message.success('登录成功')
      modalVisible.value = false

      await nextTick()
      router.push({ path: '/', replace: true })
    } else {
      message.error('登录失败，请检查账号或密码')
    }
  } catch (error) {
    console.error('[AuthModal] 登录请求失败:', error)
    message.error('登录请求失败')
  } finally {
    loginLoading.value = false
  }
}

const handleRegister = async (values: any) => {
  if (registerFormState.userPassword !== registerFormState.checkPassword) {
    return message.error('两次密码输入不一致')
  }
  registerLoading.value = true
  try {
    const res = await userRegister(values)
    if (res.data.code === 0 && res.data.data) {
      message.success('注册成功，请登录')
      activeTab.value = 'login'
      Object.assign(registerFormState, { planetCode: '', userAccount: '', userPassword: '', checkPassword: '' })
    } else {
      message.error('注册失败: ' + (res.data.description || '未知错误'))
    }
  } catch {
    message.error('注册请求失败')
  } finally {
    registerLoading.value = false
  }
}
</script>

<style scoped>
.login-type-selector {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}
:::deep(.ant-radio-button-wrapper-checked) {
  background: linear-gradient(135deg, #007BFF 0%, #00B894 100%) !important;
  border-color: #007BFF !important;
  color: #fff !important;
}
.admin-login-form { padding-top: 20px; }
:::deep(.ant-btn-primary) {
  background: linear-gradient(135deg, #007BFF 0%, #00B894 100%);
  border-color: #00B894;
}
:::deep(.ant-input:focus) {
  border-color: #007BFF;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.2);
}
</style>
