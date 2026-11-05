<script setup lang="ts">
/**
 * / 馆藏列表：Bento 统计条 + 搜索 + 分类筛选 + 表格⇄卡片双视图 + 分页
 * v0.9：卡片 stagger 入场（仅首次）· 批量多选（借阅/删除/CSV）· 键盘流（/ 搜索、方向键选行回车借阅）
 *      扫码建档入口（admin，含手输降级）· <768px 表格转卡片
 * 数据全部经 src/api/*.ts（B 阶段内部走 mock），组件无裸 fetch
 */
import { h, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { FlexRender, getCoreRowModel, useVueTable, type ColumnDef } from '@tanstack/vue-table'
import * as booksApi from '@/api/books'
import * as statsApi from '@/api/stats'
import { ApiError, type Book, type StatsData } from '@/api/types'
import BookCard from '@/components/BookCard.vue'
import ScanBookDialog from '@/components/ScanBookDialog.vue'
import SkeletonRow from '@/components/SkeletonRow.vue'
import EnterStagger from '@/components/motion/EnterStagger.vue'
import StatusTag from '@/components/StatusTag.vue'
import BentoStat from '@/components/BentoStat.vue'
import Button from '@/components/ui/button/Button.vue'
import Card from '@/components/ui/card/Card.vue'
import EmptyState from '@/components/ui/empty-state/EmptyState.vue'
import Input from '@/components/ui/input/Input.vue'
import {
  BookIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  DownloadIcon,
  GridIcon,
  ScanIcon,
  SearchIcon,
  TableIcon,
} from '@/components/icons'
import { useUserStore } from '@/stores/user'
import { bookStatusKind } from '@/lib/status'
import { downloadCsv } from '@/lib/csv'
import { toast, toastError } from '@/components/ui/toast/use-toast'

const PAGE_SIZE = 5

const userStore = useUserStore()

const stats = ref<StatsData | null>(null)
const rows = ref<Book[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(true)

const keywordInput = ref('')
const keyword = ref('')
const category = ref('全部')
const view = ref<'table' | 'cards'>('table')

const pageCount = ref(1)

/** 动效 1：stagger 仅首次进入页面播放 */
const firstEntrance = ref(true)

/* ---------------- 批量多选 ---------------- */
const selected = ref<Set<number>>(new Set())

function toggleSelect(id: number): void {
  const next = new Set(selected.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  selected.value = next
}

const pageAllSelected = ref(false)

function toggleSelectPage(): void {
  if (pageAllSelected.value) selected.value = new Set()
  else selected.value = new Set(rows.value.map((b) => b.id))
}

watch(rows, (list) => {
  pageAllSelected.value = list.length > 0 && list.every((b) => selected.value.has(b.id))
})
watch(selected, (set) => {
  pageAllSelected.value = rows.value.length > 0 && rows.value.every((b) => set.has(b.id))
})

const batchBusy = ref(false)

/** 批量借阅：仅对在馆书逐本调用；40901 汇总进 toast */
async function batchBorrow(): Promise<void> {
  const books = rows.value.filter((b) => selected.value.has(b.id) && !b.borrowed)
  if (books.length === 0) {
    toastError('所选图书中没有在馆可借的书', '批量借阅')
    return
  }
  batchBusy.value = true
  let ok = 0
  const conflicts: string[] = []
  try {
    for (const b of books) {
      try {
        await booksApi.borrow(b.id)
        ok += 1
      } catch (e) {
        if (e instanceof ApiError && e.code === 40901) conflicts.push(`《${b.title}》刚被借走`)
        else conflicts.push(`《${b.title}》失败`)
      }
    }
    toast({
      title: `批量借阅完成`,
      description: `成功 ${ok} 本${conflicts.length ? `；${conflicts.join('、')}` : ''}`,
      variant: conflicts.length ? 'destructive' : 'default',
    })
    selected.value = new Set()
    await load()
  } finally {
    batchBusy.value = false
  }
}

/** 批量删除（admin）：乐观移除 + 5s toast 撤销（撤销 = 按原数据重新建档） */
async function batchDelete(): Promise<void> {
  const books = rows.value.filter((b) => selected.value.has(b.id))
  if (books.length === 0) return
  batchBusy.value = true
  try {
    for (const b of books) await booksApi.remove(b.id)
    selected.value = new Set()
    await load()
    toast({
      title: `已删除 ${books.length} 本图书`,
      description: '误删可在 5 秒内撤销',
      actionLabel: '撤销',
      onAction: async () => {
        try {
          for (const b of [...books].reverse()) {
            await booksApi.create({
              title: b.title,
              author: b.author,
              category: b.category,
              publisher: b.publisher ?? undefined,
              isbn: b.isbn ?? undefined,
              coverUrl: b.coverUrl ?? undefined,
              description: b.description ?? undefined,
              totalCopies: b.totalCopies,
            })
          }
          toast({ title: '已撤销删除', description: `${books.length} 本图书已恢复（id 会重新分配）` })
          await load()
        } catch (e) {
          toastError(e instanceof ApiError ? e.message : '撤销失败', '批量删除')
        }
      },
    })
  } catch (e) {
    toastError(e instanceof ApiError ? e.message : '批量删除失败', '批量删除')
  } finally {
    batchBusy.value = false
  }
}

/** 导出 CSV（UTF-8 BOM）：导出所选（未选则导出当前页） */
function exportCsv(): void {
  const books = selected.value.size > 0 ? rows.value.filter((b) => selected.value.has(b.id)) : rows.value
  if (books.length === 0) {
    toastError('没有可导出的图书', '导出 CSV')
    return
  }
  const header = ['编号', '书名', '作者', '分类', '出版社', 'ISBN', '状态', '复本数']
  const body = books.map((b) => [
    b.id,
    b.title,
    b.author,
    b.category,
    b.publisher ?? '',
    b.isbn ?? '',
    b.borrowed ? '在借' : '在馆',
    b.totalCopies,
  ])
  // 文件名时间戳用本地日期（同 trend 补零，避免 UTC 0/8 点边界差一天）
  const now = new Date()
  const p2 = (n: number) => String(n).padStart(2, '0')
  const stamp = `${now.getFullYear()}${p2(now.getMonth() + 1)}${p2(now.getDate())}`
  downloadCsv(`books-${stamp}.csv`, [header, ...body])
  toast({ title: '已导出 CSV', description: `${books.length} 条记录（UTF-8 BOM）` })
}

/* ---------------- 键盘流 ---------------- */
const searchRef = ref<{ $el: HTMLInputElement } | null>(null)
const activeRowIndex = ref(-1)

function onGlobalKeydown(e: KeyboardEvent): void {
  const target = e.target as HTMLElement | null
  const typing = target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable)
  if (e.key === '/' && !typing) {
    e.preventDefault()
    searchRef.value?.$el.focus()
    return
  }
  if (view.value !== 'table' || typing) return
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    activeRowIndex.value = Math.min(activeRowIndex.value + 1, rows.value.length - 1)
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    activeRowIndex.value = Math.max(activeRowIndex.value - 1, 0)
  } else if (e.key === 'Enter' && activeRowIndex.value >= 0) {
    const book = rows.value[activeRowIndex.value]
    if (!book) return
    e.preventDefault()
    void borrowOne(book)
  }
}

async function borrowOne(book: Book): Promise<void> {
  try {
    if (book.borrowed) {
      toastError('该书在借，可到详情页预约', '借阅')
      return
    }
    await booksApi.borrow(book.id)
    toast({ title: '借阅成功', description: `《${book.title}》借出，应还日期见借阅记录` })
    await load()
  } catch (e) {
    toastError(e instanceof ApiError ? e.message : '借阅失败，请稍后重试', '借阅')
  }
}

onMounted(() => window.addEventListener('keydown', onGlobalKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onGlobalKeydown))

/* ---------------- 扫码建档（admin） ---------------- */
const scanOpen = ref(false)

/* ---------------- 表格列 ---------------- */
const columns: ColumnDef<Book>[] = [
  {
    id: 'select',
    header: () =>
      h('input', {
        type: 'checkbox',
        checked: pageAllSelected.value,
        'aria-label': '全选本页',
        class: 'size-3.5 cursor-pointer accent-[var(--primary)]',
        onChange: () => toggleSelectPage(),
      }),
    cell: ({ row }) =>
      h('input', {
        type: 'checkbox',
        checked: selected.value.has(row.original.id),
        'aria-label': `选择 ${row.original.title}`,
        class: 'size-3.5 cursor-pointer accent-[var(--primary)]',
        onChange: () => toggleSelect(row.original.id),
      }),
  },
  {
    accessorKey: 'title',
    header: '书名',
    cell: ({ row }) =>
      h(
        RouterLink,
        { to: `/books/${row.original.id}`, class: 'font-medium text-foreground hover:text-primary' },
        () => row.original.title,
      ),
  },
  { accessorKey: 'author', header: '作者' },
  {
    accessorKey: 'category',
    header: '分类',
    cell: ({ row }) =>
      h(
        'span',
        { class: 'rounded-sm bg-muted px-1.5 py-0.5 text-xs text-muted-foreground' },
        row.original.category,
      ),
  },
  {
    id: 'status',
    header: '状态',
    cell: ({ row }) => h(StatusTag, { kind: bookStatusKind(row.original.borrowed) }),
  },
  {
    id: 'actions',
    header: '',
    cell: ({ row }) => {
      const book = row.original
      if (book.borrowed) {
        return h(
          RouterLink,
          { to: `/books/${book.id}`, class: 'text-sm text-muted-foreground hover:text-primary' },
          () => '预约 →',
        )
      }
      return h(
        'button',
        {
          class: 'cursor-pointer text-sm font-medium text-primary hover:underline',
          onClick: () => void borrowOne(book),
        },
        '借阅',
      )
    },
  },
]

const table = useVueTable({
  get data() {
    return rows.value
  },
  columns,
  getCoreRowModel: getCoreRowModel(),
  manualPagination: true,
  get pageCount() {
    return pageCount.value
  },
})

async function load(): Promise<void> {
  loading.value = true
  try {
    const [statsData, listData] = await Promise.all([
      statsApi.get(),
      booksApi.list({
        keyword: keyword.value || undefined,
        category: category.value === '全部' ? undefined : category.value,
        page: page.value,
        size: PAGE_SIZE,
      }),
    ])
    stats.value = statsData
    rows.value = listData.list
    total.value = listData.total
    pageCount.value = Math.max(1, Math.ceil(listData.total / PAGE_SIZE))
    if (firstEntrance.value) {
      // 首屏 stagger 播完一轮后关闭重播
      setTimeout(() => (firstEntrance.value = false), 600)
    }
  } catch (e) {
    toastError(e instanceof ApiError ? e.message : '馆藏加载失败，请稍后重试', '馆藏')
  } finally {
    loading.value = false
  }
}

// 搜索防抖
let keywordTimer: ReturnType<typeof setTimeout> | undefined
watch(keywordInput, (value) => {
  clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => {
    keyword.value = value.trim()
    page.value = 1
  }, 300)
})

watch([keyword, category], () => {
  void load()
})
watch(page, () => {
  void load()
})

onMounted(() => {
  void load()
})

function pickCategory(name: string): void {
  category.value = name
  page.value = 1
}
</script>

<template>
  <div class="space-y-5">
    <div class="flex items-center justify-between gap-3">
      <h1 class="text-xl font-semibold tracking-tight">馆藏</h1>
      <Button
        v-if="userStore.user?.role === 'admin'"
        size="sm"
        variant="outline"
        class="max-md:size-11 max-md:px-0"
        @click="scanOpen = true"
      >
        <ScanIcon />
        <span class="max-md:hidden">扫码建档</span>
      </Button>
    </div>

    <!-- Bento 统计条（mock stats 数据，stagger 入场） -->
    <div class="grid grid-cols-2 gap-3 sm:grid-cols-3">
      <EnterStagger
        v-for="(tile, i) in ([
          { label: '馆藏总量', value: stats?.total ?? '—', hint: `${stats?.categories.length ?? 0} 个分类`, tone: 'default' },
          { label: '在馆', value: stats?.available ?? '—', hint: undefined, tone: 'success' },
          { label: '在借', value: stats?.borrowed ?? '—', hint: undefined, tone: 'info' },
        ] as { label: string; value: number | string; hint?: string; tone: 'default' | 'success' | 'info' }[])"
        :key="tile.label"
        :index="i"
        :animate="firstEntrance"
      >
        <BentoStat :label="tile.label" :value="tile.value" :hint="tile.hint" :tone="tile.tone" class="h-full" />
      </EnterStagger>
    </div>

    <!-- 搜索 + 分类筛选 + 双视图切换 -->
    <div class="flex flex-col gap-3 lg:flex-row lg:items-center">
      <div class="relative w-full lg:max-w-xs">
        <SearchIcon class="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          ref="searchRef"
          v-model="keywordInput"
          placeholder="搜索书名 / 作者（服务端搜索，按 / 聚焦）"
          class="pl-8"
        />
      </div>

      <div class="flex flex-wrap items-center gap-1.5">
        <Button
          v-for="c in ['全部', ...(stats?.categories.map((x) => x.name) ?? [])]"
          :key="c"
          size="sm"
          :variant="category === c ? 'secondary' : 'ghost'"
          class="rounded-full"
          @click="pickCategory(c)"
        >
          {{ c }}
        </Button>
      </div>

      <div class="flex items-center gap-1 lg:ml-auto">
        <Button
          size="icon"
          :variant="view === 'table' ? 'secondary' : 'ghost'"
          aria-label="表格视图"
          @click="view = 'table'"
        >
          <TableIcon />
        </Button>
        <Button
          size="icon"
          :variant="view === 'cards' ? 'secondary' : 'ghost'"
          aria-label="卡片视图"
          @click="view = 'cards'"
        >
          <GridIcon />
        </Button>
      </div>
    </div>

    <!-- 批量操作条 -->
    <Card v-if="selected.size > 0" class="flex flex-wrap items-center gap-2 p-3">
      <span class="text-sm text-muted-foreground">已选 {{ selected.size }} 本</span>
      <div class="ml-auto flex flex-wrap items-center gap-1.5">
        <Button size="sm" :loading="batchBusy" @click="batchBorrow">批量借阅</Button>
        <Button size="sm" variant="outline" @click="exportCsv">
          <DownloadIcon />
          导出 CSV
        </Button>
        <Button
          v-if="userStore.user?.role === 'admin'"
          size="sm"
          variant="destructive"
          :loading="batchBusy"
          @click="batchDelete"
        >
          批量删除
        </Button>
        <Button size="sm" variant="ghost" @click="selected = new Set()">取消选择</Button>
      </div>
    </Card>

    <!-- 表格视图（桌面）：<768px 转卡片布局 -->
    <div v-if="view === 'table'">
      <Card class="hidden overflow-hidden md:block">
        <div
          class="overflow-x-auto focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/60"
          tabindex="0"
          aria-label="馆藏表格（方向键选行，回车借阅）"
        >
          <table class="w-full text-sm">
            <thead>
              <tr
                v-for="headerGroup in table.getHeaderGroups()"
                :key="headerGroup.id"
                class="border-b border-border text-left text-xs text-muted-foreground"
              >
                <th v-for="header in headerGroup.headers" :key="header.id" class="px-3 py-2.5 font-medium">
                  <FlexRender
                    v-if="!header.isPlaceholder"
                    :render="header.column.columnDef.header"
                    :props="header.getContext()"
                  />
                </th>
              </tr>
            </thead>
            <tbody>
              <SkeletonRow v-if="loading" :cols="6" />
              <template v-else>
                <tr
                  v-for="(row, i) in table.getRowModel().rows"
                  :key="row.id"
                  class="border-b border-border/60 transition-colors last:border-0 hover:bg-muted/40"
                  :class="{ 'bg-accent/60': i === activeRowIndex }"
                >
                  <td v-for="cell in row.getVisibleCells()" :key="cell.id" class="px-3 py-2.5">
                    <FlexRender :render="cell.column.columnDef.cell" :props="cell.getContext()" />
                  </td>
                </tr>
                <tr v-if="rows.length === 0">
                  <td colspan="6">
                    <EmptyState
                      title="没有找到相关图书"
                      :description="keyword || category !== '全部' ? '换个关键词或分类试试' : '馆藏为空'"
                    >
                      <template #icon><BookIcon /></template>
                    </EmptyState>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </Card>

      <!-- 移动端：同一数据转卡片 -->
      <div class="space-y-3 md:hidden">
        <template v-if="loading">
          <div v-for="i in 4" :key="i" class="h-[104px] animate-pulse rounded-lg bg-muted" />
        </template>
        <template v-else-if="rows.length > 0">
          <BookCard v-for="book in rows" :key="book.id" :book="book" />
        </template>
        <EmptyState
          v-else
          title="没有找到相关图书"
          description="换个关键词或分类试试"
        >
          <template #icon><BookIcon /></template>
        </EmptyState>
      </div>
    </div>

    <!-- 卡片视图 -->
    <div v-if="view === 'cards'" class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
      <template v-if="loading">
        <div v-for="i in 6" :key="i" class="h-[104px] animate-pulse rounded-lg bg-muted" />
      </template>
      <template v-else>
        <EnterStagger v-for="(book, i) in rows" :key="book.id" :index="i" :animate="firstEntrance">
          <BookCard :book="book" class="h-full" />
        </EnterStagger>
        <div v-if="rows.length === 0" class="sm:col-span-2 lg:col-span-3">
          <EmptyState title="没有找到相关图书" description="换个关键词或分类试试">
            <template #icon><BookIcon /></template>
          </EmptyState>
        </div>
      </template>
    </div>

    <!-- 分页 -->
    <div class="flex items-center justify-between text-sm text-muted-foreground">
      <span>共 {{ total }} 条 · 第 {{ page }} / {{ pageCount }} 页</span>
      <div class="flex items-center gap-1.5">
        <Button size="icon" variant="outline" :disabled="page <= 1" aria-label="上一页" @click="page--">
          <ChevronLeftIcon />
        </Button>
        <Button
          size="icon"
          variant="outline"
          :disabled="page >= pageCount"
          aria-label="下一页"
          @click="page++"
        >
          <ChevronRightIcon />
        </Button>
      </div>
    </div>

    <ScanBookDialog v-model:open="scanOpen" @saved="load()" />
  </div>
</template>
