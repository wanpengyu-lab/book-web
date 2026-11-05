/** 认证 · POST /api/auth/**（契约 §1，开放接口） */
import { request } from './http'
import type { LoginData } from './types'

export function login(username: string, password: string): Promise<LoginData> {
  return request<LoginData>('POST', '/api/auth/login', { body: { username, password } })
}

export function register(username: string, password: string): Promise<boolean> {
  return request<boolean>('POST', '/api/auth/register', { body: { username, password } })
}
