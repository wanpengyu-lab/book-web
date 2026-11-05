<script setup lang="ts">
/** 图书卡片（卡片视图）：封面占位 + 元信息 + 状态标签，点击进详情
 * 动效 2：hover 边框提亮 + 阴影加深，位移 2px（-translate-y-0.5）
 */
import { computed } from 'vue'
import type { Book } from '@/api/types'
import StatusTag from '@/components/StatusTag.vue'
import { bookStatusKind } from '@/lib/status'

const props = defineProps<{ book: Book }>()

const initials = computed(() => props.book.title.slice(0, 2))
</script>

<template>
  <RouterLink
    :to="`/books/${book.id}`"
    class="group flex gap-4 rounded-lg border border-border bg-card p-4 shadow-rest transition-all duration-150 hover:-translate-y-0.5 hover:border-ring/50 hover:shadow-hover"
  >
    <div
      class="flex h-24 w-16 shrink-0 items-center justify-center rounded-sm bg-muted text-sm font-semibold text-muted-foreground select-none"
    >
      {{ initials }}
    </div>
    <div class="flex min-w-0 flex-1 flex-col justify-between gap-2">
      <div class="min-w-0">
        <p class="truncate text-sm font-semibold text-foreground group-hover:text-primary">
          {{ book.title }}
        </p>
        <p class="mt-1 truncate text-xs text-muted-foreground">{{ book.author }}</p>
      </div>
      <div class="flex items-center justify-between gap-2">
        <span class="rounded-sm bg-muted px-1.5 py-0.5 text-xs text-muted-foreground">
          {{ book.category }}
        </span>
        <StatusTag :kind="bookStatusKind(book.borrowed)" />
      </div>
    </div>
  </RouterLink>
</template>
