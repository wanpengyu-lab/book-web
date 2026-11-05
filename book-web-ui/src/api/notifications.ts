/** 站内通知 · GET /api/notifications（契约 §3，v0.6） */
import { request } from './http'
import type { AppNotification, NotificationQuery, Page } from './types'

export function list(query: NotificationQuery): Promise<Page<AppNotification>> {
  return request<Page<AppNotification>>('GET', '/api/notifications', { query: { ...query, size: query.size ?? 20 } })
}
