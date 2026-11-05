<script setup lang="ts">
/**
 * /dashboard 看板：趋势 LineChart 大格 + 分类占比 PieChart + 排行榜格
 * 拍板项 a：trend 稀疏返回 → 前端补零成连续 30 天横轴（契约注明）
 * 动效 4：三格 scroll reveal（Reveal 组件，尊重 prefers-reduced-motion）
 */
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import * as dashboardApi from '@/api/dashboard'
import { ApiError, type DashboardData, type TrendPoint } from '@/api/types'
import BentoStat from '@/components/BentoStat.vue'
import Reveal from '@/components/motion/Reveal.vue'
import LineChart from '@/components/charts/LineChart.vue'
import PieChart from '@/components/charts/PieChart.vue'
import Card from '@/components/ui/card/Card.vue'
import CardContent from '@/components/ui/card/CardContent.vue'
import CardHeader from '@/components/ui/card/CardHeader.vue'
import CardTitle from '@/components/ui/card/CardTitle.vue'
import EmptyState from '@/components/ui/empty-state/EmptyState.vue'
import { toastError } from '@/components/ui/toast/use-toast'

const data = ref<DashboardData | null>(null)
const loading = ref(true)

/** 近 30 天连续序列：稀疏 trend 补零（契约拍板项 a） */
function fillTrend(sparse: TrendPoint[]): { date: string; count: number }[] {
  const byDate = new Map(sparse.map((p) => [p.date, p.count]))
  const DAY = 24 * 60 * 60 * 1000
  const out: { date: string; count: number }[] = []
  for (let i = 29; i >= 0; i--) {
    const d = new Date(Date.now() - i * DAY)
    const date = d.toISOString().slice(0, 10)
    out.push({ date, count: byDate.get(date) ?? 0 })
  }
  return out
}

const trend = computed(() => fillTrend(data.value?.trend ?? []))
const trendTotal = computed(() => trend.value.reduce((sum, p) => sum + p.count, 0))
const trendActiveDays = computed(() => trend.value.filter((p) => p.count > 0).length)
const trendPeak = computed(() => trend.value.reduce((a, b) => (b.count > a.count ? b : a), { date: '', count: 0 }))

onMounted(async () => {
  try {
    data.value = await dashboardApi.get()
  } catch (e) {
    toastError(e instanceof ApiError ? e.message : '看板加载失败，请稍后重试', '看板')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-xl font-semibold tracking-tight">看板</h1>
      <p class="mt-1 text-sm text-muted-foreground">近 30 天借出与馆藏分布（数据来自 SQL 聚合）</p>
    </div>

    <div v-if="loading" class="grid gap-4 lg:grid-cols-2">
      <div class="h-72 animate-pulse rounded-lg bg-muted lg:col-span-2" />
      <div class="h-64 animate-pulse rounded-lg bg-muted" />
      <div class="h-64 animate-pulse rounded-lg bg-muted" />
    </div>

    <!-- Bento 三格 + scroll reveal（min-w-0 防 ECharts 画布把 grid 撑出横向滚动） -->
    <div v-else-if="data" class="grid min-w-0 gap-4 lg:grid-cols-2">
      <!-- 大格：借出趋势 -->
      <Reveal class="min-w-0 lg:col-span-2">
        <Card class="min-w-0 overflow-hidden">
          <CardHeader class="flex-col items-stretch gap-3 sm:flex-row sm:items-center sm:justify-between">
            <CardTitle>借出趋势 · 近 30 天</CardTitle>
            <div class="grid grid-cols-3 gap-2 sm:gap-3">
              <BentoStat label="总借出" :value="trendTotal" tone="info" class="min-w-0" />
              <BentoStat label="有借出天数" :value="trendActiveDays" class="min-w-0" />
              <BentoStat
                label="峰值单日"
                :value="trendPeak.count"
                :hint="trendPeak.date || undefined"
                tone="success"
                class="min-w-0"
              />
            </div>
          </CardHeader>
          <CardContent class="min-w-0">
            <div v-if="trendTotal > 0" class="h-56 min-w-0">
              <LineChart :data="trend" />
            </div>
            <EmptyState
              v-else
              title="近 30 天暂无借出"
              description="发生借阅后趋势图会在这里生成"
              class="py-8"
            />
          </CardContent>
        </Card>
      </Reveal>

      <!-- 中格：分类占比 -->
      <Reveal :delay="0.06" class="min-w-0">
        <Card class="h-full min-w-0 overflow-hidden">
          <CardHeader>
            <CardTitle>分类占比</CardTitle>
          </CardHeader>
          <CardContent>
            <div v-if="data.categoryRatio.length > 0" class="h-64">
              <PieChart :data="data.categoryRatio" />
            </div>
            <EmptyState
              v-else
              title="暂无图书"
              description="馆藏入库后这里会展示分类分布"
              class="py-6"
            />
          </CardContent>
        </Card>
      </Reveal>

      <!-- 右格：借阅排行榜 -->
      <Reveal :delay="0.12" class="min-w-0">
        <Card class="h-full min-w-0 overflow-hidden">
          <CardHeader>
            <CardTitle>借阅排行 · 前 10</CardTitle>
          </CardHeader>
          <CardContent>
            <EmptyState
              v-if="data.topBorrowed.length === 0"
              title="暂无借阅数据"
              description="发生借阅后这里会生成排行"
              class="py-6"
            />
            <ol v-else class="space-y-2">
              <li v-for="(t, i) in data.topBorrowed" :key="t.id" class="flex items-center gap-3 text-sm">
                <span
                  class="flex size-6 shrink-0 items-center justify-center rounded-sm bg-muted text-xs font-semibold tabular-nums"
                  :class="i < 3 ? 'text-primary' : 'text-muted-foreground'"
                >
                  {{ i + 1 }}
                </span>
                <RouterLink :to="`/books/${t.id}`" class="min-w-0 flex-1 truncate hover:text-primary">
                  {{ t.title }}
                  <span class="ml-1.5 text-xs text-muted-foreground">{{ t.author }}</span>
                </RouterLink>
                <span class="tabular-nums text-muted-foreground">{{ t.count }} 次</span>
              </li>
            </ol>
          </CardContent>
        </Card>
      </Reveal>
    </div>
  </div>
</template>
