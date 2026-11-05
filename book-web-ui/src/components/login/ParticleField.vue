<script setup lang="ts">
/**
 * TresJS 粒子场：three BufferGeometry 点云，缓慢自转
 * 必须挂在 TresCanvas 内（useLoop 依赖 canvas 上下文）
 */
import { useLoop } from '@tresjs/core'
import * as THREE from 'three'
import { onBeforeUnmount } from 'vue'

const props = withDefaults(defineProps<{ count?: number }>(), { count: 800 })

// 球形壳分布，视觉上更「星尘」
const positions = new Float32Array(props.count * 3)
for (let i = 0; i < props.count; i += 1) {
  const radius = 2.2 + Math.random() * 2.4
  const theta = Math.random() * Math.PI * 2
  const phi = Math.acos(2 * Math.random() - 1)
  positions[i * 3] = radius * Math.sin(phi) * Math.cos(theta)
  positions[i * 3 + 1] = radius * Math.sin(phi) * Math.sin(theta)
  positions[i * 3 + 2] = radius * Math.cos(phi)
}

const geometry = new THREE.BufferGeometry()
geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))

const material = new THREE.PointsMaterial({
  size: 0.05,
  color: 0x8b7cf7,
  transparent: true,
  opacity: 0.8,
  sizeAttenuation: true,
  depthWrite: false,
  // 圆形粒子贴图（canvas 生成），避免默认方块
  map: makeCircleTexture(),
  alphaTest: 0.2,
})

function makeCircleTexture(): THREE.CanvasTexture {
  const size = 64
  const canvas = document.createElement('canvas')
  canvas.width = size
  canvas.height = size
  const ctx = canvas.getContext('2d')!
  ctx.fillStyle = '#fff'
  ctx.beginPath()
  ctx.arc(size / 2, size / 2, size / 2 - 2, 0, Math.PI * 2)
  ctx.fill()
  return new THREE.CanvasTexture(canvas)
}

const points = new THREE.Points(geometry, material)

const { onBeforeRender, stop } = useLoop()
onBeforeRender(({ delta }) => {
  points.rotation.y += delta * 0.06
  points.rotation.x += delta * 0.012
})
onBeforeUnmount(() => {
  stop()
  geometry.dispose()
  material.dispose()
})
</script>

<template>
  <primitive :object="points" />
</template>
