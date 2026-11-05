<script setup lang="ts">
/**
 * 状态标签唯一产出（PRD v0.8 强制规范）：在馆 / 在借 / 逾期 / 预约中
 * 全站任何地方需要这四个状态标签，一律引用本组件 + lib/status.ts
 */
import { computed } from 'vue'
import { cn } from '@/lib/utils'
import { STATUS_META, type StatusKind } from '@/lib/status'

const props = defineProps<{ kind: StatusKind; class?: string }>()

const label = computed(() => STATUS_META[props.kind].label)

const kindClass = computed(() => {
  switch (props.kind) {
    case 'available':
      return 'bg-success-soft text-success border-success/30'
    case 'borrowed':
      return 'bg-info-soft text-info border-info/30'
    case 'overdue':
      return 'bg-danger-soft text-danger border-danger/30'
    case 'reserved':
      return 'bg-warning-soft text-warning border-warning/30'
  }
})

const delegatedClass = computed(() =>
  cn(
    'inline-flex items-center rounded-sm border px-2 py-0.5 text-xs font-medium leading-4 whitespace-nowrap',
    kindClass.value,
    props.class,
  ),
)
</script>

<template>
  <span :class="delegatedClass">{{ label }}</span>
</template>
