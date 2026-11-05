<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<{ class?: string }>()
const delegatedClass = computed(() => cn('flex flex-col gap-1.5 p-5', props.class))
</script>

<template>
  <div :class="delegatedClass">
    <slot />
  </div>
</template>
