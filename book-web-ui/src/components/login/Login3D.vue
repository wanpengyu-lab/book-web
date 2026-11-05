<script setup lang="ts">
/**
 * /login 3D 粒子场（PRD v0.9）：TresJS + three，路由级懒加载 chunk（主包不含 three）
 * - 桌面 800 粒子 / 移动端 200 粒子
 * - 鼠标视差 ±8px（CSS transform，屏幕空间精确 8px）
 * - prefers-reduced-motion → 静态渐变网格（渐变仅登录页允许）
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { TresCanvas } from '@tresjs/core'
import ParticleField from '@/components/login/ParticleField.vue'
import { isMobileViewport, prefersReducedMotion } from '@/lib/media'

const reduced = prefersReducedMotion()
const count = isMobileViewport() ? 200 : 800

/* 鼠标视差 ±8px：作用于 canvas 容器的 CSS transform */
const parallaxX = ref(0)
const parallaxY = ref(0)

function onMouseMove(e: MouseEvent): void {
  const nx = e.clientX / window.innerWidth - 0.5
  const ny = e.clientY / window.innerHeight - 0.5
  parallaxX.value = nx * 16 // 半幅 ±8px
  parallaxY.value = ny * 16
}

onMounted(() => {
  if (reduced) return
  window.addEventListener('mousemove', onMouseMove)
})
onBeforeUnmount(() => window.removeEventListener('mousemove', onMouseMove))

const parallaxStyle = computed(() => ({
  transform: `translate3d(${parallaxX.value.toFixed(1)}px, ${parallaxY.value.toFixed(1)}px, 0)`,
  transition: 'transform 0.15s ease-out',
}))
</script>

<template>
  <!-- 静态渐变网格降级（prefers-reduced-motion） -->
  <div
    v-if="reduced"
    class="pointer-events-none absolute inset-0"
    aria-hidden="true"
    :style="{
      backgroundImage:
        'linear-gradient(rgba(139,124,247,0.08) 1px, transparent 1px), linear-gradient(90deg, rgba(139,124,247,0.08) 1px, transparent 1px)',
      backgroundSize: '40px 40px',
      maskImage: 'radial-gradient(ellipse at center, black 30%, transparent 75%)',
      WebkitMaskImage: 'radial-gradient(ellipse at center, black 30%, transparent 75%)',
    }"
  />

  <!-- TresJS 粒子场（three 只进本组件的懒加载 chunk） -->
  <div
    v-else
    class="pointer-events-none absolute inset-0 overflow-hidden"
    :style="parallaxStyle"
    aria-hidden="true"
  >
    <TresCanvas :alpha="true" :antialias="true">
      <TresPerspectiveCamera :position="[0, 0, 6]" :fov="60" />
      <ParticleField :count="count" />
    </TresCanvas>
  </div>
</template>
