<script setup lang="ts">
/**
 * Bento 统计格：只用于统计条 / 看板 / 个人中心（PRD v0.8 强制规范）
 * 数据表一律使用紧凑表格，禁止套 Bento。
 */
import { computed } from 'vue'
import { cn } from '@/lib/utils'

const props = withDefaults(
  defineProps<{
    label: string
    value: number | string
    hint?: string
    tone?: 'default' | 'success' | 'info' | 'danger' | 'warning'
    class?: string
  }>(),
  { tone: 'default' },
)

const valueClass = computed(() => {
  switch (props.tone) {
    case 'success':
      return 'text-success'
    case 'info':
      return 'text-info'
    case 'danger':
      return 'text-danger'
    case 'warning':
      return 'text-warning'
    default:
      return 'text-foreground'
  }
})

const delegatedClass = computed(() =>
  cn(
    'rounded-lg border border-border bg-card p-4 shadow-rest transition-colors hover:shadow-hover',
    props.class,
  ),
)
</script>

<template>
  <div :class="delegatedClass">
    <p class="text-xs text-muted-foreground">{{ label }}</p>
    <p class="mt-1 text-2xl font-semibold tracking-tight tabular-nums" :class="valueClass">
      {{ value }}
    </p>
    <p v-if="hint" class="mt-0.5 truncate text-xs text-muted-foreground">{{ hint }}</p>
  </div>
</template>
