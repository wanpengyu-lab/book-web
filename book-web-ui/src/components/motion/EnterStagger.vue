<script setup lang="ts">
/**
 * 卡片入场 stagger 包装（动效 1）：fade + 8px，40ms/项，仅首次进入页面时播放
 * prefers-reduced-motion → 直接显示（PRD v0.9）
 */
import { computed } from 'vue'
import { motion, useReducedMotion } from 'motion-v'

const props = withDefaults(
  defineProps<{
    index?: number
    /** 父组件在首次数据到达后置 false，翻页/筛选不再重播（仅首次） */
    animate?: boolean
    class?: string
  }>(),
  { index: 0, animate: true },
)

const reduced = useReducedMotion()

const shouldAnimate = computed(() => props.animate && !reduced.value)

const delay = computed(() => {
  // 30–50ms/项（PRD 取 40ms），上限 400ms 防长列表拖沓
  return Math.min(props.index * 0.04, 0.4)
})
</script>

<template>
  <motion.div
    :class="props.class"
    :initial="shouldAnimate ? { opacity: 0, y: 8 } : false"
    :animate="shouldAnimate ? { opacity: 1, y: 0 } : undefined"
    :transition="{ duration: 0.28, delay, ease: 'easeOut' }"
  >
    <slot />
  </motion.div>
</template>
