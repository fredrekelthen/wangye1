import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'

import Antd from 'ant-design-vue';

import 'ant-design-vue/dist/reset.css';

// 全局抑制 ResizeObserver 循环错误（Ant Design Table 已知无害问题）
const debounce = (fn: (...args: any[]) => void, delay: number) => {
  let timer: number | null = null
  return (...args: any[]) => {
    if (timer) clearTimeout(timer)
    timer = window.setTimeout(() => fn(...args), delay)
  }
}

const _ResizeObserver = window.ResizeObserver
window.ResizeObserver = class ResizeObserver extends _ResizeObserver {
  constructor(callback: ResizeObserverCallback) {
    callback = debounce(callback, 16)
    super(callback)
  }
} as any

// 拦截该错误，阻止 webpack-dev-server overlay 显示
const originalError = console.error
console.error = (...args: any[]) => {
  if (typeof args[0] === 'string' && args[0].includes('ResizeObserver loop completed with undelivered notifications')) {
    return
  }
  originalError.call(console, ...args)
}

window.addEventListener('error', (e) => {
  if (e.message === 'ResizeObserver loop completed with undelivered notifications.') {
    e.stopImmediatePropagation()
    e.preventDefault()
  }
}, true)

window.addEventListener('unhandledrejection', (e) => {
  const reason = e.reason
  if (reason?.message === 'ResizeObserver loop completed with undelivered notifications.' ||
      (typeof reason === 'string' && reason.includes('ResizeObserver loop completed with undelivered notifications'))) {
    e.stopImmediatePropagation()
    e.preventDefault()
  }
}, true)

const pinia = createPinia()

createApp(App).use(pinia).use(Antd).use(router).mount('#app')
