/** AI 馆藏助手 · POST /api/assistant/chat（契约 §4，登录；非流式） */
import { request } from './http'
import type { AssistantReply } from './types'

export function chat(message: string): Promise<AssistantReply | null> {
  return request<AssistantReply | null>('POST', '/api/assistant/chat', { body: { message } })
}
