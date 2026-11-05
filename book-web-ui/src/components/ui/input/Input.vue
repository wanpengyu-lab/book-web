<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

const model = defineModel<string | number>()

const props = defineProps<{ class?: string; placeholder?: string; type?: string }>()

const delegatedClass = computed(() =>
  cn(
    'flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-rest transition-colors',
    'placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/60',
    'disabled:cursor-not-allowed disabled:opacity-50',
    props.class,
  ),
)
</script>

<template>
  <input v-model="model" :type="type ?? 'text'" :placeholder="placeholder" :class="delegatedClass" />
</template>
