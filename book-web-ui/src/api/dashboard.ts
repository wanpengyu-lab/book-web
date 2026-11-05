/** 看板 · GET /api/dashboard（契约 §3，登录；trend 稀疏返回，前端补零） */
import { request } from './http'
import type { DashboardData } from './types'

export function get(): Promise<DashboardData> {
  return request<DashboardData>('GET', '/api/dashboard')
}
