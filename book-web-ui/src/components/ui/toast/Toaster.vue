<script setup lang="ts">
/** shadcn-vue Toast（Reka UI 基座）：右下角堆叠，状态源见 use-toast.ts */
import { ToastDescription, ToastProvider, ToastRoot, ToastTitle, ToastViewport } from 'reka-ui'
import { cn } from '@/lib/utils'
import { dismissToast, toastState } from './use-toast'
</script>

<template>
  <ToastProvider>
    <ToastRoot
      v-for="t in toastState.toasts"
      :key="t.id"
      open
      :class="
        cn(
          'pointer-events-auto relative z-[100] flex w-full items-start gap-3 rounded-lg border border-border p-4 shadow-hover',
          'bg-card text-card-foreground',
          t.variant === 'destructive' && 'border-destructive/50',
        )
      "
      @update:open="(o: boolean) => !o && dismissToast(t.id)"
    >
      <div class="flex-1 space-y-1">
        <ToastTitle v-if="t.title" class="text-sm font-semibold">
          {{ t.title }}
        </ToastTitle>
        <ToastDescription v-if="t.description" class="whitespace-pre-line text-sm text-muted-foreground">
          {{ t.description }}
        </ToastDescription>
      </div>
    </ToastRoot>
    <ToastViewport class="fixed bottom-4 right-4 z-[100] flex w-80 max-w-[calc(100vw-2rem)] flex-col gap-2 outline-none" />
  </ToastProvider>
</template>
