<script setup lang="ts">
/**
 * 全局顶部玻璃导航：品牌 + 面包屑 + 主导航 + 通知红点 + 主题切换 + 用户区
 */
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BellIcon, LogOutIcon, BookIcon, MoonIcon, MonitorIcon, SunIcon, UserIcon } from '@/components/icons'
import Button from '@/components/ui/button/Button.vue'
import Dialog from '@/components/ui/dialog/Dialog.vue'
import { useThemeStore, type ThemeMode } from '@/stores/theme'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/lib/format'

const route = useRoute()
const router = useRouter()
const theme = useThemeStore()
const user = useUserStore()

const crumbs = computed(() => {
  const title = (route.meta.title as string) ?? ''
  return route.name === 'home' ? [{ label: title }] : [{ label: '馆藏', to: '/' }, { label: title }]
})

const themeIcon = computed(() => {
  const iconByMode: Record<ThemeMode, unknown> = {
    dark: MoonIcon,
    light: SunIcon,
    system: MonitorIcon,
  }
  return iconByMode[theme.mode]
})

const notifOpen = ref(false)
watch(notifOpen, (open) => {
  if (open) void user.refreshNotifications()
})

function onLogout(): void {
  user.logout()
  void router.push('/login')
}
</script>

<template>
  <header class="glass fixed inset-x-0 top-0 z-40 border-b border-border">
    <div class="mx-auto flex h-14 w-full max-w-6xl items-center gap-3 px-4">
      <RouterLink to="/" class="flex items-center gap-2 text-sm font-semibold tracking-tight">
        <BookIcon class="size-4 text-primary" />
        <span>boook<span class="text-primary">·</span>web</span>
      </RouterLink>

      <!-- 面包屑（首页与主导航重复，仅次级页面展示） -->
      <nav
        v-if="route.name !== 'home'"
        class="hidden min-w-0 items-center gap-1 text-sm text-muted-foreground sm:flex"
        aria-label="面包屑"
      >
        <template v-for="(c, i) in crumbs" :key="`${c.label}-${i}`">
          <span v-if="i > 0" class="text-muted-foreground/50">/</span>
          <RouterLink v-if="c.to && i < crumbs.length - 1" :to="c.to" class="hover:text-foreground">
            {{ c.label }}
          </RouterLink>
          <span v-else class="truncate text-foreground">{{ c.label }}</span>
        </template>
      </nav>

      <nav class="ml-2 hidden items-center gap-1 text-sm md:flex">
        <RouterLink to="/" class="rounded-md px-2.5 py-1.5 text-muted-foreground hover:bg-accent hover:text-foreground" :class="{ 'text-foreground': route.name === 'home' }">
          馆藏
        </RouterLink>
        <RouterLink to="/records" class="rounded-md px-2.5 py-1.5 text-muted-foreground hover:bg-accent hover:text-foreground" :class="{ 'text-foreground': route.name === 'records' }">
          借阅记录
        </RouterLink>
        <RouterLink to="/dashboard" class="rounded-md px-2.5 py-1.5 text-muted-foreground hover:bg-accent hover:text-foreground" :class="{ 'text-foreground': route.name === 'dashboard' }">
          看板
        </RouterLink>
      </nav>

      <div class="ml-auto flex items-center gap-1.5">
        <!-- 通知红点（mock） -->
        <Button variant="ghost" size="icon" class="relative" aria-label="通知" @click="notifOpen = true">
          <BellIcon />
          <span
            v-if="user.unreadCount > 0"
            class="absolute right-1.5 top-1.5 flex size-4 items-center justify-center rounded-full bg-danger text-[10px] font-semibold leading-none text-white"
          >
            {{ user.unreadCount > 9 ? '9+' : user.unreadCount }}
          </span>
        </Button>

        <!-- 主题切换：深色 → 亮色 → 跟随系统 -->
        <Button variant="ghost" size="icon" :aria-label="`主题：${theme.mode}`" @click="theme.cycle()">
          <component :is="themeIcon" />
        </Button>

        <RouterLink
          to="/me"
          class="flex items-center gap-2 rounded-md px-2 py-1.5 text-sm text-muted-foreground hover:bg-accent hover:text-foreground"
        >
          <UserIcon class="size-4" />
          <span class="max-w-20 truncate">{{ user.user?.username }}</span>
          <span class="hidden rounded-sm bg-muted px-1.5 py-0.5 text-xs sm:inline">{{ user.user?.role }}</span>
        </RouterLink>

        <Button variant="ghost" size="icon" aria-label="退出登录" @click="onLogout">
          <LogOutIcon />
        </Button>
      </div>
    </div>
  </header>

  <Dialog v-model:open="notifOpen" title="站内通知" description="未读优先，最新在前">
    <ul class="max-h-80 space-y-2 overflow-y-auto">
      <li
        v-for="n in user.notifications"
        :key="n.id"
        class="rounded-md border border-border p-3"
        :class="{ 'border-primary/40 bg-primary/5': !n.read }"
      >
        <p class="text-sm" :class="n.read ? 'text-muted-foreground' : 'text-foreground'">
          {{ n.content }}
        </p>
        <p class="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
          <span>{{ formatDateTime(n.createdAt) }}</span>
          <span v-if="!n.read" class="inline-block size-1.5 rounded-full bg-danger" />
        </p>
      </li>
      <li v-if="user.notifications.length === 0" class="py-6 text-center text-sm text-muted-foreground">
        暂无通知
      </li>
    </ul>
  </Dialog>
</template>
