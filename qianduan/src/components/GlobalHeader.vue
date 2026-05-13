<template>
  <div id ="global-header">
    <a-row :wrap="false" align="middle" justify="space-between">
      <a-col flex="200px">
        <div  class = "title-bar">
          <img class = "loge" src = "@/assets/huzuhhh.jpg" alt = "loge"/>
          <div class = "title">思政教育</div>
        </div>
      </a-col>
      <a-col flex="auto">
        <a-menu 
          v-model:selectedKeys="current" 
          mode="horizontal"
          :items="menuItems" 
          @click="doMenuClick"
        />
      </a-col>
      <!-- 【关键】使用 computedUser 作为唯一数据源，带 fallback -->
      <a-col flex="140px">
        <div class ="login-status" :key="'login-' + (computedUser?.id || 'no-user')">
          <a-dropdown v-if="isUserLoggedIn">
            <div class="user-info">
              <a-avatar :size="32" style="background-color: #00B894;">
                {{ (computedUser.userName || computedUser.userAccount || '用').charAt(0).toUpperCase() }}
              </a-avatar>
              <span class="username">{{ computedUser.userName || computedUser.userAccount || '用户' }}</span>
              <CaretDownOutlined style="color: #666; font-size: 12px;" />
            </div>
            <template #overlay>
              <a-menu @click="handleMenuClick">
                <a-menu-item key="profile">
                  <UserOutlined /> 个人中心
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" style="color: #ff4d4f;">
                  <LogoutOutlined /> 退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
          <a-button v-else type = "primary" @click="showAuthModal">登录/注册</a-button>
        </div>
      </a-col>
    </a-row>

    <!-- 登录注册弹窗 -->
    <AuthModal v-model:visible="authModalVisible" />
  </div>
</template>

<script lang="ts" setup>
import { h, ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { CrownOutlined, HomeOutlined, UserOutlined, CaretDownOutlined, LogoutOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { useUserLogin, getGlobalCurrentUser } from '@/store/useUesrLogin'
import { message } from 'ant-design-vue'
import { userLogout } from '@/api/user'
import AuthModal from './AuthModal.vue'

const router = useRouter()
const authModalVisible = ref(false)
const userStore = useUserLogin()

// 【关键】统一数据源计算属性 - 带三层 fallback
const computedUser = computed(() => {
  // 第一层：Pinia Store ref（Vue 响应式）
  const storeUser = userStore.loginUser.value
  if (storeUser && storeUser.id) {
    return storeUser
  }
  
  // 第二层：全局变量（跨实例保障）
  const globalUser = userStore.globalLoginUser?.()
  if (globalUser && globalUser.id) {
    console.log('[GlobalHeader] 使用 fallback: globalLoginUser')
    return globalUser
  }
  
  // 第三层：localStorage（持久化保障）
  const lsUser = getGlobalCurrentUser()
  if (lsUser && lsUser.id) {
    console.log('[GlobalHeader] 使用 fallback: localStorage')
    // 回填到 Pinia，恢复响应式
    nextTick(() => {
      if (!userStore.loginUser.value?.id) {
        userStore.setLoginUser(lsUser)
      }
    })
    return lsUser
  }

  return null
})

// 是否已登录
const isUserLoggedIn = computed(() => !!computedUser.value?.id)

// 监听变化（调试）
watch(computedUser, (newVal) => {
  console.log('[GlobalHeader] computedUser 变化:', newVal?.id ? newVal.userAccount + '(id=' + newVal.id.substring(0,8) + '...)' : 'null/undefined')
}, { immediate: true })

const doMenuClick = (e:any)=> router.push(e.key)
const current = ref([router.currentRoute.value?.path || '/'])
router.afterEach(to=> current.value = [to.path])

const openAuthModal = ()=> authModalVisible.value = true
const showAuthModal = openAuthModal
defineExpose({ openAuthModal })

const onStorage = ()=>{
  const m = localStorage.getItem('openAuthModalMode')
  if (m === 'login' || m === 'register') { openAuthModal(); localStorage.removeItem('openAuthModalMode') }
}

// 监听全局登录事件
const handleUserLoggedIn = () => {
  console.log('[GlobalHeader] 收到登录事件')
}

// 动态生成菜单项
const menuItems = computed(() => {
  const items: any[] = [
    { key: '/', icon: ()=>h(HomeOutlined), label: '主页', title: '主页' },
  ]

  const user = computedUser.value
  const isAdmin = user?.userRole === 'admin' || user?.role === 'ADMIN' || user?.userRole === 'ADMIN'
  
  if (isUserLoggedIn.value && isAdmin) {
    items.push({ key: '/admin/material-manage', icon: ()=>h(CrownOutlined), label: '资料管理', title: '资料管理' })
    items.push({ key: '/admin/user-manage', icon: ()=>h(UserOutlined), label: '学生管理', title: '学生管理' })
  }

  items.push({ key: '/course', icon: ()=>h(HomeOutlined), label: '课程学习', title: '课程学习' })
  items.push({ key: 'others', label: h('a',{href:'https://www.xuexi.cn',target:'_blank'}, '学习强国'), title: '跳转学习强国' })

  return items
})

// 处理下拉菜单点击
const handleMenuClick = async ({ key }: { key: string }) => {
  if (key === 'profile') {
    router.push('/user-center')
  } else if (key === 'logout') {
    try {
      await userLogout({})
      userStore.clearLoginUser()
      message.success('退出登录成功')
      router.push('/')
    } catch (error) {
      userStore.clearLoginUser()
      message.success('已退出登录')
    }
  }
}

onMounted(()=> {
  window.addEventListener('user-logged-in', handleUserLoggedIn as any)
  
  // 初始化时也检查 fallback
  if (!userStore.loginUser.value?.id) {
    const fallbackUser = getGlobalCurrentUser()
    if (fallbackUser?.id) {
      console.log('[GlobalHeader] 初始化时从 fallback 恢复用户:', fallbackUser.userAccount)
      userStore.setLoginUser(fallbackUser)
    }
  }
  
  window.addEventListener('storage', onStorage)
})

onUnmounted(()=> { 
  window.removeEventListener('user-logged-in', handleUserLoggedIn as any)
  window.removeEventListener('storage', onStorage)
})
</script>

<style scoped>
.title-bar{
  display:flex;
  align-items: center ;
}
.title{
  color: #f0f0f0;
  font-size: 18px;
  margin-left: 16px;
  font-weight: 600;
}
.loge{
  height: 44px;
}
::deep(.ant-menu-item-selected) {
  background: linear-gradient(135deg, #00B894 0%, #55E6C1 100%) !important;
  color: #fff !important;
  border-radius: 4px;
}
::deep(.ant-menu-item:hover) {
  color: #00B894 !important;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.1);
}
.user-info:hover {
  background: rgba(255, 255, 255, 0.2);
}
.username {
  color: #f0f0f0;
  font-size: 14px;
  font-weight: 500;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.login-status {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
</style>
