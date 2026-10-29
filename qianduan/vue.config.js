const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  productionSourceMap: false,

  devServer: {
    // "ResizeObserver loop completed with undelivered notifications" 是浏览器对
    // 布局抖动的良性告警（ant-design-vue 的表格/滚动组件常触发），并非真实错误。
    // 但 dev-server 的 overlay 会把它当成未捕获运行时错误全屏弹出，这里按内容过滤。
    // 注意：该函数会被序列化后下发到浏览器执行，不能引用外部变量。
    client: {
      overlay: {
        errors: true,
        warnings: false,
        runtimeErrors: error => {
          const message = (error && error.message) || ''
          return message.indexOf('ResizeObserver loop') === -1
        }
      }
    },

    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },

  chainWebpack: config => {
    // 关闭首屏 prefetch，避免与关键资源抢带宽
    config.plugins.delete('prefetch')

    // 精细化分包
    config.optimization.splitChunks({
      cacheGroups: {
        antdVendor: {
          test: /[\\/]node_modules[\\/]ant-design-vue[\\/]/,
          name: 'chunk-antd',
          chunks: 'all',
          priority: 20
        },
        g6Vendor: {
          test: /[\\/]node_modules[\\/]@antv[\\/]g6[\\/]/,
          name: 'chunk-g6',
          chunks: 'all',
          priority: 20
        },
        echartsVendor: {
          test: /[\\/]node_modules[\\/]echarts[\\/]/,
          name: 'chunk-echarts',
          chunks: 'all',
          priority: 20
        }
      }
    })
  }
})
