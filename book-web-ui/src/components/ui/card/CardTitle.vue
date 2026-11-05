<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<{ class?: string }>()
const delegatedClass = computed(() => cn('text-base font-semibold leading-none tracking-tight', props.class))
</script>

<template>
  <div :class="delegatedClass">
    <slot />
  </div>
</template>
