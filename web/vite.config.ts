import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      port: 5173,
      host: '0.0.0.0',
      proxy: {
        '/prod-api': {
          target: env.VITE_API_PROXY_TARGET || 'http://localhost:9999',
          changeOrigin: true,
          // 旧前端通过 Nginx 会自动剥掉 /prod-api 前缀，Vite 代理需要显式重写。
          rewrite: (path) => path.replace(/^\/prod-api/, ''),
        },
      },
    },
    build: {
      chunkSizeWarningLimit: 1000,
      rollupOptions: {
        output: {
          manualChunks: (id) => {
            if (id.includes('node_modules')) {
              if (id.includes('element-plus')) {
                return 'element-plus'
              }
              if (id.includes('vue') || id.includes('pinia') || id.includes('vue-router')) {
                return 'vue-core'
              }
              if (id.includes('echarts')) {
                return 'echarts'
              }
              return 'vendors'
            }
          },
        },
      },
    },
  }
})
