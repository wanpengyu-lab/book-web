<script setup lang="ts">
/**
 * AI 馆藏助手侧栏（PRD v0.9）：
 * 全局悬浮按钮 + backdrop-blur 面板；mock 问答（走 assistant api → mock 层）
 * + 本地打字机效果 + 回答内嵌馆藏链接（跳图书详情）。
 */
import { computed, nextTick, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import * as assistantApi from '@/api/assistant'
import * as booksApi from '@/api/books'
import { USE_MOCK } from '@/api/http'
import { ApiError, type Book } from '@/api/types'
import Button from '@/components/ui/button/Button.vue'
import Input from '@/components/ui/input/Input.vue'
import { SendIcon, SparklesIcon, XIcon } from '@/components/icons'
import { prefersReducedMotion } from '@/lib/media'
import { toastError } from '@/components/ui/toast/use-toast'

interface ChatMessage {
  role: 'user' | 'assistant'
  text: string
}

const open = ref(false)
const input = ref('')
const waiting = ref(false)

const messages = ref<ChatMessage[]>([])

/** 打字机：最近一条 assistant 消息已显示的字符数 */
const typingIndex = ref(-1)
const typedCount = ref(0)
let typingTimer: ReturnType<typeof setInterval> | undefined

/** 馆藏标题 → id 映射（面板打开时拉一次列表，用于回答回填详情链接） */
const bookIdByTitle = ref<Map<string, Book>>(new Map())

watch(open, async (value) => {
  if (!value) return
  try {
    const page = await booksApi.list({ page: 1, size: 100 })
    bookIdByTitle.value = new Map(page.list.map((b) => [b.title, b]))
  } catch {
    // 链接映射失败不影响问答
  }
  await nextTick()
  scrollToBottom()
})

const canSend = computed(() => input.value.trim().length > 0 && !waiting.value)

const SUGGESTIONS = ['馆里有哪些科幻小说', '《三体》能借吗', '帮我找找马尔克斯的书']

async function send(text?: string): Promise<void> {
  const message = (text ?? input.value).trim()
  if (!message || waiting.value) return
  input.value = ''
  messages.value.push({ role: 'user', text: message })
  waiting.value = true
  scrollToBottom()
  try {
    const data = await assistantApi.chat(message)
    if (!data) {
      messages.value.push({ role: 'assistant', text: 'AI 服务暂时不可用，请稍后重试。' })
      return
    }
    const reply = data.reply
    messages.value.push({ role: 'assistant', text: reply })
    startTypewriter(messages.value.length - 1)
  } catch (e) {
    toastError(e instanceof ApiError ? e.message : 'AI 服务调用失败', '馆藏助手')
  } finally {
    waiting.value = false
    scrollToBottom()
  }
}

function startTypewriter(index: number): void {
  if (prefersReducedMotion()) {
    typingIndex.value = -1
    return
  }
  const full = messages.value[index]?.text ?? ''
  typingIndex.value = index
  typedCount.value = 0
  clearInterval(typingTimer)
  typingTimer = setInterval(() => {
    typedCount.value += 1
    scrollToBottom()
    if (typedCount.value >= full.length) {
      clearInterval(typingTimer)
      typingTimer = undefined
      typingIndex.value = -1
    }
  }, 18)
}

/** 把回复文本切成 文本 / 馆藏链接 段（《书名》→ 跳详情），clip 控制打字机进度 */
interface Segment {
  type: 'text' | 'link'
  content: string
  bookId?: number
}

function segmentsOf(text: string, clip: number): Segment[] {
  const parts = text.split(/(《[^》]+》)/g)
  const segments: Segment[] = []
  let used = 0
  for (const part of parts) {
    if (used >= clip) break
    const remain = clip - used
    const content = part.length > remain ? part.slice(0, remain) : part
    used += Math.min(part.length, remain)
    if (!content) continue
    const m = part.match(/^《(.+)》$/)
    const book = m ? bookIdByTitle.value.get(m[1] ?? '') : undefined
    segments.push(
      book
        ? { type: 'link', content: `《${m![1]}》`, bookId: book.id }
        : { type: 'text', content },
    )
  }
  return segments
}

const listRef = ref<HTMLDivElement | null>(null)

function scrollToBottom(): void {
  requestAnimationFrame(() => {
    listRef.value?.scrollTo({ top: listRef.value.scrollHeight })
  })
}
</script>

<template>
  <!-- 悬浮按钮（左下角，避开右下角 toast 堆叠区） -->
  <Button
    size="icon"
    class="fixed bottom-5 left-5 z-50 size-12 rounded-full max-md:size-14"
    :aria-label="open ? '关闭馆藏助手' : '打开馆藏助手'"
    @click="open = !open"
  >
    <XIcon v-if="open" />
    <SparklesIcon v-else />
  </Button>

  <!-- 侧栏面板 -->
  <Transition name="panel">
    <section
      v-if="open"
      class="glass fixed bottom-20 left-5 z-50 flex h-[480px] w-96 max-w-[calc(100vw-2.5rem)] flex-col rounded-lg border border-border bg-card/85 shadow-hover"
      aria-label="AI 馆藏助手"
    >
      <header class="flex items-center gap-2 border-b border-border px-4 py-3">
        <SparklesIcon class="size-4 text-primary" />
        <h2 class="text-sm font-semibold">AI 馆藏助手</h2>
        <!-- v0.10：mock 徽标仅离线 mock 模式显示，真实 API 模式不得误导（E2E 预检修复） -->
        <span v-if="USE_MOCK" class="ml-auto rounded-sm bg-muted px-1.5 py-0.5 text-xs text-muted-foreground">mock</span>
      </header>

      <!-- 消息区 -->
      <div ref="listRef" class="flex-1 space-y-3 overflow-y-auto px-4 py-3">
        <div v-if="messages.length === 0" class="space-y-3 pt-4 text-center">
          <p class="text-sm text-muted-foreground">基于真实馆藏回答，查不到的书会告诉你「馆内没有」</p>
          <div class="flex flex-wrap justify-center gap-1.5">
            <Button
              v-for="s in SUGGESTIONS"
              :key="s"
              size="sm"
              variant="secondary"
              class="rounded-full"
              @click="send(s)"
            >
              {{ s }}
            </Button>
          </div>
        </div>

        <template v-for="(msg, i) in messages" :key="i">
          <div
            class="max-w-[85%] whitespace-pre-line rounded-lg px-3 py-2 text-sm leading-relaxed"
            :class="
              msg.role === 'user'
                ? 'ml-auto bg-primary text-primary-foreground'
                : 'bg-muted text-foreground'
            "
          >
            <template v-if="msg.role === 'assistant' && i === typingIndex">
              <template v-for="(seg, j) in segmentsOf(msg.text, typedCount)" :key="j">
                <RouterLink
                  v-if="seg.type === 'link'"
                  :to="`/books/${seg.bookId}`"
                  class="text-primary underline underline-offset-2"
                >{{ seg.content }}</RouterLink>
                <template v-else>{{ seg.content }}</template>
              </template>
              <span class="ml-0.5 inline-block h-3.5 w-[2px] animate-pulse bg-primary align-middle" />
            </template>
            <template v-else-if="msg.role === 'assistant'">
              <template v-for="(seg, j) in segmentsOf(msg.text, msg.text.length)" :key="j">
                <RouterLink
                  v-if="seg.type === 'link'"
                  :to="`/books/${seg.bookId}`"
                  class="text-primary underline underline-offset-2"
                >{{ seg.content }}</RouterLink>
                <template v-else>{{ seg.content }}</template>
              </template>
            </template>
            <template v-else>{{ msg.text }}</template>
          </div>
        </template>

        <div v-if="waiting" class="flex items-center gap-2 text-xs text-muted-foreground">
          <SparklesIcon class="size-3.5 animate-pulse" />
          正在检索馆藏…
        </div>
      </div>

      <!-- 输入区 -->
      <footer class="flex items-center gap-2 border-t border-border p-3">
        <Input
          v-model="input"
          placeholder="问问馆藏，如「馆里有哪些科幻小说」"
          class="flex-1"
          @keydown.enter="send()"
        />
        <Button size="icon" :disabled="!canSend" aria-label="发送" @click="send()">
          <SendIcon />
        </Button>
      </footer>
    </section>
  </Transition>
</template>

<style scoped>
@media (prefers-reduced-motion: no-preference) {
  .panel-enter-active,
  .panel-leave-active {
    transition:
      opacity 0.18s ease,
      transform 0.18s ease;
  }
  .panel-enter-from,
  .panel-leave-to {
    opacity: 0;
    transform: translateY(8px);
  }
}
</style>
