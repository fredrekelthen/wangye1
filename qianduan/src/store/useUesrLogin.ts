import { getCurrentUser } from '@/api/user'
import { defineStore } from 'pinia'
import { ref } from 'vue'

// ========== 【保障1】全局变量作为备用数据源 ==========
let globalLoginUser: any = null

export const useUserLogin = defineStore('counter', () => {
  // 初始化时从 localStorage 读取用户信息（但不自动认为已登录）
  const storedUserInfo = localStorage.getItem('userInfo')
  
  // 默认状态为未登录
  const initialUser: any = { userName: '未登录' }
  
  // 注意：这里不再自动从 localStorage 恢复用户状态
  // 会话验证将在应用启动时通过 fetchLoginUser 进行
  
  const loginUser = ref<any>(initialUser)

  // 远程获取登录用户信息
  async function fetchLoginUser(userInfo?: any) {
    try {
      console.log('[Store] 开始 fetchLoginUser...')
      const res = await getCurrentUser();
      if (res.data.code === 0 && res.data.data) {
        const userData = res.data.data
        syncUserRoleFields(userData)
        
        // 【保障】同时更新三个地方
        loginUser.value = userData
        globalLoginUser = userData
        localStorage.setItem('userInfo', JSON.stringify(userData))
        
        console.log('[Store] fetchLoginUser 成功:', userData.userAccount, 'id:', userData.id)
      } else {
        // 后端会话失效，清除本地存储的用户信息
        console.log('[Store] 后端会话失效，清除本地状态')
        clearLoginUser()
      }
    } catch (error) {
      console.error('[Store] 获取用户信息失败:', error)
      // 请求失败也清除本地状态（可能是未登录或网络问题）
      clearLoginUser()
    }
  }

  function syncUserRoleFields(user: any) {
    if (!user) return
    
    if (user.role === 'ADMIN' && !user.userRole) {
      user.userRole = 'admin'
    } else if ((user.userRole === 'admin' || user.userRole === 'ADMIN') && !user.role) {
      user.role = 'ADMIN'
    }
    
    if (!user.userName && user.userAccount) {
      user.userName = user.userAccount
    }
    
    return user
  }

  // 【核心】设置用户信息 - 三重写入保障
  function setLoginUser(newLoginUser: any) {
    console.log('[Store] ======== setLoginUser 开始 ========')
    console.log('[Store] 输入数据:', JSON.stringify({
      id: newLoginUser?.id,
      account: newLoginUser?.userAccount,
      role: newLoginUser?.role,
      userRole: newLoginUser?.userRole
    }))
    
    if (!newLoginUser || !newLoginUser.id) {
      console.error('[Store] 无效数据，取消设置')
      return
    }
    
    // 同步角色字段
    syncUserRoleFields(newLoginUser)
    
    // ========== 三重写入 ==========
    // 1️⃣ 写入 Pinia ref（Vue 响应式）
    loginUser.value = newLoginUser
    
    // 2️⃣ 写入全局变量（跨实例保障）
    globalLoginUser = newLoginUser
    
    // 3️⃣ 写入 localStorage（持久化）
    localStorage.setItem('userInfo', JSON.stringify(newLoginUser))
    
    // 验证
    console.log('[Store] 三重写入完成:')
    console.log('  [ref] loginUser.value.id =', loginUser.value?.id)
    console.log('  [global] globalLoginUser.id =', globalLoginUser?.id)
    console.log('  [localStorage] =', !!localStorage.getItem('userInfo'))
    console.log('[Store] =================================')
  }

  // 清除用户信息
  function clearLoginUser() {
    loginUser.value = { userName: '未登录' }
    globalLoginUser = null
    localStorage.removeItem('userInfo')
    console.log('[Store] 已清除登录状态')
  }

  // 【新增】获取当前用户（优先从全局变量读取）
  function getRawUser(): any {
    // 优先级：Pinia ref > 全局变量 > localStorage
    if (loginUser.value?.id) return loginUser.value
    if (globalLoginUser?.id) return globalLoginUser
    const stored = localStorage.getItem('userInfo')
    if (stored) {
      try {
        return JSON.parse(stored)
      } catch (e) { /* ignore */ }
    }
    return null
  }

  return {
    loginUser,
    globalLoginUser: () => globalLoginUser, // 暴露只读访问
    getRawUser,
    fetchLoginUser,
    setLoginUser,
    clearLoginUser
  }
})

// ========== 【保障2】导出独立的全局读取函数 ==========
// 不依赖 Pinia 实例，任何地方都能调用
export function getGlobalCurrentUser(): any {
  if (globalLoginUser?.id) return globalLoginUser
  const stored = localStorage.getItem('userInfo')
  if (stored) {
    try { return JSON.parse(stored) } catch(e) { /* ignore */ }
  }
  return null
}
