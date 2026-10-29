<template>
  <div id="global-header">
    <a-row :wrap="false" align="middle" justify="space-between">
      <a-col flex="200px">
        <div class="title-bar">
          <img class="logo" src="@/assets/huzuhhh.jpg" alt="logo"/>
          <div class="title">思政教育</div>
        </div>
      </a-col>
      <a-col flex="auto" class="menu-col">
        <a-menu 
          v-if="!isMobile"
          v-model:selectedKeys="current" 
          mode="horizontal"
          :items="menuItems" 
          @click="doMenuClick"
        />
        <a-button v-else type="text" class="mobile-menu-btn" @click="drawerVisible = true">
          <MenuOutlined />
        </a-button>
      </a-col>
      <a-col flex="auto" class="right-section">
        <a-button class="ai-assistant-btn" @click="openAgentChat">
          <RobotOutlined /> <span class="ai-text">AI助手</span>
        </a-button>
        <div class="login-status" :key="'login-' + (computedUser?.id || 'no-user')">
          <a-dropdown v-if="isUserLoggedIn">
            <div class="user-info">
              <a-avatar :size="32" class="user-avatar">
                {{ (computedUser.userName || computedUser.userAccount || '用').charAt(0).toUpperCase() }}
              </a-avatar>
              <span v-if="!isMobile" class="username">{{ computedUser.userName || computedUser.userAccount || '用户' }}</span>
              <CaretDownOutlined class="dropdown-icon" />
            </div>
            <template #overlay>
              <a-menu @click="handleMenuClick">
                <a-menu-item key="profile">
                  <UserOutlined /> 个人中心
                </a-menu-item>
                <a-menu-item key="change-password">
                  <LockOutlined /> 修改密码
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" style="color: #ff4d4f;">
                  <LogoutOutlined /> 退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
          <a-button v-else class="login-btn" @click="showAuthModal">登录/注册</a-button>
        </div>
      </a-col>
    </a-row>

    <AuthModal v-model:visible="authModalVisible" />
    <AgentChat v-model:open="agentModalVisible" />
    <ChangePasswordModal v-model:open="changePwdVisible" />

    <!-- 移动端抽屉菜单 -->
    <a-drawer v-model:open="drawerVisible" placement="left" :width="240" :closable="false">
      <div class="drawer-title">导航菜单</div>
      <a-menu mode="vertical" :items="menuItems" @click="onMobileMenuClick" />
    </a-drawer>
  </div>
</template>

<script lang="ts" setup>
import { h, ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { CrownOutlined, HomeOutlined, UserOutlined, CaretDownOutlined, LogoutOutlined, RobotOutlined, MenuOutlined, LockOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { useUserLogin, getGlobalCurrentUser } from '@/store/useUesrLogin'
import { message } from 'ant-design-vue'
import { userLogout } from '@/api/user'
import AuthModal from './AuthModal.vue'
import AgentChat from './AgentChat.vue'
import ChangePasswordModal from './ChangePasswordModal.vue'

const router = useRouter()
const authModalVisible = ref(false)
const agentModalVisible = ref(false)
const drawerVisible = ref(false)
const changePwdVisible = ref(false)
const isMobile = ref(false)
const updateIsMobile = () => { isMobile.value = window.innerWidth < 768 }
const userStore = useUserLogin()

const computedUser = computed(() => {
  const storeUser = userStore.loginUser.value
  if (storeUser && storeUser.id) {
    return storeUser
  }
  
  const globalUser = userStore.globalLoginUser?.()
  if (globalUser && globalUser.id) {
    return globalUser
  }
  
  const lsUser = getGlobalCurrentUser()
  if (lsUser && lsUser.id) {
    nextTick(() => {
      if (!userStore.loginUser.value?.id) {
        userStore.setLoginUser(lsUser)
      }
    })
    return lsUser
  }

  return null
})

const isUserLoggedIn = computed(() => !!computedUser.value?.id)

watch(computedUser, (newVal) => {
  console.log('[GlobalHeader] computedUser 变化:', newVal?.id ? newVal.userAccount + '(id=' + newVal.id.substring(0,8) + '...)' : 'null/undefined')
}, { immediate: true })

const doMenuClick = (e:any)=> router.push(e.key)
const onMobileMenuClick = (e: any) => {
  drawerVisible.value = false
  if (e.key === 'others') {
    window.open('https://www.xuexi.cn', '_blank')
    return
  }
  router.push(e.key)
}
const current = ref([router.currentRoute.value?.path || '/'])
router.afterEach(to=> current.value = [to.path])

const openAuthModal = ()=> authModalVisible.value = true
const showAuthModal = openAuthModal
const openAgentChat = ()=> agentModalVisible.value = true
defineExpose({ openAuthModal, openAgentChat })

const onStorage = ()=>{
  const m = localStorage.getItem('openAuthModalMode')
  if (m === 'login' || m === 'register') { openAuthModal(); localStorage.removeItem('openAuthModalMode') }
}

const handleUserLoggedIn = () => {
  console.log('[GlobalHeader] 收到登录事件')
}

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

const handleMenuClick = async ({ key }: { key: string }) => {
  if (key === 'profile') {
    router.push('/user-center')
  } else if (key === 'change-password') {
    changePwdVisible.value = true
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

const handleOpenAgentChat = () => {
  openAgentChat()
}

onMounted(()=> {
  updateIsMobile()
  window.addEventListener('resize', updateIsMobile)
  window.addEventListener('user-logged-in', handleUserLoggedIn as any)
  window.addEventListener('storage', onStorage)
  window.addEventListener('openAgentChat', handleOpenAgentChat)
})

onUnmounted(()=> { 
  window.removeEventListener('resize', updateIsMobile)
  window.removeEventListener('user-logged-in', handleUserLoggedIn as any)
  window.removeEventListener('storage', onStorage)
  window.removeEventListener('openAgentChat', handleOpenAgentChat)
})
</script>

<style scoped>
#global-header {
  background: linear-gradient(135deg, #1e88e5 0%, #00b894 100%);
  padding: 0 24px;
}

.title-bar {
  display: flex;
  align-items: center;
}

.title {
  color: #ffffff;
  font-size: 18px;
  margin-left: 16px;
  font-weight: 600;
}

.logo {
  height: 44px;
}

.right-section {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  padding-right: 12px;
}

.ai-assistant-btn {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.4);
  color: #ffffff;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.ai-assistant-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  border-color: rgba(255, 255, 255, 0.6);
  color: #ffffff !important;
}

.login-btn {
  background: rgba(255, 255, 255, 0.95);
  border: none;
  color: #00b894;
  font-weight: 500;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.login-btn:hover {
  background: #ffffff;
  color: #00b894 !important;
  box-shadow: 0 2px 8px rgba(0, 184, 148, 0.3);
}

::deep(.ant-menu) {
  background: linear-gradient(135deg, #1e88e5 0%, #00b894 100%) !important;
  border-bottom: none !important;
}

::deep(.ant-menu-horizontal) {
  background: linear-gradient(135deg, #1e88e5 0%, #00b894 100%) !important;
}

::deep(.ant-menu-item) {
  color: rgba(255, 255, 255, 0.9);
}

::deep(.ant-menu-item:hover) {
  color: #ffffff !important;
}

::deep(.ant-menu-item-selected) {
  background: rgba(255, 255, 255, 0.2) !important;
  color: #ffffff !important;
  border-radius: 4px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.15);
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.25);
}

.user-avatar {
  background: rgba(255, 255, 255, 0.3) !important;
  color: #ffffff;
  font-weight: 600;
}

.username {
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-icon {
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
}

.login-status {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.mobile-menu-btn {
  color: #ffffff;
  font-size: 20px;
}

.drawer-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

/* 移动端（<768px）收缩右侧与标题 */
@media (max-width: 767px) {
  #global-header {
    padding: 0 12px;
  }
  .title {
    font-size: 15px;
    margin-left: 8px;
  }
  .logo {
    height: 34px;
  }
  .ai-text {
    display: none;
  }
  .right-section {
    gap: 6px;
    padding-right: 0;
  }
}
</style>
