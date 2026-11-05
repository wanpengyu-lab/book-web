/**
 * 状态标签唯一产出源（PRD v0.8 强制规范）：
 * 在馆 / 在借 / 逾期 / 预约中 四个状态标签一律经 StatusTag 渲染，
 * 其他地方只允许引用这里的 kind 与文案，禁止自造第二套状态样式。
 */
import type { BorrowRecord } from '@/api/types'
import { isOverdueDue } from '@/lib/format'

export type StatusKind = 'available' | 'borrowed' | 'overdue' | 'reserved'

export const STATUS_META: Record<StatusKind, { label: string }> = {
  available: { label: '在馆' },
  borrowed: { label: '在借' },
  overdue: { label: '逾期' },
  reserved: { label: '预约中' },
}

/** 图书在馆/在借口径 */
export function bookStatusKind(borrowed: boolean): StatusKind {
  return borrowed ? 'borrowed' : 'available'
}

/** 借阅记录口径：active 且到期未还 → 逾期 */
export function recordStatusKind(record: BorrowRecord): StatusKind | 'returned' {
  if (record.status === 'returned') return 'returned'
  return isOverdueDue(record.dueAt) ? 'overdue' : 'borrowed'
}
