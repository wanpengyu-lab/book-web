import { fileURLToPath, URL } from 'node:url'

import tailwindcss from '@tailwindcss/vite'
import vue from '@vitejs/plugin-vue'
import { templateCompilerOptions } from '@tresjs/core'
import { defineConfig } from 'vite'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    // TresJS：自定义渲染器所需的模板编译选项（/login 3D 场景）
    vue({ ...templateCompilerOptions }),
    tailwindcss(),
    // PWA（PRD v0.9）：manifest + autoUpdate；/api/books network-first 兜底 cache（真实验证留 v0.10）
    VitePWA({
      registerType: 'autoUpdate',
      injectRegister: 'auto',
      manifest: {
        name: 'boook-web 图书管理系统',
        short_name: 'boook',
        description: '馆藏检索 · 借阅闭环 · 看板与馆藏助手',
        lang: 'zh-CN',
        theme_color: '#121216',
        background_color: '#121216',
        display: 'standalone',
        start_url: '/',
        icons: [{ src: 'favicon.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'any' }],
      },
      workbox: {
        runtimeCaching: [
          {
            urlPattern: ({ url }) => url.pathname.startsWith('/api/books'),
            handler: 'NetworkFirst',
            options: {
              cacheName: 'books-cache',
              expiration: { maxEntries: 20, maxAgeSeconds: 60 * 60 * 24 },
            },
          },
        ],
      },
    }),
  ],
  // 默认根路径；mock 预览构建用 build:preview（--base=/mock-preview/）
  base: process.env.VITE_PUBLIC_BASE || '/',
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      // C 阶段（VITE_USE_MOCK=false）启用：dev 下转发到本地后端 8080
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
