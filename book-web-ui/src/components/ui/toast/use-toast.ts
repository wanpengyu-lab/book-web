/**
 * shadcn-vue Toast 状态源（模块级共享，供任意组件直接 toast()）
 */
import { reactive } from 'vue'

export interface ToastInput {
  title?: string
  description?: string
  variant?: 'default' | 'destructive'
}

export interface ToastItem extends ToastInput {
  id: number
}

export const toastState = reactive<{ toasts: ToastItem[] }>({ toasts: [] })

let seq = 0

export function dismissToast(id: number): void {
  const idx = toastState.toasts.findIndex((t) => t.id === id)
  if (idx >= 0) toastState.toasts.splice(idx, 1)
}

export function toast(input: ToastInput): void {
  const id = ++seq
  toastState.toasts.push({ id, ...input })
  // 5s 自动消失（与 v0.9 交互规范一致）
  setTimeout(() => dismissToast(id), 5000)
}

export function toastError(message: string, title = '操作失败'): void {
  toast({ title, description: message, variant: 'destructive' })
}

export function useToast() {
  return { toastState, toast, toastError, dismissToast }
}
