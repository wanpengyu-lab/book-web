/** 借阅记录 · GET /api/records（契约 §3，拍板项 b：带 bookTitle/bookAuthor） */
import { request } from './http'
import type { BorrowRecord, Page, RecordQuery } from './types'

export function list(query: RecordQuery): Promise<Page<BorrowRecord>> {
  return request<Page<BorrowRecord>>('GET', '/api/records', { query: { ...query, size: query.size ?? 10 } })
}
