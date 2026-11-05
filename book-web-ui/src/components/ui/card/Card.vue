<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<{ class?: string }>()
const delegatedClass = computed(() =>
  cn('rounded-lg border border-border bg-card text-card-foreground shadow-rest', props.class),
)
</script>

<template>
  <div :class="delegatedClass">
    <slot />
  </div>
</template>
