<script setup lang="ts">
/** 分类占比环形图（vue-echarts，按需注册见 lib/echarts.ts） */
import { computed } from 'vue'
import type { CategoryRatio } from '@/api/types'
import { CHART_PALETTE, CHART_TEXT, VChart } from '@/lib/echarts'

const props = defineProps<{ data: CategoryRatio[]; height?: string }>()

const option = computed(() => ({
  color: CHART_PALETTE,
  tooltip: { trigger: 'item' },
  legend: {
    bottom: 0,
    left: 'center',
    textStyle: { color: CHART_TEXT, fontSize: 11 },
    itemWidth: 10,
    itemHeight: 10,
  },
  series: [
    {
      name: '分类占比',
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '44%'],
      data: props.data.map((c) => ({ name: c.name, value: c.value })),
      label: { color: CHART_TEXT, fontSize: 11, formatter: '{b} {c}' },
      itemStyle: { borderWidth: 2, borderColor: 'rgba(0,0,0,0)' },
    },
  ],
}))
</script>

<template>
  <VChart class="h-full w-full" :style="{ height: props.height ?? '100%' }" :option="option" autoresize />
</template>
