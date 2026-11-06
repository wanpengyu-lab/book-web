<script setup lang="ts">
/**
 * /login 3D 粒子场（PRD v0.9）：TresJS + three，路由级懒加载 chunk（主包不含 three）
 * - 桌面 800 粒子 / 移动端 200 粒子
 * - 粒子缓慢自转（ParticleField 内 delta*0.06 环境动画，非鼠标驱动）
 * - prefers-reduced-motion → 静态渐变网格（渐变仅登录页允许）
 * - v1.0.2：用户拍板移除鼠标视差（背景层不再跟随鼠标，overflow-hidden 层保留）
 */
import { TresCanvas } from '@tresjs/core'
import ParticleField from '@/components/login/ParticleField.vue'
import { isMobileViewport, prefersReducedMotion } from '@/lib/media'

const reduced = prefersReducedMotion()
const count = isMobileViewport() ? 200 : 800
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
  <div v-else class="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
    <TresCanvas :alpha="true" :antialias="true">
      <TresPerspectiveCamera :position="[0, 0, 6]" :fov="60" />
      <ParticleField :count="count" />
    </TresCanvas>
  </div>
</template>
