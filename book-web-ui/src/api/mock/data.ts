/**
 * mock 数据集（PRD 2.4 锁定）：
 * - ≥12 本书：覆盖全部分类与在借/在馆状态
 * - 3 个用户：含 admin
 * - ≥8 条借阅记录：含逾期与已还样例
 * - 看板样例 + AI 问答样例（由数据动态生成，符合"回答基于真实馆藏"约束）
 *
 * 状态为内存可变数据：借/还/预约操作会真实流转，供页面自查。
 * 说明：mock 登录不校验、不存储任何密码（避免明文凭据落库），
 * 用户名存在即签发 mock token —— 仅限 B 阶段 mock 使用。
 */
import type {
  AppNotification,
  Book,
  BorrowRecord,
  CategoryRatio,
  TopBorrowed,
  TrendPoint,
  UserInfo,
} from '../types'

export interface MockUser extends UserInfo {
  createdAt: string
}

export interface MockReservation {
  id: number
  bookId: number
  userId: number
  status: 'waiting' | 'notified' | 'fulfilled' | 'cancelled'
  createdAt: string
}

/** 以 2026-11-06（契约示例同日）为基准构造样例 */
export const users: MockUser[] = [
  { id: 1, username: 'admin', role: 'admin', createdAt: '2026-06-01T09:00:00' },
  { id: 2, username: 'zhangsan', role: 'reader', createdAt: '2026-06-15T09:00:00' },
  { id: 3, username: 'lisi', role: 'reader', createdAt: '2026-07-02T09:00:00' },
]

export const books: Book[] = [
  { id: 1, title: '三体', author: '刘慈欣', borrowed: true, category: '科幻', publisher: '重庆出版社', isbn: '9787536692930', coverUrl: null, description: '文化大革命期间，一次绝密的星际发射改变了人类的命运，三体文明与地球文明的博弈就此展开。', totalCopies: 3 },
  { id: 2, title: '三体Ⅱ：黑暗森林', author: '刘慈欣', borrowed: false, category: '科幻', publisher: '重庆出版社', isbn: '9787536692931', coverUrl: null, description: null, totalCopies: 2 },
  { id: 3, title: '百年孤独', author: '加西亚·马尔克斯', borrowed: true, category: '文学', publisher: '南海出版公司', isbn: '9787544253994', coverUrl: null, description: '布恩迪亚家族七代人的传奇故事，魔幻现实主义文学的代表作。', totalCopies: 2 },
  { id: 4, title: '红楼梦', author: '曹雪芹', borrowed: false, category: '文学', publisher: '人民文学出版社', isbn: '9787020002207', coverUrl: null, description: null, totalCopies: 3 },
  { id: 5, title: '深入理解计算机系统', author: 'Randal E. Bryant / David R. O’Hallaron', borrowed: false, category: '计算机', publisher: '机械工业出版社', isbn: '9787111544937', coverUrl: null, description: null, totalCopies: 2 },
  { id: 6, title: '代码整洁之道', author: 'Robert C. Martin', borrowed: true, category: '计算机', publisher: '人民邮电出版社', isbn: '9787115216878', coverUrl: null, description: '软件大师 Clean Code 先生的程序员专业修炼指南。', totalCopies: 1 },
  { id: 7, title: '人类简史：从动物到上帝', author: '尤瓦尔·赫拉利', borrowed: false, category: '历史', publisher: '中信出版社', isbn: '9787508647357', coverUrl: null, description: null, totalCopies: 2 },
  { id: 8, title: '万历十五年', author: '黄仁宇', borrowed: false, category: '历史', publisher: '生活·读书·新知三联书店', isbn: '9787108010391', coverUrl: null, description: null, totalCopies: 1 },
  { id: 9, title: '艺术的故事', author: '贡布里希', borrowed: false, category: '艺术', publisher: '广西美术出版社', isbn: '9787549410407', coverUrl: null, description: null, totalCopies: 1 },
  { id: 10, title: '霍乱时期的爱情', author: '加西亚·马尔克斯', borrowed: false, category: '文学', publisher: '南海出版公司', isbn: '9787544258975', coverUrl: null, description: null, totalCopies: 1 },
  { id: 11, title: '球状闪电', author: '刘慈欣', borrowed: false, category: '科幻', publisher: '四川科学技术出版社', isbn: '9787536458147', coverUrl: null, description: null, totalCopies: 1 },
  { id: 12, title: '城南旧事', author: '林海音', borrowed: false, category: '未分类', publisher: '中国青年出版社', isbn: null, coverUrl: null, description: null, totalCopies: 1 },
  { id: 13, title: '算法导论', author: 'Thomas H. Cormen 等', borrowed: true, category: '计算机', publisher: '人民邮电出版社', isbn: '9787115287584', coverUrl: null, description: null, totalCopies: 2 },
]

/** 含逾期（record 1：due 2026-10-31 已过期仍在借）与已还样例 */
export const records: BorrowRecord[] = [
  { id: 1, bookId: 1, userId: 2, borrowedAt: '2026-10-01T09:30:00', dueAt: '2026-10-31T09:30:00', returnedAt: null, status: 'active' },
  { id: 2, bookId: 3, userId: 3, borrowedAt: '2026-10-20T14:00:00', dueAt: '2026-11-19T14:00:00', returnedAt: null, status: 'active' },
  { id: 3, bookId: 6, userId: 2, borrowedAt: '2026-10-10T10:00:00', dueAt: '2026-11-09T10:00:00', returnedAt: null, status: 'active' },
  { id: 4, bookId: 13, userId: 3, borrowedAt: '2026-10-25T19:00:00', dueAt: '2026-11-24T19:00:00', returnedAt: null, status: 'active' },
  { id: 5, bookId: 6, userId: 3, borrowedAt: '2026-09-10T09:00:00', dueAt: '2026-10-10T09:00:00', returnedAt: '2026-10-08T15:00:00', status: 'returned' },
  { id: 6, bookId: 1, userId: 3, borrowedAt: '2026-08-01T09:00:00', dueAt: '2026-08-31T09:00:00', returnedAt: '2026-08-30T10:00:00', status: 'returned' },
  { id: 7, bookId: 2, userId: 2, borrowedAt: '2026-08-10T09:00:00', dueAt: '2026-09-09T09:00:00', returnedAt: '2026-09-01T11:00:00', status: 'returned' },
  { id: 8, bookId: 8, userId: 3, borrowedAt: '2026-09-15T09:00:00', dueAt: '2026-10-15T09:00:00', returnedAt: '2026-10-14T09:00:00', status: 'returned' },
  { id: 9, bookId: 13, userId: 2, borrowedAt: '2026-09-20T09:00:00', dueAt: '2026-10-20T09:00:00', returnedAt: '2026-10-19T09:00:00', status: 'returned' },
  { id: 10, bookId: 1, userId: 2, borrowedAt: '2026-07-05T09:00:00', dueAt: '2026-08-04T09:00:00', returnedAt: '2026-07-20T09:00:00', status: 'returned' },
  { id: 11, bookId: 5, userId: 2, borrowedAt: '2026-10-28T09:00:00', dueAt: '2026-11-27T09:00:00', returnedAt: '2026-11-02T09:00:00', status: 'returned' },
  { id: 12, bookId: 7, userId: 3, borrowedAt: '2026-10-30T09:00:00', dueAt: '2026-11-29T09:00:00', returnedAt: '2026-11-04T09:00:00', status: 'returned' },
]

export const reservations: MockReservation[] = [
  { id: 1, bookId: 1, userId: 3, status: 'waiting', createdAt: '2026-10-15T09:00:00' },
]

export const notifications: AppNotification[] = [
  { id: 1, userId: 3, content: '您预约的《三体》当前仍被借出，归还后将第一时间通知您', read: false, createdAt: '2026-10-15T09:00:01' },
  { id: 2, userId: 3, content: '您借阅的《百年孤独》应还日期为 2026-11-19，请按时归还', read: false, createdAt: '2026-11-04T09:00:00' },
  { id: 3, userId: 2, content: '您借阅的《三体》已逾期，请尽快归还', read: true, createdAt: '2026-11-01T09:00:00' },
  { id: 4, userId: 2, content: '欢迎使用 boook-web 图书管理系统', read: true, createdAt: '2026-07-01T09:00:00' },
]

/** ISBN 查询命中样例（仅供建档预填） */
export const isbnHits: Record<string, { title: string; author: string; publisher: string; coverUrl: string; description: string }> = {
  '9787536692930': {
    title: '三体',
    author: '刘慈欣',
    publisher: '重庆出版社',
    coverUrl: '',
    description: '文化大革命期间，一次绝密的星际发射改变了人类的命运。',
  },
  '9787111544937': {
    title: '深入理解计算机系统',
    author: 'Randal E. Bryant / David R. O’Hallaron',
    publisher: '机械工业出版社',
    coverUrl: '',
    description: '从程序员的视角详细阐述计算机系统的本质概念。',
  },
}

/* ---------------- 自增 id 与统计派生（数据流转后实时重算） ---------------- */

let nextBookId = 14
let nextRecordId = 13
let nextReservationId = 2
let nextNotificationId = 5

export function allocBookId(): number {
  return nextBookId++
}
export function allocRecordId(): number {
  return nextRecordId++
}
export function allocReservationId(): number {
  return nextReservationId++
}
export function allocNotificationId(): number {
  return nextNotificationId++
}

export function stats(): { total: number; available: number; borrowed: number; categories: { name: string; count: number }[] } {
  const total = books.length
  const borrowed = books.filter((b) => b.borrowed).length
  const byName = new Map<string, number>()
  for (const b of books) byName.set(b.category, (byName.get(b.category) ?? 0) + 1)
  const categories = [...byName.entries()]
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count)
  return { total, available: total - borrowed, borrowed, categories }
}

/** 看板：trend 近 30 天稀疏（拍板项 a，前端补零）；categoryRatio 与 topBorrowed 来自聚合 */
export function dashboard(): { trend: TrendPoint[]; categoryRatio: CategoryRatio[]; topBorrowed: TopBorrowed[] } {
  const DAY = 24 * 60 * 60 * 1000
  const today = new Date()
  const windowStart = today.getTime() - 29 * DAY
  const byDate = new Map<string, number>()
  for (const r of records) {
    const t = new Date(r.borrowedAt).getTime()
    if (t >= windowStart && t <= today.getTime() + DAY) {
      const date = r.borrowedAt.slice(0, 10)
      byDate.set(date, (byDate.get(date) ?? 0) + 1)
    }
  }
  const trend = [...byDate.entries()]
    .map(([date, count]) => ({ date, count }))
    .sort((a, b) => a.date.localeCompare(b.date))

  const ratio = stats().categories.map((c) => ({ name: c.name, value: c.count }))

  const byBook = new Map<number, number>()
  for (const r of records) byBook.set(r.bookId, (byBook.get(r.bookId) ?? 0) + 1)
  const topBorrowed = [...byBook.entries()]
    .map(([id, count]) => {
      const b = books.find((x) => x.id === id)
      return { id, title: b?.title ?? `#${id}`, author: b?.author ?? '', count }
    })
    .sort((a, b) => b.count - a.count || a.id - b.id)
    .slice(0, 10)

  return { trend, categoryRatio: ratio, topBorrowed }
}

export function nowIso(): string {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

/** mock 借期：与后端 BORROW_DAYS 默认 30 天对齐 */
export const BORROW_DAYS = 30

export function dueFromNow(): string {
  const d = new Date(Date.now() + BORROW_DAYS * 24 * 60 * 60 * 1000)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}
