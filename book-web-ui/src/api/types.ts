/**
 * 前端类型 · 与 docs/api-contract.md（v0.7 冻结契约）逐项对齐
 * mock（B 阶段）与真实 API（C 阶段）共用同一套签名
 */

/** 统一响应（字段顺序固定 code, message, data） */
export interface Result<T> {
  code: number
  message: string
  data: T
}

/** 业务错误：由 http.ts 解包非 0 code 后抛出 */
export class ApiError extends Error {
  readonly code: number
  constructor(code: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

export type UserRole = 'admin' | 'reader'

export interface UserInfo {
  id: number
  username: string
  role: UserRole
}

export interface LoginData {
  token: string
  user: UserInfo
}

export interface Book {
  id: number
  title: string
  author: string
  borrowed: boolean
  category: string
  publisher: string | null
  isbn: string | null
  coverUrl: string | null
  description: string | null
  totalCopies: number
}

/** GET /api/books/{id} 聚合详情：全量字段 + 最近 5 条借阅 + 共借推荐 id */
export interface BookDetail extends Book {
  recentRecords: BorrowRecord[]
  relatedIds: number[]
}

export type RecordStatus = 'active' | 'returned'
/** 记录筛选口径：overdue 为派生态（status='active' AND due_at < NOW()） */
export type RecordStatusFilter = RecordStatus | 'overdue'

export interface BorrowRecord {
  id: number
  bookId: number
  userId: number
  borrowedAt: string
  dueAt: string
  returnedAt: string | null
  status: RecordStatus
  /** 拍板项 b：/api/records 列表带书名/作者（服务端 JOIN 取齐，前端禁止拼请求） */
  bookTitle?: string
  bookAuthor?: string
}

export interface CategoryCount {
  name: string
  count: number
}

export interface StatsData {
  total: number
  available: number
  borrowed: number
  categories: CategoryCount[]
}

/** 拍板项 a：trend 稀疏返回（仅含有借出的日期），前端补零成连续 30 天 */
export interface TrendPoint {
  date: string
  count: number
}

export interface CategoryRatio {
  name: string
  value: number
}

export interface TopBorrowed {
  id: number
  title: string
  author: string
  count: number
}

export interface DashboardData {
  trend: TrendPoint[]
  categoryRatio: CategoryRatio[]
  topBorrowed: TopBorrowed[]
}

export interface AppNotification {
  id: number
  userId: number
  content: string
  read: boolean
  createdAt: string
}

export interface Page<T> {
  total: number
  page: number
  size: number
  list: T[]
}

export interface BookQuery {
  keyword?: string
  category?: string
  page?: number
  size?: number
}

export interface RecordQuery {
  status?: RecordStatusFilter
  userId?: number
  page?: number
  size?: number
}

export interface NotificationQuery {
  page?: number
  size?: number
}

/** POST /api/books · PUT /api/books/{id} 请求体 */
export interface BookPayload {
  title: string
  author: string
  category?: string
  publisher?: string
  isbn?: string
  coverUrl?: string
  description?: string
  totalCopies?: number
}

/** GET /api/isbn/{isbn} 命中回填（仅供建档预填，不落库） */
export interface IsbnInfo {
  title: string
  author: string
  publisher: string
  coverUrl: string
  description: string
}

export interface AssistantReply {
  reply: string
}
