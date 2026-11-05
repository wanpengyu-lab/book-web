<script setup lang="ts">
import { computed } from 'vue'
import { cva, type VariantProps } from 'class-variance-authority'
import { LoaderIcon } from '@/components/icons'
import { cn } from '@/lib/utils'

const buttonVariants = cva(
  'inline-flex cursor-pointer items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/60 disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0',
  {
    variants: {
      variant: {
        default: 'bg-primary text-primary-foreground shadow-rest hover:bg-primary/90',
        secondary: 'bg-muted text-foreground hover:bg-accent',
        outline: 'border border-border bg-transparent shadow-rest hover:bg-accent hover:text-accent-foreground',
        ghost: 'hover:bg-accent hover:text-accent-foreground',
        destructive: 'bg-destructive text-destructive-foreground shadow-rest hover:bg-destructive/90',
        link: 'text-primary underline-offset-4 hover:underline',
      },
      size: {
        // <768px 触控目标 ≥44px（PRD v0.9 响应式）
        default: 'h-9 px-4 py-2 max-md:h-11',
        sm: 'h-8 rounded-sm px-3 text-xs max-md:h-11 max-md:px-4',
        lg: 'h-10 rounded-md px-6 max-md:h-11',
        icon: 'size-9 max-md:size-11',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  },
)

type ButtonVariants = VariantProps<typeof buttonVariants>

const props = defineProps<{
  variant?: ButtonVariants['variant']
  size?: ButtonVariants['size']
  class?: string
  loading?: boolean
  disabled?: boolean
}>()

const delegatedClass = computed(() =>
  cn(buttonVariants({ variant: props.variant, size: props.size }), props.class),
)
</script>

<template>
  <button :class="delegatedClass" :disabled="disabled || loading">
    <LoaderIcon v-if="loading" class="animate-spin" />
    <slot />
  </button>
</template>
