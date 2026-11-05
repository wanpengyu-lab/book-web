<script setup lang="ts">
/** EmptyState：全站空态统一出口（图标 / 标题 / 描述） */
import { computed } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<{
  title: string
  description?: string
  class?: string
}>()

const delegatedClass = computed(() =>
  cn('flex flex-col items-center justify-center gap-2 rounded-lg px-6 py-12 text-center', props.class),
)
</script>

<template>
  <div :class="delegatedClass">
    <div v-if="$slots.icon" class="text-muted-foreground/60 [&_svg]:size-8">
      <slot name="icon" />
    </div>
    <p class="text-sm font-medium text-foreground">{{ title }}</p>
    <p v-if="description" class="max-w-sm text-sm text-muted-foreground">{{ description }}</p>
  </div>
</template>
