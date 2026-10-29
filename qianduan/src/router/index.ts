import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import { getGlobalCurrentUser } from '@/store/useUesrLogin'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'home',
    component: HomeView
  },
  {
    path: '/course',
    name: 'course',
    component: () => import('@/views/CourseView.vue')
  },
  {
    path: '/user-center',
    name: 'user-center',
    component: () => import('@/views/UserCenter.vue')
  },
  {
    path: '/admin/material-manage',
    name: 'admin-material-manage',
    component: () => import('@/pages/admin/MaterialManage.vue')
  },
  {
    path: '/admin/user-manage',
    name: 'admin-user-manage',
    component: () => import('@/pages/admin/UserManage.vue')
  },
  {
    path: '/user/login',
    name: 'user-login',
    redirect: '/'
  },
  {
    path: '/user/register',
    name: 'user-register',
    redirect: '/'
  },
  {
    path: '/exam',
    name: 'exam',
    component: () => import('@/views/ExamView.vue')
  },
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.path.startsWith('/admin/')) {
    const user = getGlobalCurrentUser()
    const isAdmin = user?.userRole === 'admin' || user?.role === 'ADMIN' || user?.userRole === 'ADMIN'
    if (!isAdmin) {
      next({ path: '/', replace: true })
      return
    }
  }
  next()
})

export default router
