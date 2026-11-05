<script setup lang="ts">
/** 表格加载骨架行：数据未返回时替代文字 loading */
import { cn } from '@/lib/utils'

const props = withDefaults(defineProps<{ cols?: number; class?: string }>(), { cols: 5 })
</script>

<template>
  <tr :class="cn(props.class)">
    <td v-for="i in props.cols" :key="i" class="px-3 py-3">
      <div class="h-3.5 animate-pulse rounded-sm bg-muted" :style="{ width: `${60 + ((i * 13) % 35)}%` }" />
    </td>
  </tr>
</template>
