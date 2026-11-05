/** 用户会话 store：token/user（localStorage 持久）+ 站内通知与红点 */
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import * as notificationsApi from '@/api/notifications'
import { clearSession, getSessionUser, getToken, setSession } from '@/api/token'
import type { AppNotification, UserInfo } from '@/api/types'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(getToken())
  const user = ref<UserInfo | null>(getSessionUser())
  const isLoggedIn = computed(() => token.value !== null)

  const notifications = ref<AppNotification[]>([])
  const unreadCount = computed(() => notifications.value.filter((n) => !n.read).length)

  async function login(username: string, password: string): Promise<void> {
    const data = await authApi.login(username, password)
    token.value = data.token
    user.value = data.user
    setSession(data.token, data.user)
    await refreshNotifications()
  }

  function logout(): void {
    clearSession()
    token.value = null
    user.value = null
    notifications.value = []
  }

  async function refreshNotifications(): Promise<void> {
    if (!token.value) return
    try {
      const page = await notificationsApi.list({ page: 1, size: 20 })
      notifications.value = page.list
    } catch {
      // 通知加载失败不打断主流程（红点静默降级）
    }
  }

  return { token, user, isLoggedIn, notifications, unreadCount, login, logout, refreshNotifications }
})
