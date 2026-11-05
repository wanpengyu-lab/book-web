<script setup lang="ts">
/**
 * /me 个人中心：Bento 小格（身份 / 在借 / 逾期 / 未读）+ 我的在借列表 + 最新通知
 */
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import * as recordsApi from '@/api/records'
import { ApiError, type BorrowRecord } from '@/api/types'
import BentoStat from '@/components/BentoStat.vue'
import RecordTag from '@/components/RecordTag.vue'
import Card from '@/components/ui/card/Card.vue'
import CardContent from '@/components/ui/card/CardContent.vue'
import CardHeader from '@/components/ui/card/CardHeader.vue'
import CardTitle from '@/components/ui/card/CardTitle.vue'
import EmptyState from '@/components/ui/empty-state/EmptyState.vue'
import { UserIcon } from '@/components/icons'
import { formatDate, formatDateTime, isOverdueDue } from '@/lib/format'
import { toastError } from '@/components/ui/toast/use-toast'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const myRecords = ref<BorrowRecord[]>([])
const loading = ref(true)

const activeRecords = ref<BorrowRecord[]>([])

const overdueCount = ref(0)

onMounted(async () => {
  try {
    const page = await recordsApi.list({ page: 1, size: 50 })
    myRecords.value = page.list
    activeRecords.value = page.list.filter((r) => r.status === 'active')
    overdueCount.value = activeRecords.value.filter((r) => isOverdueDue(r.dueAt)).length
  } catch (e) {
    toastError(e instanceof ApiError ? e.message : '个人数据加载失败', '个人中心')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="space-y-6">
    <h1 class="text-xl font-semibold tracking-tight">个人中心</h1>

    <!-- Bento 小格 -->
    <div class="grid grid-cols-2 gap-3 lg:grid-cols-4">
      <Card class="flex items-center gap-3">
        <div
          class="flex size-11 shrink-0 items-center justify-center rounded-full bg-primary/15 text-primary"
        >
          <UserIcon class="size-5" />
        </div>
        <div class="min-w-0">
          <p class="truncate text-sm font-semibold">{{ userStore.user?.username }}</p>
          <p class="text-xs text-muted-foreground">角色：{{ userStore.user?.role }}</p>
        </div>
      </Card>
      <BentoStat label="我的在借" :value="loading ? '—' : activeRecords.length" tone="info" hint="本（未归还）" />
      <BentoStat label="我的逾期" :value="loading ? '—' : overdueCount" :tone="overdueCount > 0 ? 'danger' : 'default'" hint="本（超期未还）" />
      <BentoStat
        label="未读通知"
        :value="userStore.unreadCount"
        :tone="userStore.unreadCount > 0 ? 'warning' : 'default'"
        hint="条（顶栏铃铛查看）"
      />
    </div>

    <div class="grid items-start gap-6 lg:grid-cols-2">
      <!-- 我的在借 -->
      <Card>
        <CardHeader>
          <CardTitle>我的在借</CardTitle>
        </CardHeader>
        <CardContent>
          <ul v-if="activeRecords.length > 0" class="space-y-2">
            <li
              v-for="r in activeRecords"
              :key="r.id"
              class="flex items-center justify-between gap-3 rounded-md border border-border p-3"
            >
              <div class="min-w-0">
                <RouterLink :to="`/books/${r.bookId}`" class="truncate text-sm font-medium hover:text-primary">
                  {{ r.bookTitle ?? `#${r.bookId}` }}
                </RouterLink>
                <p class="mt-0.5 text-xs text-muted-foreground">应还日期：{{ formatDate(r.dueAt) }}</p>
              </div>
              <RecordTag :record="r" />
            </li>
          </ul>
          <EmptyState v-else title="暂无在借图书" description="去馆藏页借一本吧" class="py-8" />
        </CardContent>
      </Card>

      <!-- 借阅历史（最近） -->
      <Card>
        <CardHeader>
          <CardTitle>借阅历史</CardTitle>
        </CardHeader>
        <CardContent>
          <ul v-if="myRecords.length > 0" class="space-y-2">
            <li v-for="r in myRecords.slice(0, 8)" :key="r.id" class="flex items-center justify-between gap-3 text-sm">
              <div class="min-w-0">
                <RouterLink :to="`/books/${r.bookId}`" class="block truncate hover:text-primary">
                  {{ r.bookTitle ?? `#${r.bookId}` }}
                </RouterLink>
                <p class="text-xs text-muted-foreground">
                  {{ formatDate(r.borrowedAt) }} 借出
                  <template v-if="r.returnedAt"> · {{ formatDateTime(r.returnedAt) }} 归还</template>
                </p>
              </div>
              <RecordTag :record="r" />
            </li>
          </ul>
          <EmptyState v-else title="暂无借阅记录" description="首次借阅后会出现在这里" class="py-8" />
        </CardContent>
      </Card>
    </div>
  </div>
</template>
