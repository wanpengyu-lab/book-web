<script setup lang="ts">
/** 趋势折线图（vue-echarts，按需注册见 lib/echarts.ts） */
import { computed } from 'vue'
import type { TrendPoint } from '@/api/types'
import { CHART_AXIS, CHART_PALETTE, CHART_TEXT, VChart } from '@/lib/echarts'

const props = defineProps<{ data: TrendPoint[]; height?: string }>()

const option = computed(() => ({
  color: CHART_PALETTE,
  tooltip: { trigger: 'axis' },
  grid: { left: 32, right: 16, top: 24, bottom: 28 },
  xAxis: {
    type: 'category',
    data: props.data.map((p) => p.date.slice(5)), // MM-DD
    axisLine: { lineStyle: { color: CHART_AXIS } },
    axisTick: { show: false },
    axisLabel: { color: CHART_TEXT, fontSize: 10, interval: 5 },
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    axisLabel: { color: CHART_TEXT, fontSize: 10 },
    splitLine: { lineStyle: { color: CHART_AXIS } },
  },
  series: [
    {
      name: '借出',
      type: 'line',
      data: props.data.map((p) => p.count),
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { width: 2 },
      // 纯色低透明度衬托，不用渐变（设计基准：渐变仅登录页/空态）
      areaStyle: { opacity: 0.12 },
    },
  ],
}))
</script>

<template>
  <VChart class="h-full w-full" :style="{ height: props.height ?? '100%' }" :option="option" autoresize />
</template>
