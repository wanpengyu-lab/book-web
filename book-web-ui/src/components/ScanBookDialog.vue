<script setup lang="ts">
/**
 * 扫码建档（PRD v0.9）：
 * - 'BarcodeDetector' in window 且摄像头可用 → 摄像头扫码
 * - 否则 ISBN 手输降级（无 BarcodeDetector / 授权被拒均降级）
 * - 查到（mock ISBN 查询）→ 预填 Dialog 表单，允许修正后提交建档
 */
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import * as booksApi from '@/api/books'
import { ApiError, type IsbnInfo } from '@/api/types'
import Button from '@/components/ui/button/Button.vue'
import Dialog from '@/components/ui/dialog/Dialog.vue'
import Input from '@/components/ui/input/Input.vue'
import { ScanIcon } from '@/components/icons'
import { toast, toastError } from '@/components/ui/toast/use-toast'

const open = defineModel<boolean>('open', { default: false })

const emit = defineEmits<{ saved: [] }>()

/** BarcodeDetector 类型不在标准 lib.dom 中，本地最小声明 */
interface BarcodeDetectorLike {
  detect(source: CanvasImageSource): Promise<{ rawValue: string }[]>
}
type BarcodeDetectorCtor = new (options?: { formats?: string[] }) => BarcodeDetectorLike

const barcodeSupported = computed(() => 'BarcodeDetector' in window)

const mode = ref<'idle' | 'camera' | 'manual'>('idle')
const cameraError = ref('')
const isbnInput = ref('')

const videoRef = ref<HTMLVideoElement | null>(null)
let stream: MediaStream | null = null
let scanTimer: ReturnType<typeof setInterval> | undefined

/* ------- 建档表单（预填后可修正） ------- */
const form = ref({
  title: '',
  author: '',
  category: '',
  publisher: '',
  isbn: '',
  coverUrl: '',
  description: '',
  totalCopies: 1,
})
const looking = ref(false)
const submitting = ref(false)

watch(open, async (value) => {
  if (value) {
    reset()
    await nextTick()
    await tryStartCamera()
  } else {
    stopCamera()
  }
})

onBeforeUnmount(stopCamera)

function reset(): void {
  mode.value = 'idle'
  cameraError.value = ''
  isbnInput.value = ''
  form.value = { title: '', author: '', category: '', publisher: '', isbn: '', coverUrl: '', description: '', totalCopies: 1 }
}

function stopCamera(): void {
  if (scanTimer) clearInterval(scanTimer)
  scanTimer = undefined
  stream?.getTracks().forEach((t) => t.stop())
  stream = null
}

async function tryStartCamera(): Promise<void> {
  if (!barcodeSupported.value) {
    mode.value = 'manual'
    cameraError.value = '当前浏览器不支持 BarcodeDetector，已切换为 ISBN 手输'
    return
  }
  mode.value = 'camera'
  try {
    stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } })
    await nextTick()
    if (videoRef.value) {
      videoRef.value.srcObject = stream
      await videoRef.value.play().catch(() => undefined)
    }
    const Ctor = (window as unknown as { BarcodeDetector: BarcodeDetectorCtor }).BarcodeDetector
    const detector = new Ctor({ formats: ['ean_13', 'ean_8'] })
    scanTimer = setInterval(async () => {
      if (!videoRef.value || !open.value) return
      try {
        const codes = await detector.detect(videoRef.value)
        const raw = codes[0]?.rawValue?.replace(/\D/g, '')
        if (raw) {
          isbnInput.value = raw
          stopCamera()
          void lookup()
        }
      } catch {
        // 单帧识别失败忽略，继续下一帧
      }
    }, 350)
  } catch {
    cameraError.value = '摄像头不可用（未授权或无设备），已切换为 ISBN 手输'
    mode.value = 'manual'
  }
}

function switchToManual(): void {
  stopCamera()
  mode.value = 'manual'
}

/** mock ISBN 查询：命中预填表单；未命中/失败按契约 message 提示 */
async function lookup(): Promise<void> {
  const isbn = isbnInput.value.trim()
  if (!/^\d{1,20}$/.test(isbn)) {
    toastError('ISBN 需为 1–20 位纯数字', '扫码建档')
    return
  }
  looking.value = true
  try {
    const hit: IsbnInfo | null = await booksApi.isbnLookup(isbn)
    if (!hit) {
      toastError('未查到该书目，请手动录入', '扫码建档')
      return
    }
    form.value = {
      title: hit.title,
      author: hit.author,
      category: form.value.category,
      publisher: hit.publisher,
      isbn,
      coverUrl: hit.coverUrl || '',
      description: hit.description,
      totalCopies: 1,
    }
    toast({ title: '已回填书目信息', description: '请核对并修正后提交建档' })
  } catch (e) {
    toastError(e instanceof ApiError ? e.message : '查询失败，请稍后重试', '扫码建档')
  } finally {
    looking.value = false
  }
}

async function submit(): Promise<void> {
  if (!form.value.title.trim() || !form.value.author.trim()) {
    toastError('书名与作者必填', '扫码建档')
    return
  }
  submitting.value = true
  try {
    await booksApi.create({
      title: form.value.title.trim(),
      author: form.value.author.trim(),
      category: form.value.category.trim() || undefined,
      publisher: form.value.publisher || undefined,
      isbn: form.value.isbn || undefined,
      coverUrl: form.value.coverUrl || undefined,
      description: form.value.description || undefined,
      totalCopies: form.value.totalCopies,
    })
    toast({ title: '建档成功', description: `《${form.value.title}》已入馆` })
    emit('saved')
    open.value = false
  } catch (e) {
    toastError(e instanceof ApiError ? e.message : '建档失败，请稍后重试', '扫码建档')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <Dialog v-model:open="open" title="扫码建档" description="扫 ISBN 条码或手输编号，查询成功后回填建档表单">
    <!-- 第一步：条码 / 手输 -->
    <div v-if="mode !== 'manual' || !form.title" class="space-y-3">
      <div v-if="mode === 'camera'" class="space-y-2">
        <div class="relative aspect-video overflow-hidden rounded-md border border-border bg-black">
          <video ref="videoRef" class="h-full w-full object-cover" muted playsinline />
        </div>
        <p class="text-xs text-muted-foreground">对准图书背面 ISBN 条码，识别后自动查询</p>
      </div>
      <p v-if="cameraError" class="rounded-md bg-warning-soft px-3 py-2 text-xs text-warning">
        {{ cameraError }}
      </p>
      <div class="flex items-end gap-2">
        <div class="flex-1 space-y-1.5">
          <label class="text-sm font-medium" for="scan-isbn">ISBN</label>
          <Input
            id="scan-isbn"
            v-model="isbnInput"
            inputmode="numeric"
            placeholder="如 9787536692930"
            @keydown.enter="lookup"
          />
        </div>
        <Button :loading="looking" @click="lookup">查询</Button>
      </div>
      <Button v-if="mode === 'camera'" variant="ghost" size="sm" @click="switchToManual">
        改为手动输入
      </Button>
    </div>

    <!-- 第二步：预填表单（可修正） -->
    <div v-else class="space-y-3">
      <div class="grid gap-3 sm:grid-cols-2">
        <div class="space-y-1.5">
          <label class="text-sm font-medium" for="scan-title">书名 *</label>
          <Input id="scan-title" v-model="form.title" />
        </div>
        <div class="space-y-1.5">
          <label class="text-sm font-medium" for="scan-author">作者 *</label>
          <Input id="scan-author" v-model="form.author" />
        </div>
        <div class="space-y-1.5">
          <label class="text-sm font-medium" for="scan-category">分类</label>
          <Input id="scan-category" v-model="form.category" placeholder="默认「未分类」" />
        </div>
        <div class="space-y-1.5">
          <label class="text-sm font-medium" for="scan-publisher">出版社</label>
          <Input id="scan-publisher" v-model="form.publisher" />
        </div>
        <div class="space-y-1.5">
          <label class="text-sm font-medium" for="scan-isbn2">ISBN</label>
          <Input id="scan-isbn2" v-model="form.isbn" />
        </div>
        <div class="space-y-1.5">
          <label class="text-sm font-medium" for="scan-copies">复本数</label>
          <Input id="scan-copies" v-model.number="form.totalCopies" type="number" min="1" />
        </div>
      </div>
      <div class="space-y-1.5">
        <label class="text-sm font-medium" for="scan-cover">封面地址</label>
        <Input id="scan-cover" v-model="form.coverUrl" placeholder="https://…" />
      </div>
      <div class="space-y-1.5">
        <label class="text-sm font-medium" for="scan-desc">简介</label>
        <textarea
          id="scan-desc"
          v-model="form.description"
          rows="3"
          class="w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/60"
        />
      </div>
    </div>

    <template #footer>
      <Button variant="ghost" @click="open = false">取消</Button>
      <Button
        v-if="form.title"
        :loading="submitting"
        @click="submit"
      >
        <ScanIcon />
        提交建档
      </Button>
    </template>
  </Dialog>
</template>
