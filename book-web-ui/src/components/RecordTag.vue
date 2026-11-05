<script setup lang="ts">
/**
 * 借阅记录状态渲染：四个状态标签（在借/逾期）唯一经 StatusTag 产出；
 * 「已归还」按规范用普通文案（不属于四状态标签体系）。
 */
import { computed } from 'vue'
import type { BorrowRecord } from '@/api/types'
import StatusTag from '@/components/StatusTag.vue'
import { recordStatusKind, type StatusKind } from '@/lib/status'

const props = defineProps<{ record: BorrowRecord }>()

type RecordTagView = { returned: true } | { returned: false; kind: StatusKind }

const view = computed<RecordTagView>(() => {
  const kind = recordStatusKind(props.record)
  return kind === 'returned' ? { returned: true } : { returned: false, kind }
})
</script>

<template>
  <StatusTag v-if="!view.returned" :kind="view.kind" />
  <span v-else class="text-xs text-muted-foreground">已归还</span>
</template>
