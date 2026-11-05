/** 图书 · /api/books/**（契约 §2） */
import { request } from './http'
import type { Book, BookDetail, BookPayload, BookQuery, Page } from './types'

export function list(query: BookQuery): Promise<Page<Book>> {
  return request<Page<Book>>('GET', '/api/books', { query: { ...query, size: query.size ?? 5 } })
}

/** 聚合详情：书不存在时 data 为 null（契约约定 HTTP 200） */
export function detail(id: number): Promise<BookDetail | null> {
  return request<BookDetail | null>('GET', `/api/books/${id}`)
}

export function create(payload: BookPayload): Promise<boolean> {
  return request<boolean>('POST', '/api/books', { body: payload })
}

export function update(id: number, payload: BookPayload): Promise<boolean> {
  return request<boolean>('PUT', `/api/books/${id}`, { body: payload })
}

export function remove(id: number): Promise<boolean> {
  return request<boolean>('DELETE', `/api/books/${id}`)
}

export function borrow(id: number): Promise<boolean> {
  return request<boolean>('PUT', `/api/books/${id}/borrow`)
}

export function giveBack(id: number): Promise<boolean> {
  return request<boolean>('PUT', `/api/books/${id}/return`)
}

export function reserve(id: number): Promise<boolean> {
  return request<boolean>('POST', `/api/books/${id}/reserve`)
}

/** 共借推荐（登录）：id 数组；无共借退化为同分类最近入库 */
export function related(id: number): Promise<number[]> {
  return request<number[]>('GET', `/api/books/${id}/related`)
}

/** ISBN 查询（admin，契约 §4）：未命中/外部失败时 data 为 null + 明确 message */
export function isbnLookup(isbn: string): Promise<{ title: string; author: string; publisher: string; coverUrl: string; description: string } | null> {
  return request<{ title: string; author: string; publisher: string; coverUrl: string; description: string } | null>(
    'GET',
    `/api/isbn/${encodeURIComponent(isbn)}`,
  )
}
