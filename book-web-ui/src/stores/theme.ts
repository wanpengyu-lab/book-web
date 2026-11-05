/** 主题 store：深色默认 · 令牌切亮色 · 跟随 prefers-color-scheme（PRD 2.2 锁定） */
import { ref, watchEffect } from 'vue'
import { defineStore } from 'pinia'

export type ThemeMode = 'dark' | 'light' | 'system'

const STORAGE_KEY = 'bw-theme'
const ORDER: ThemeMode[] = ['dark', 'light', 'system']

function loadMode(): ThemeMode {
  const raw = localStorage.getItem(STORAGE_KEY)
  return raw === 'light' || raw === 'system' ? raw : 'dark'
}

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>(loadMode())

  function apply(): void {
    const dark =
      mode.value === 'dark' ||
      (mode.value === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches)
    document.documentElement.classList.toggle('dark', dark)
  }

  function setMode(next: ThemeMode): void {
    mode.value = next
    localStorage.setItem(STORAGE_KEY, next)
  }

  /** 依次循环 深色 → 亮色 → 跟随系统 */
  function cycle(): void {
    const idx = ORDER.indexOf(mode.value)
    setMode(ORDER[(idx + 1) % ORDER.length]!)
  }

  watchEffect(() => {
    apply()
    const mq = window.matchMedia('(prefers-color-scheme: dark)')
    const onChange = () => {
      if (mode.value === 'system') apply()
    }
    mq.addEventListener('change', onChange)
  })

  return { mode, setMode, cycle }
})
