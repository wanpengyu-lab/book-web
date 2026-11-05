<script setup lang="ts">
/**
 * /books/:id 图书详情（两栏）：封面元信息 / 简介与借阅历史 + 借·还·预约按钮状态流转
 * 契约：详情一次请求取齐（recentRecords + relatedIds），前端不拼多个详情请求
 */
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import * as booksApi from '@/api/books'
import * as recordsApi from '@/api/records'
import { ApiError, type Book, type BookDetail, type BorrowRecord } from '@/api/types'
import BentoStat from '@/components/BentoStat.vue'
import RecordTag from '@/components/RecordTag.vue'
import StatusTag from '@/components/StatusTag.vue'
import Button from '@/components/ui/button/Button.vue'
import Card from '@/components/ui/card/Card.vue'
import CardContent from '@/components/ui/card/CardContent.vue'
import CardHeader from '@/components/ui/card/CardHeader.vue'
import CardTitle from '@/components/ui/card/CardTitle.vue'
import Dialog from '@/components/ui/dialog/Dialog.vue'
import EmptyState from '@/components/ui/empty-state/EmptyState.vue'
import { ArrowLeftIcon, BookIcon, ClockIcon } from '@/components/icons'
import { formatDate, isOverdueDue } from '@/lib/format'
import { bookStatusKind } from '@/lib/status'
import { toast, toastError } from '@/components/ui/toast/use-toast'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const bookId = computed(() => Number(route.params.id))

const detail = ref<BookDetail | null>(null)
const loading = ref(true)
const acting = ref(false)

/** 我的在借记录（用于"归还"按钮判定） */
const myActiveRecord = ref<BorrowRecord | null>(null)
/** 本会话内预约成功（契约无"我的预约"查询，mock 流转用本地态） */
const reservedLocal = ref(false)
const reserveOpen = ref(false)

const relatedBooks = ref<Book[]>([])

const isOverdueActive = computed(
  () => myActiveRecord.value !== null && isOverdueDue(myActiveRecord.value.dueAt),
)

async function load(): Promise<void> {
  loading.value = true
  try {
    const [data, mine, listPage] = await Promise.all([
      booksApi.detail(bookId.value),
      recordsApi.list({ status: 'active', page: 1, size: 100 }),
      booksApi.list({ page: 1, size: 100 }),
    ])
    detail.value = data
    myActiveRecord.value = mine.list.find((r) => r.bookId === bookId.value) ?? null
    const relatedIds = data?.relatedIds ?? []
    relatedBooks.value = listPage.list.filter((b) => relatedIds.includes(b.id))
  } catch (e) {
    toastError(e instanceof ApiError ? e.message : '详情加载失败，请稍后重试', '图书详情')
  } finally {
    loading.value = false
  }
}

async function act(action: 'borrow' | 'return' | 'reserve'): Promise<void> {
  acting.value = true
  try {
    if (action === 'borrow') {
      await booksApi.borrow(bookId.value)
      toast({ title: '借阅成功', description: '应还日期以借出时间 + 30 天计' })
    } else if (action === 'return') {
      await booksApi.giveBack(bookId.value)
      toast({ title: '归还成功', description: '在借标志已释放' })
    } else {
      await booksApi.reserve(bookId.value)
      reservedLocal.value = true
      reserveOpen.value = false
      toast({ title: '预约成功', description: '本书归还后将第一时间通知您' })
    }
    await load()
  } catch (e) {
    toastError(e instanceof ApiError ? e.message : '操作失败，请稍后重试')
  } finally {
    acting.value = false
  }
}

watch(bookId, () => {
  // 切换图书：清掉上一本的会话内预约态
  reservedLocal.value = false
  void load()
})
onMounted(() => void load())

const historyRows = computed(() => detail.value?.recentRecords ?? [])

/** 删除（admin）：乐观删除 + 5s toast 撤销（替代 confirm），撤销 = 按原数据重新建档 */
async function removeBook(): Promise<void> {
  const book = detail.value
  if (!book || userStore.user?.role !== 'admin') return
  acting.value = true
  try {
    await booksApi.remove(book.id)
    toast({
      title: `已删除《${book.title}》`,
      description: '误删可在 5 秒内撤销',
      actionLabel: '撤销',
      onAction: async () => {
        try {
          await booksApi.create({
            title: book.title,
            author: book.author,
            category: book.category,
            publisher: book.publisher ?? undefined,
            isbn: book.isbn ?? undefined,
            coverUrl: book.coverUrl ?? undefined,
            description: book.description ?? undefined,
            totalCopies: book.totalCopies,
          })
          toast({ title: '已撤销删除', description: `《${book.title}》已恢复（id 会重新分配）` })
        } catch (e) {
          toastError(e instanceof ApiError ? e.message : '撤销失败', '删除图书')
        }
      },
    })
    await router.push('/')
  } catch (e) {
    toastError(e instanceof ApiError ? e.message : '删除失败，请稍后重试', '删除图书')
  } finally {
    acting.value = false
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center gap-3">
      <RouterLink
        to="/"
        class="inline-flex size-9 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
        aria-label="返回馆藏"
      >
        <ArrowLeftIcon class="size-4" />
      </RouterLink>
      <h1 class="truncate text-xl font-semibold tracking-tight">图书详情</h1>
    </div>

    <!-- 加载骨架 -->
    <div v-if="loading" class="grid gap-6 lg:grid-cols-[320px_1fr]">
      <div class="h-72 animate-pulse rounded-lg bg-muted" />
      <div class="space-y-4">
        <div class="h-8 w-1/3 animate-pulse rounded-sm bg-muted" />
        <div class="h-24 animate-pulse rounded-lg bg-muted" />
        <div class="h-48 animate-pulse rounded-lg bg-muted" />
      </div>
    </div>

    <!-- 契约约定：书不存在 → data:null -->
    <EmptyState
      v-else-if="!detail"
      title="图书不存在"
      description="可能已被删除，或链接有误"
    >
      <template #icon><BookIcon /></template>
    </EmptyState>

    <div v-else class="grid items-start gap-6 lg:grid-cols-[320px_1fr]">
      <!-- 左栏：封面 + 元信息 + 操作 -->
      <Card class="lg:sticky lg:top-20">
        <CardContent class="space-y-4 p-5">
          <div
            class="flex aspect-[3/4] items-center justify-center rounded-md bg-muted text-3xl font-semibold text-muted-foreground select-none"
          >
            {{ detail.title.slice(0, 2) }}
          </div>

          <div class="space-y-1.5">
            <div class="flex items-start justify-between gap-2">
              <h2 class="text-lg font-semibold leading-snug">{{ detail.title }}</h2>
              <StatusTag :kind="bookStatusKind(detail.borrowed)" />
            </div>
            <p class="text-sm text-muted-foreground">{{ detail.author }}</p>
          </div>

          <dl class="space-y-1.5 text-sm">
            <div class="flex justify-between gap-3">
              <dt class="text-muted-foreground">分类</dt>
              <dd>{{ detail.category }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-muted-foreground">出版社</dt>
              <dd class="truncate">{{ detail.publisher ?? '—' }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-muted-foreground">ISBN</dt>
              <dd class="truncate tabular-nums">{{ detail.isbn ?? '—' }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-muted-foreground">在馆复本</dt>
              <dd class="tabular-nums">{{ detail.totalCopies }}</dd>
            </div>
          </dl>

          <!-- 借 / 还 / 预约状态流转（按钮 loading 为动效 5 的一部分） -->
          <div class="space-y-2 border-t border-border pt-4">
            <template v-if="myActiveRecord">
              <StatusTag v-if="isOverdueActive" kind="overdue" class="mb-2" />
              <Button class="w-full" variant="secondary" :loading="acting" @click="act('return')">
                {{ acting ? '归还中…' : '归还' }}
              </Button>
              <p class="flex items-center gap-1.5 text-xs text-muted-foreground">
                <ClockIcon class="size-3.5" />
                应还日期：{{ formatDate(myActiveRecord.dueAt) }}
              </p>
            </template>

            <template v-else-if="!detail.borrowed">
              <Button class="w-full" :loading="acting" @click="act('borrow')">
                {{ acting ? '借阅中…' : '借阅' }}
              </Button>
            </template>

            <template v-else>
              <Button v-if="reservedLocal" class="w-full" variant="secondary" disabled>
                已预约，待归还通知
              </Button>
              <Button v-else class="w-full" variant="outline" :disabled="acting" @click="reserveOpen = true">
                预约
              </Button>
              <StatusTag v-if="reservedLocal" kind="reserved" class="mt-2" />
            </template>

            <!-- admin 维护动作 -->
            <Button
              v-if="userStore.user?.role === 'admin'"
              class="w-full"
              variant="ghost"
              :loading="acting"
              @click="removeBook"
            >
              删除此书（撤销窗口 5 秒）
            </Button>
          </div>
        </CardContent>
      </Card>

      <!-- 右栏：简介 / 借阅历史 / 相关推荐 -->
      <div class="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle>简介</CardTitle>
          </CardHeader>
          <CardContent>
            <p v-if="detail.description" class="text-sm leading-relaxed text-muted-foreground">
              {{ detail.description }}
            </p>
            <EmptyState v-else title="暂无简介" description="录入图书时可补充 description 字段" />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>借阅历史</CardTitle>
          </CardHeader>
          <CardContent>
            <div v-if="historyRows.length > 0" class="overflow-x-auto max-md:hidden">
              <table class="w-full text-sm">
                <thead>
                  <tr class="border-b border-border text-left text-xs text-muted-foreground">
                    <th class="px-3 py-2 font-medium">借阅人</th>
                    <th class="px-3 py-2 font-medium">借出时间</th>
                    <th class="px-3 py-2 font-medium">应还日期</th>
                    <th class="px-3 py-2 font-medium">归还时间</th>
                    <th class="px-3 py-2 font-medium">状态</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="record in historyRows"
                    :key="record.id"
                    class="border-b border-border/60 last:border-0"
                  >
                    <td class="px-3 py-2.5">
                      {{ record.userId === userStore.user?.id ? '我' : `用户 ${record.userId}` }}
                    </td>
                    <td class="px-3 py-2.5 tabular-nums text-muted-foreground">{{ formatDate(record.borrowedAt) }}</td>
                    <td class="px-3 py-2.5 tabular-nums text-muted-foreground">{{ formatDate(record.dueAt) }}</td>
                    <td class="px-3 py-2.5 tabular-nums text-muted-foreground">
                      {{ record.returnedAt ? formatDate(record.returnedAt) : '—' }}
                    </td>
                    <td class="px-3 py-2.5">
                      <RecordTag :record="record" />
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <!-- 移动端：借阅历史转卡片 -->
            <ul v-if="historyRows.length > 0" class="space-y-2 md:hidden">
              <li v-for="record in historyRows" :key="record.id" class="rounded-md border border-border p-3 text-sm">
                <div class="flex items-center justify-between gap-2">
                  <span>{{ record.userId === userStore.user?.id ? '我' : `用户 ${record.userId}` }}</span>
                  <RecordTag :record="record" />
                </div>
                <p class="mt-1 text-xs tabular-nums text-muted-foreground">
                  借出 {{ formatDate(record.borrowedAt) }} · 应还 {{ formatDate(record.dueAt) }}
                  <template v-if="record.returnedAt"> · 归还 {{ formatDate(record.returnedAt) }}</template>
                </p>
              </li>
            </ul>
            <EmptyState v-if="historyRows.length === 0" title="暂无借阅记录" description="这本书还没有被借过" />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>相关推荐</CardTitle>
          </CardHeader>
          <CardContent>
            <div v-if="relatedBooks.length > 0" class="grid gap-3 sm:grid-cols-2">
              <RouterLink
                v-for="b in relatedBooks"
                :key="b.id"
                :to="`/books/${b.id}`"
                class="flex items-center justify-between gap-3 rounded-md border border-border p-3 transition-all duration-150 hover:-translate-y-0.5 hover:border-ring/50 hover:shadow-rest"
              >
                <div class="min-w-0">
                  <p class="truncate text-sm font-medium">{{ b.title }}</p>
                  <p class="truncate text-xs text-muted-foreground">{{ b.author }}</p>
                </div>
                <StatusTag :kind="bookStatusKind(b.borrowed)" />
              </RouterLink>
            </div>
            <EmptyState v-else title="暂无推荐" description="共借数据不足，且同分类下暂无其他图书" />
          </CardContent>
        </Card>
      </div>
    </div>

    <!-- 预约确认 -->
    <Dialog v-model:open="reserveOpen" title="确认预约" :description="`《${detail?.title ?? ''}》当前在借，归还后将按预约顺序通知您`">
      <div class="grid grid-cols-2 gap-3">
        <BentoStat label="当前状态" value="在借" tone="info" />
        <BentoStat label="通知方式" value="站内通知" hint="红点提醒" />
      </div>
      <template #footer>
        <Button variant="ghost" @click="reserveOpen = false">取消</Button>
        <Button :disabled="acting" @click="act('reserve')">{{ acting ? '预约中…' : '确认预约' }}</Button>
      </template>
    </Dialog>
  </div>
</template>
