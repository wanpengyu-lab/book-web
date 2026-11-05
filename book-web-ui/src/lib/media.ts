/** 媒体偏好工具：prefers-reduced-motion / 移动端断点（PRD v0.9 动效与 3D 降级共用） */
export function prefersReducedMotion(): boolean {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

export function isMobileViewport(): boolean {
  return window.innerWidth < 768
}
