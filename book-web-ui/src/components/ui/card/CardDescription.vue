<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<{ class?: string }>()
const delegatedClass = computed(() => cn('text-sm text-muted-foreground', props.class))
</script>

<template>
  <p :class="delegatedClass">
    <slot />
  </p>
</template>
