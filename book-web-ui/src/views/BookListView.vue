<script setup lang="ts">
/**
 * / 馆藏列表：Bento 统计条 + 搜索 + 分类筛选 + 表格⇄卡片双视图 + 分页
 * 数据全部经 src/api/*.ts（B 阶段内部走 mock），组件无裸 fetch
 */
import { h, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { getCoreRowModel, useVueTable, FlexRender, type ColumnDef } from '@tanstack/vue-table'
import * as booksApi from '@/api/books'
import * as statsApi from '@/api/stats'
import { ApiError, type Book, type StatsData } from '@/api/types'
import BookCard from '@/components/BookCard.vue'
import SkeletonRow from '@/components/SkeletonRow.vue'
import StatusTag from '@/components/StatusTag.vue'
import BentoStat from '@/components/BentoStat.vue'
import Button from '@/components/ui/button/Button.vue'
import Card from '@/components/ui/card/Card.vue'
import EmptyState from '@/components/ui/empty-state/EmptyState.vue'
import Input from '@/components/ui/input/Input.vue'
import {
  ChevronLeftIcon,
  ChevronRightIcon,
  GridIcon,
  SearchIcon,
  TableIcon,
  BookIcon,
} from '@/components/icons'
import { bookStatusKind } from '@/lib/status'
import { toastError } from '@/components/ui/toast/use-toast'

const PAGE_SIZE = 5

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

const columns: ColumnDef<Book>[] = [
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
    cell: ({ row }) =>
      h(
        RouterLink,
        { to: `/books/${row.original.id}`, class: 'text-sm text-primary hover:underline' },
        () => '详情',
      ),
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
  <div class="space-y-6">
    <h1 class="text-xl font-semibold tracking-tight">馆藏</h1>

    <!-- Bento 统计条（mock stats 数据） -->
    <div class="grid grid-cols-2 gap-3 sm:grid-cols-3">
      <BentoStat label="馆藏总量" :value="stats?.total ?? '—'" :hint="`${stats?.categories.length ?? 0} 个分类`" />
      <BentoStat label="在馆" :value="stats?.available ?? '—'" tone="success" />
      <BentoStat label="在借" :value="stats?.borrowed ?? '—'" tone="info" />
    </div>

    <!-- 搜索 + 分类筛选 + 双视图切换 -->
    <div class="flex flex-col gap-3 lg:flex-row lg:items-center">
      <div class="relative w-full lg:max-w-xs">
        <SearchIcon class="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input v-model="keywordInput" placeholder="搜索书名 / 作者（服务端搜索）" class="pl-8" />
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

    <!-- 表格视图（TanStack Vue Table，紧凑表格） -->
    <Card v-if="view === 'table'" class="overflow-hidden">
      <div class="overflow-x-auto">
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
            <SkeletonRow v-if="loading" :cols="5" />
            <template v-else>
              <tr
                v-for="row in table.getRowModel().rows"
                :key="row.id"
                class="border-b border-border/60 transition-colors last:border-0 hover:bg-muted/40"
              >
                <td v-for="cell in row.getVisibleCells()" :key="cell.id" class="px-3 py-2.5">
                  <FlexRender :render="cell.column.columnDef.cell" :props="cell.getContext()" />
                </td>
              </tr>
              <tr v-if="rows.length === 0">
                <td colspan="5">
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

    <!-- 卡片视图 -->
    <div v-if="view === 'cards'" class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
      <template v-if="loading">
        <div v-for="i in 6" :key="i" class="h-[104px] animate-pulse rounded-lg bg-muted" />
      </template>
      <template v-else>
        <BookCard v-for="book in rows" :key="book.id" :book="book" />
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
  </div>
</template>
