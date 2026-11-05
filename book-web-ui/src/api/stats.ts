/** 全库统计 · GET /api/stats（契约 §3，开放接口） */
import { request } from './http'
import type { StatsData } from './types'

export function get(): Promise<StatsData> {
  return request<StatsData>('GET', '/api/stats')
}
