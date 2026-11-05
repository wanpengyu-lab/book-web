/**
 * 原生 fetch 封装（PRD 2.2 锁定：禁止 axios）。
 * - 自动带 Authorization: Bearer <token>
 * - 统一解包 Result<T>：code===0 返回 data，否则抛 ApiError
 * - 401（40101）：清会话并跳 /login（B 阶段对 mock 生效）
 * - VITE_USE_MOCK=true（B 阶段锁定）时全部流量走手写 mock 层
 */
import { ApiError, type Result } from './types'
import { clearSession, getToken } from './token'
import { mockRequest } from './mock'

/** B 阶段 .env 固定 VITE_USE_MOCK=true；C 阶段以 .env.production 覆盖为 false */
export const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

export interface RequestOptions {
  query?: Record<string, string | number | undefined>
  body?: unknown
}

function buildUrl(path: string, query: RequestOptions['query']): string {
  if (!query) return path
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== '') params.set(key, String(value))
  }
  const qs = params.toString()
  return qs ? `${path}?${qs}` : path
}

async function fetchResult(method: string, path: string, options: RequestOptions): Promise<Result<unknown>> {
  const headers: Record<string, string> = {}
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'

  const response = await fetch(buildUrl(path, options.query), {
    method,
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  })
  try {
    return (await response.json()) as Result<unknown>
  } catch {
    throw new ApiError(50001, '服务器响应异常，请稍后重试')
  }
}

export async function request<T>(method: string, path: string, options: RequestOptions = {}): Promise<T> {
  let result: Result<unknown>
  try {
    result = USE_MOCK
      ? await mockRequest<unknown>(method, path, options)
      : await fetchResult(method, path, options)
  } catch (e) {
    if (e instanceof ApiError) throw e
    throw new ApiError(50001, '网络异常，请稍后重试')
  }

  if (result.code === 0) return result.data as T

  if (result.code === 40101) {
    // v0.10 修复（E2E 预检）：除 localStorage 外必须同步重置 pinia 会话状态，
    // 否则 isLoggedIn 仍为 true，登录页守卫会把用户弹回 /（僵尸登录态），401 落不到 /login。
    // logout() 为纯前端清理（清 store + clearSession），动态 import 避免与 stores 的模块循环。
    clearSession()
    const { useUserStore } = await import('@/stores/user')
    useUserStore().logout()
    const { default: router } = await import('@/router')
    void router.push('/login')
  }
  throw new ApiError(result.code, result.message)
}
