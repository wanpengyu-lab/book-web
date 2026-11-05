/**
 * 手写 TS mock 层（PRD 2.4 锁定：禁止 MSW / json-server / mock 服务器）。
 * 按 docs/api-contract.md（v0.7 冻结契约）同签名实现每个端点：
 * - 入参：method + path + query/body，与真实 fetch 请求一一对应
 * - 出参：统一 Result<T> 信封（含错误码信封，由 http.ts 统一解包）
 * - 固定 200ms 延迟模拟网络
 */
import type { Result } from '../types'
import { getToken } from '../token'
import type { UserInfo } from '../types'
import {
  allocBookId,
  allocNotificationId,
  allocRecordId,
  allocReservationId,
  books,
  notifications,
  records,
  reservations,
  stats,
  dashboard,
  isbnHits,
  users,
  nowIso,
  dueFromNow,
  type MockUser,
} from './data'

export interface MockRequestOptions {
  query?: Record<string, string | number | undefined>
  body?: unknown
}

/** 内部异常 → 错误码信封 */
class MockHttpError extends Error {
  readonly code: number
  constructor(code: number, message: string) {
    super(message)
    this.name = 'MockHttpError'
    this.code = code
  }
}

const delay = () => new Promise((resolve) => setTimeout(resolve, 200))

const ok = <T>(data: T, message = 'ok'): Result<T> => ({ code: 0, message, data })
const err = (code: number, message: string): Result<never> => ({
  code,
  message,
  data: null as never,
})

/* ---------------- 鉴权辅助 ---------------- */

function userFromToken(): MockUser | null {
  const token = getToken()
  if (!token) return null
  const m = token.match(/^mock-token-(\d+)$/)
  if (!m) return null
  return users.find((u) => u.id === Number(m[1])) ?? null
}

function requireUser(): MockUser {
  const u = userFromToken()
  if (!u) throw new MockHttpError(40101, '未登录或登录已过期')
  return u
}

function requireAdmin(): MockUser {
  const u = requireUser()
  if (u.role !== 'admin') throw new MockHttpError(40301, '需要 admin 权限')
  return u
}

/* ---------------- 请求体/查询参数工具 ---------------- */

interface LoginBody {
  username?: unknown
  password?: unknown
}

function asLoginBody(body: unknown): LoginBody {
  return (body ?? {}) as LoginBody
}

function asBookPayload(body: unknown): Record<string, unknown> {
  if (typeof body !== 'object' || body === null) {
    throw new MockHttpError(40001, '参数校验失败')
  }
  const payload = body as Record<string, unknown>
  if (typeof payload.title !== 'string' || !payload.title.trim()) {
    throw new MockHttpError(40001, '参数校验失败：title 必填')
  }
  if (typeof payload.author !== 'string' || !payload.author.trim()) {
    throw new MockHttpError(40001, '参数校验失败：author 必填')
  }
  return payload
}

function numParam(v: string | number | undefined, fallback: number): number {
  if (v === undefined || v === '') return fallback
  const n = Number(v)
  return Number.isFinite(n) ? n : fallback
}

/* ---------------- 端点实现 ---------------- */

function register(body: unknown): Result<boolean> {
  const { username, password } = asLoginBody(body)
  if (typeof username !== 'string' || username.trim().length < 2) {
    throw new MockHttpError(40001, '参数校验失败：用户名至少 2 个字符')
  }
  if (typeof password !== 'string' || password.length < 6) {
    throw new MockHttpError(40001, '参数校验失败：密码至少 6 位')
  }
  if (users.some((u) => u.username === username)) {
    throw new MockHttpError(40901, '用户名已存在')
  }
  const id = Math.max(...users.map((u) => u.id)) + 1
  users.push({ id, username, role: 'reader', createdAt: nowIso() })
  return ok(true)
}

function login(body: unknown): Result<{ token: string; user: UserInfo }> {
  const { username } = asLoginBody(body)
  const user = users.find((u) => u.username === username)
  if (!user) throw new MockHttpError(40101, '用户名不存在（mock 不校验密码）')
  return ok({ token: `mock-token-${user.id}`, user: { id: user.id, username: user.username, role: user.role } })
}

function listBooks(query: MockRequestOptions['query']): Result<unknown> {
  const keyword = typeof query?.keyword === 'string' ? query.keyword.trim() : ''
  const category = typeof query?.category === 'string' && query.category !== '全部' ? query.category : ''
  const page = Math.max(1, numParam(query?.page, 1))
  const size = Math.min(100, Math.max(1, numParam(query?.size, 5)))

  let list = [...books]
  if (keyword) {
    list = list.filter((b) => b.title.includes(keyword) || b.author.includes(keyword))
  }
  if (category) {
    list = list.filter((b) => b.category === category)
  }
  const total = list.length
  const slice = list.slice((page - 1) * size, page * size)
  return ok({ total, page, size, list: slice })
}

function bookDetail(id: number): Result<unknown> {
  const book = books.find((b) => b.id === id)
  if (!book) return ok(null) // 契约：书不存在 → data:null（HTTP 200）
  const recentRecords = records
    .filter((r) => r.bookId === id)
    .sort((a, b) => b.borrowedAt.localeCompare(a.borrowedAt))
    .slice(0, 5)
    .map((r) => ({ ...r }))
  const related = relatedIds(id, book.category, false)
  return ok({ ...book, recentRecords, relatedIds: related })
}

/** 共借推荐：借过该书的人还借过的书按共借次数降序前 6；无共借退化同分类最近入库（排除自身） */
function relatedIds(id: number, category: string, requireLogin: boolean): number[] {
  if (requireLogin) requireUser()
  const byBook = new Map<number, number>()
  for (const r1 of records.filter((r) => r.bookId === id)) {
    for (const r2 of records.filter((r) => r.userId === r1.userId && r.bookId !== id)) {
      byBook.set(r2.bookId, (byBook.get(r2.bookId) ?? 0) + 1)
    }
  }
  const coBorrow = [...byBook.entries()]
    .sort((a, b) => b[1] - a[1] || a[0] - b[0])
    .map(([bookId]) => bookId)
    .slice(0, 6)
  if (coBorrow.length > 0) return coBorrow
  return books
    .filter((b) => b.id !== id && b.category === category)
    .sort((a, b) => b.id - a.id)
    .slice(0, 6)
    .map((b) => b.id)
}

function createBook(body: unknown): Result<boolean> {
  requireAdmin()
  const payload = asBookPayload(body)
  const id = allocBookId()
  books.push({
    id,
    title: String(payload.title).trim(),
    author: String(payload.author).trim(),
    borrowed: false,
    category: typeof payload.category === 'string' && payload.category.trim() ? payload.category.trim() : '未分类',
    publisher: typeof payload.publisher === 'string' ? payload.publisher : null,
    isbn: typeof payload.isbn === 'string' ? payload.isbn : null,
    coverUrl: typeof payload.coverUrl === 'string' ? payload.coverUrl : null,
    description: typeof payload.description === 'string' ? payload.description : null,
    totalCopies: typeof payload.totalCopies === 'number' && payload.totalCopies >= 1 ? payload.totalCopies : 1,
  })
  return ok(true)
}

function updateBook(id: number, body: unknown): Result<boolean> {
  requireAdmin()
  const book = books.find((b) => b.id === id)
  if (!book) throw new MockHttpError(40401, '图书不存在')
  const payload = asBookPayload(body)
  book.title = String(payload.title).trim()
  book.author = String(payload.author).trim()
  if (typeof payload.category === 'string' && payload.category.trim()) book.category = payload.category.trim()
  if ('publisher' in payload) book.publisher = typeof payload.publisher === 'string' ? payload.publisher : null
  if ('isbn' in payload) book.isbn = typeof payload.isbn === 'string' ? payload.isbn : null
  if ('coverUrl' in payload) book.coverUrl = typeof payload.coverUrl === 'string' ? payload.coverUrl : null
  if ('description' in payload) book.description = typeof payload.description === 'string' ? payload.description : null
  if (typeof payload.totalCopies === 'number' && payload.totalCopies >= 1) book.totalCopies = payload.totalCopies
  return ok(true)
}

function deleteBook(id: number): Result<boolean> {
  requireAdmin()
  const idx = books.findIndex((b) => b.id === id)
  if (idx === -1) throw new MockHttpError(40401, '图书不存在')
  books.splice(idx, 1)
  return ok(true)
}

/** 借出：并发语义在 mock 中按同步流程天然互斥；借出即置在借 + 生成记录 */
function borrowBook(id: number): Result<boolean> {
  requireUser()
  const book = books.find((b) => b.id === id)
  if (!book) throw new MockHttpError(40401, '图书不存在')
  if (book.borrowed) throw new MockHttpError(40901, '该书刚被借走')
  book.borrowed = true
  records.push({
    id: allocRecordId(),
    bookId: id,
    userId: userFromToken()!.id,
    borrowedAt: nowIso(),
    dueAt: dueFromNow(),
    returnedAt: null,
    status: 'active',
  })
  // 借出时本人预约（waiting/notified）→ fulfilled（契约约定）
  for (const r of reservations) {
    if (r.bookId === id && r.userId === userFromToken()!.id && (r.status === 'waiting' || r.status === 'notified')) {
      r.status = 'fulfilled'
    }
  }
  return ok(true)
}

/** 归还：回写记录 + 释放在借标志 + 通知队首 waiting 预约 */
function returnBook(id: number): Result<boolean> {
  const user = requireUser()
  const book = books.find((b) => b.id === id)
  if (!book) throw new MockHttpError(40401, '图书不存在')
  const candidates = records.filter((r) => r.bookId === id && r.status === 'active')
  const record =
    candidates.find((r) => r.userId === user.id) ??
    (user.role === 'admin' ? candidates[0] : undefined)
  if (!record) throw new MockHttpError(40001, '该书当前没有您名下的在借记录，无法归还')
  record.returnedAt = nowIso()
  record.status = 'returned'
  book.borrowed = false
  const head = reservations
    .filter((r) => r.bookId === id && r.status === 'waiting')
    .sort((a, b) => a.createdAt.localeCompare(b.createdAt))[0]
  if (head) {
    head.status = 'notified'
    notifications.push({
      id: allocNotificationId(),
      userId: head.userId,
      content: `您预约的《${book.title}》已归还，当前可借，请及时处理`,
      read: false,
      createdAt: nowIso(),
    })
  }
  return ok(true)
}

/** 预约：在借才可约；重复预约 40901；在馆书 40001 */
function reserveBook(id: number): Result<boolean> {
  const user = requireUser()
  const book = books.find((b) => b.id === id)
  if (!book) throw new MockHttpError(40401, '图书不存在')
  if (!book.borrowed) throw new MockHttpError(40001, '在馆图书无需预约，可直接借阅')
  const duplicated = reservations.some(
    (r) => r.bookId === id && r.userId === user.id && (r.status === 'waiting' || r.status === 'notified'),
  )
  if (duplicated) throw new MockHttpError(40901, '您已预约本书，请勿重复预约')
  reservations.push({
    id: allocReservationId(),
    bookId: id,
    userId: user.id,
    status: 'waiting',
    createdAt: nowIso(),
  })
  return ok(true)
}

/** 借阅记录（登录）：reader 只看本人；status overdue 为派生态；列表带 bookTitle/bookAuthor */
function listRecords(query: MockRequestOptions['query']): Result<unknown> {
  const user = requireUser()
  const page = Math.max(1, numParam(query?.page, 1))
  const size = Math.min(100, Math.max(1, numParam(query?.size, 10)))
  const status = typeof query?.status === 'string' && query.status ? query.status : ''

  let userId = user.id
  if (user.role === 'admin' && query?.userId !== undefined && query.userId !== '') {
    userId = numParam(query.userId, user.id)
  } else if (query?.userId !== undefined && query.userId !== '' && numParam(query.userId, user.id) !== user.id) {
    throw new MockHttpError(40301, '无权限查看他人借阅记录')
  }

  let list = records.filter((r) => r.userId === userId)
  if (status === 'active') list = list.filter((r) => r.status === 'active' && new Date(r.dueAt).getTime() >= Date.now())
  else if (status === 'overdue') list = list.filter((r) => r.status === 'active' && new Date(r.dueAt).getTime() < Date.now())
  else if (status === 'returned') list = list.filter((r) => r.status === 'returned')

  list = [...list].sort((a, b) => b.borrowedAt.localeCompare(a.borrowedAt))
  const joined = list.map((r) => {
    const book = books.find((b) => b.id === r.bookId)
    return { ...r, bookTitle: book?.title ?? `#${r.bookId}`, bookAuthor: book?.author ?? '' }
  })
  const total = joined.length
  const slice = joined.slice((page - 1) * size, page * size)
  return ok({ total, page, size, list: slice })
}

/** 站内通知（登录）：未读优先，同状态最新在前 */
function listNotifications(query: MockRequestOptions['query']): Result<unknown> {
  const user = requireUser()
  const page = Math.max(1, numParam(query?.page, 1))
  const size = Math.min(100, Math.max(1, numParam(query?.size, 20)))
  const list = notifications
    .filter((n) => n.userId === user.id)
    .sort((a, b) => Number(a.read) - Number(b.read) || b.createdAt.localeCompare(a.createdAt))
  const total = list.length
  const slice = list.slice((page - 1) * size, page * size)
  return ok({ total, page, size, list: slice })
}

/** AI 馆藏助手（登录）：回答基于真实馆藏（search_books 语义），查不到答「馆内没有」 */
function assistantChat(body: unknown): Result<{ reply: string } | null> {
  requireUser()
  const payload = (body ?? {}) as { message?: unknown }
  if (typeof payload.message !== 'string' || !payload.message.trim()) {
    throw new MockHttpError(40001, '参数校验失败：message 必填')
  }
  const message = payload.message.trim()
  const keyword = message.replace(/[馆里有哪些有本借阅状态吗么呢？?？\s]/g, '')
  const matches = books
    .filter((b) => b.title.includes(keyword) || b.author.includes(keyword) || b.category.includes(keyword))
    .slice(0, 6)

  if (matches.length === 0) {
    return ok({ reply: `馆内没有找到与「${message}」相关的图书。您可以换个关键词，例如问我"馆里有哪些科幻小说"。` })
  }
  const lines = matches.map((b) => `《${b.title}》— ${b.author}（${b.category}，${b.borrowed ? '在借' : '在馆'}）`)
  return ok({
    reply: `根据当前馆藏，为您找到 ${matches.length} 本相关图书：\n${lines.join('\n')}\n需要查询某本书的借阅状态，可以直接告诉我书名。`,
  })
}

/** ISBN 查询（admin）：命中样例表回填；未命中按契约 200 + data:null + message */
function isbnLookup(isbn: string): Result<unknown> {
  requireAdmin()
  const hit = isbnHits[isbn]
  if (!hit) return ok(null, '未查到该书目，请手动录入')
  return ok(hit)
}

/* ---------------- 路由分发（与契约路径一一对应） ---------------- */

export async function mockRequest<T>(
  method: string,
  path: string,
  options: MockRequestOptions = {},
): Promise<Result<T>> {
  await delay()
  const query = options.query
  const body = options.body
  try {
    if (method === 'POST' && path === '/api/auth/register') return register(body) as Result<T>
    if (method === 'POST' && path === '/api/auth/login') return login(body) as Result<T>
    if (method === 'POST' && path === '/api/books') return createBook(body) as Result<T>
    if (method === 'GET' && path === '/api/stats') return ok(stats()) as Result<T>
    if (method === 'GET' && path === '/api/dashboard') return ok(dashboard()) as Result<T>
    if (method === 'GET' && path === '/api/books') return listBooks(query) as Result<T>
    if (method === 'GET' && path === '/api/records') return listRecords(query) as Result<T>
    if (method === 'GET' && path === '/api/notifications') return listNotifications(query) as Result<T>
    if (method === 'POST' && path === '/api/assistant/chat') return assistantChat(body) as Result<T>

    let m = path.match(/^\/api\/books\/(\d+)$/)
    if (m) {
      const id = Number(m[1])
      if (method === 'GET') return bookDetail(id) as Result<T>
      if (method === 'PUT') return updateBook(id, body) as Result<T>
      if (method === 'DELETE') return deleteBook(id) as Result<T>
    }
    m = path.match(/^\/api\/books\/(\d+)\/(borrow|return|reserve|related)$/)
    if (m) {
      const id = Number(m[1])
      if (method === 'PUT' && m[2] === 'borrow') return borrowBook(id) as Result<T>
      if (method === 'PUT' && m[2] === 'return') return returnBook(id) as Result<T>
      if (method === 'POST' && m[2] === 'reserve') return reserveBook(id) as Result<T>
      if (method === 'GET' && m[2] === 'related') {
        const book = books.find((b) => b.id === id)
        if (!book) throw new MockHttpError(40401, '图书不存在')
        return ok(relatedIds(id, book.category, true)) as Result<T>
      }
    }
    m = path.match(/^\/api\/isbn\/(\d{1,20})$/)
    if (m && method === 'GET') return isbnLookup(m[1]) as Result<T>
    if (path.startsWith('/api/isbn/') && method === 'GET') {
      throw new MockHttpError(40001, '参数校验失败：ISBN 需为 1–20 位纯数字')
    }

    throw new MockHttpError(40401, `接口不存在：${method} ${path}`)
  } catch (e) {
    if (e instanceof MockHttpError) return err(e.code, e.message) as Result<T>
    return err(50001, '服务器错误') as Result<T>
  }
}
