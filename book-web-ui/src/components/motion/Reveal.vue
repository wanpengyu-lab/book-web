<script setup lang="ts">
/**
 * scroll reveal 包装（动效 4）：进入视口后 fade+16px 上移，仅一次
 * prefers-reduced-motion → 直接显示，不播动画（PRD v0.9）
 */
import { motion, useReducedMotion } from 'motion-v'

const props = withDefaults(
  defineProps<{
    delay?: number
    class?: string
  }>(),
  { delay: 0 },
)

const reduced = useReducedMotion()
</script>

<template>
  <motion.div
    :class="props.class"
    :initial="reduced ? false : { opacity: 0, y: 16 }"
    :while-in-view="reduced ? undefined : { opacity: 1, y: 0 }"
    :viewport="{ once: true, margin: '-40px' }"
    :transition="{ duration: 0.35, delay: reduced ? 0 : props.delay, ease: 'easeOut' }"
  >
    <slot />
  </motion.div>
</template>
