<script setup lang="ts">
/**
 * shadcn-vue Dialog（Reka UI 无头基座）：受控 v-model:open 复合封装
 * 用法：<Dialog v-model:open="open" title="…" description="…">…<template #footer>…</template></Dialog>
 */
import {
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogOverlay,
  DialogPortal,
  DialogRoot,
  DialogTitle,
} from 'reka-ui'
import { XIcon } from '@/components/icons'

const open = defineModel<boolean>('open', { default: false })

defineProps<{
  title?: string
  description?: string
}>()
</script>

<template>
  <DialogRoot :open="open" @update:open="open = $event">
    <DialogPortal>
      <DialogOverlay class="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm" />
      <DialogContent
        class="fixed left-1/2 top-1/2 z-50 max-h-[85vh] w-[calc(100%-2rem)] max-w-lg -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-lg border border-border bg-card p-6 text-card-foreground shadow-hover focus:outline-none"
      >
        <div class="mb-4 flex items-start justify-between gap-4">
          <div class="space-y-1">
            <DialogTitle v-if="title" class="text-lg font-semibold leading-none tracking-tight">
              {{ title }}
            </DialogTitle>
            <DialogDescription v-if="description" class="text-sm text-muted-foreground">
              {{ description }}
            </DialogDescription>
          </div>
          <DialogClose
            class="cursor-pointer rounded-sm text-muted-foreground opacity-70 transition-opacity hover:opacity-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring/60"
          >
            <XIcon class="size-4" />
            <span class="sr-only">关闭</span>
          </DialogClose>
        </div>
        <slot />
        <div v-if="$slots.footer" class="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <slot name="footer" />
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>
