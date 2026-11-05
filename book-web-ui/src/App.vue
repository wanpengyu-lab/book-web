<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import AiPanel from '@/components/AiPanel.vue'
import Toaster from '@/components/ui/toast/Toaster.vue'
import { useThemeStore } from '@/stores/theme'

// 主题初始化（深色默认 / 亮色令牌 / 跟随系统）
useThemeStore()

const route = useRoute()
const showChrome = computed(() => route.meta.public !== true)
</script>

<template>
  <div class="min-h-screen bg-background text-foreground">
    <AppHeader v-if="showChrome" />
    <main :class="showChrome ? 'mx-auto w-full max-w-6xl px-4 pb-16 pt-20' : 'min-h-screen'">
      <!-- 路由过渡：fade + 8px，180ms（动效 3，CSS 侧尊重 prefers-reduced-motion） -->
      <RouterView v-slot="{ Component }">
        <Transition name="route" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>
    <Toaster />
    <AiPanel v-if="showChrome" />
  </div>
</template>
