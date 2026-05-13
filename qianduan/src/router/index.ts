import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import CourseView from '@/views/CourseView.vue'
import UserCenter from '@/views/UserCenter.vue'
import MaterialManage from '@/pages/admin/MaterialManage.vue'
import UserManage from '@/pages/admin/UserManage.vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'home',
    component: HomeView
  },
  {
    path: '/course',
    name: 'course',
    component: CourseView
  },
  {
    path: '/user-center',
    name: 'user-center',
    component: UserCenter
  },
  {
    path: '/admin/material-manage',
    name: 'admin-material-manage',
    component: MaterialManage
  },
  {
    path: '/admin/user-manage',
    name: 'admin-user-manage',
    component: UserManage
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
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

export default router
