<script setup lang="ts">
/**
 * /records 借阅记录：状态筛选（active / returned / overdue 派生）+ 紧凑表格 + 应还日期
 * reader 只看本人记录（契约行为，mock 同步实现）
 */
import { h, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { FlexRender, getCoreRowModel, useVueTable, type ColumnDef } from '@tanstack/vue-table'
import * as recordsApi from '@/api/records'
import { ApiError, type BorrowRecord, type RecordStatusFilter } from '@/api/types'
import SkeletonRow from '@/components/SkeletonRow.vue'
import RecordTag from '@/components/RecordTag.vue'
import Button from '@/components/ui/button/Button.vue'
import Card from '@/components/ui/card/Card.vue'
import EmptyState from '@/components/ui/empty-state/EmptyState.vue'
import { ChevronLeftIcon, ChevronRightIcon, ClockIcon } from '@/components/icons'
import { formatDate, formatDateTime } from '@/lib/format'
import { toastError } from '@/components/ui/toast/use-toast'

const PAGE_SIZE = 10

const FILTERS: { key: RecordStatusFilter | ''; label: string }[] = [
  { key: '', label: '全部' },
  { key: 'active', label: '在借' },
  { key: 'overdue', label: '逾期' },
  { key: 'returned', label: '已还' },
]

const status = ref<RecordStatusFilter | ''>('')
const rows = ref<BorrowRecord[]>([])
const total = ref(0)
const page = ref(1)
const pageCount = ref(1)
const loading = ref(true)

const columns: ColumnDef<BorrowRecord>[] = [
  {
    accessorKey: 'bookTitle',
    header: '书名',
    cell: ({ row }) =>
      h(
        RouterLink,
        { to: `/books/${row.original.bookId}`, class: 'font-medium text-foreground hover:text-primary' },
        () => row.original.bookTitle ?? `#${row.original.bookId}`,
      ),
  },
  { accessorKey: 'bookAuthor', header: '作者' },
  {
    accessorKey: 'borrowedAt',
    header: '借出时间',
    cell: ({ row }) => h('span', { class: 'tabular-nums text-muted-foreground' }, formatDate(row.original.borrowedAt)),
  },
  {
    accessorKey: 'dueAt',
    header: '应还日期',
    cell: ({ row }) => h('span', { class: 'tabular-nums' }, formatDate(row.original.dueAt)),
  },
  {
    accessorKey: 'returnedAt',
    header: '归还时间',
    cell: ({ row }) =>
      h(
        'span',
        { class: 'tabular-nums text-muted-foreground' },
        row.original.returnedAt ? formatDateTime(row.original.returnedAt) : '—',
      ),
  },
  {
    id: 'status',
    header: '状态',
    cell: ({ row }) => h(RecordTag, { record: row.original }),
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
    const data = await recordsApi.list({
      status: status.value || undefined,
      page: page.value,
      size: PAGE_SIZE,
    })
    rows.value = data.list
    total.value = data.total
    pageCount.value = Math.max(1, Math.ceil(data.total / PAGE_SIZE))
  } catch (e) {
    toastError(e instanceof ApiError ? e.message : '记录加载失败，请稍后重试', '借阅记录')
  } finally {
    loading.value = false
  }
}

watch([status, page], () => void load())
onMounted(() => void load())

function pick(key: RecordStatusFilter | ''): void {
  status.value = key
  page.value = 1
}
</script>

<template>
  <div class="space-y-6">
    <h1 class="text-xl font-semibold tracking-tight">借阅记录</h1>

    <!-- 状态筛选 -->
    <div class="flex flex-wrap items-center gap-1.5">
      <Button
        v-for="f in FILTERS"
        :key="f.key"
        size="sm"
        :variant="status === f.key ? 'secondary' : 'ghost'"
        class="rounded-full"
        @click="pick(f.key)"
      >
        {{ f.label }}
      </Button>
      <span class="ml-auto flex items-center gap-1.5 text-xs text-muted-foreground">
        <ClockIcon class="size-3.5" />
        逾期为派生态：在借且超过应还日期
      </span>
    </div>

    <Card class="overflow-hidden">
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
            <SkeletonRow v-if="loading" :cols="6" />
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
                <td colspan="6">
                  <EmptyState title="暂无借阅记录" description="去馆藏页挑一本好书吧">
                    <template #icon><ClockIcon /></template>
                  </EmptyState>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>
    </Card>

    <div class="flex items-center justify-between text-sm text-muted-foreground">
      <span>共 {{ total }} 条 · 第 {{ page }} / {{ pageCount }} 页</span>
      <div class="flex items-center gap-1.5">
        <Button size="icon" variant="outline" :disabled="page <= 1" aria-label="上一页" @click="page--">
          <ChevronLeftIcon />
        </Button>
        <Button size="icon" variant="outline" :disabled="page >= pageCount" aria-label="下一页" @click="page++">
          <ChevronRightIcon />
        </Button>
      </div>
    </div>
  </div>
</template>
