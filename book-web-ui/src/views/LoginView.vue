<script setup lang="ts">
/**
 * /login：登录 / 注册表单（3D 背景位留空，v0.9 接 TresJS 粒子场）
 * v1.0.1：补「登录 ⇄ 注册」切换——注册调用既有 authApi.register（契约 §1，后端零改动）。
 * 客户端校验与后端 RegisterRequest @Valid 镜像同文案
 * （username 非空 ≤50 / password 非空 ≤100；后端文案格式 = 字段名 + 空格 + 消息）。
 * mock 阶段不校验密码：admin / zhangsan / lisi 任一用户名 + 任意密码即可进入
 */
import { ref, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '@/api/types'
import { USE_MOCK } from '@/api/http'
import * as authApi from '@/api/auth'
import Button from '@/components/ui/button/Button.vue'
import Card from '@/components/ui/card/Card.vue'
import CardContent from '@/components/ui/card/CardContent.vue'
import CardDescription from '@/components/ui/card/CardDescription.vue'
import CardHeader from '@/components/ui/card/CardHeader.vue'
import CardTitle from '@/components/ui/card/CardTitle.vue'
import Input from '@/components/ui/input/Input.vue'
import { BookIcon } from '@/components/icons'
import { toast, toastError } from '@/components/ui/toast/use-toast'
import { useUserStore } from '@/stores/user'

// 3D 粒子场：路由级懒加载 chunk（three 只出现在该 chunk，主包不含）
const Login3D = defineAsyncComponent(() => import('@/components/login/Login3D.vue'))

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

type Mode = 'login' | 'register'
const mode = ref<Mode>('login')
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const submitting = ref(false)

/** 切换登录/注册：清确认密码，用户名保留（注册成功回登录时即预填） */
function switchMode(next: Mode): void {
  mode.value = next
  confirmPassword.value = ''
}

/** 客户端校验：与后端 RegisterRequest @Valid 同规则同文案 */
function validate(): string | null {
  const name = username.value.trim()
  if (!name) return 'username 不能为空'
  if (name.length > 50) return 'username 长度不能超过 50'
  if (!password.value) return 'password 不能为空'
  if (password.value.length > 100) return 'password 长度不能超过 100'
  if (mode.value === 'register' && confirmPassword.value !== password.value) {
    return '两次输入的密码不一致'
  }
  return null
}

async function onSubmit(): Promise<void> {
  const problem = validate()
  if (problem) {
    toastError(problem, mode.value === 'login' ? '登录' : '注册')
    return
  }
  submitting.value = true
  try {
    if (mode.value === 'register') {
      await authApi.register(username.value.trim(), password.value)
      toast({ title: '注册成功，请登录' })
      switchMode('login')
      password.value = ''
      return
    }
    await userStore.login(username.value.trim(), password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (e) {
    // 40901（用户名已存在）等接口错误：原样展示接口 message
    toastError(e instanceof ApiError ? e.message : mode.value === 'login' ? '登录失败，请稍后重试' : '注册失败，请稍后重试', mode.value === 'login' ? '登录' : '注册')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <!-- overflow-hidden：视差层 translate3d ±8px 会被裁在本页内，不再撑出文档滚动条（v1.0.1 实机修复） -->
  <div class="relative flex min-h-screen items-center justify-center overflow-hidden px-4">
    <!-- 3D 背景位：TresJS 粒子场（桌面 800 / 移动 200 粒子；reduced-motion → 静态渐变网格） -->
    <Login3D />

    <Card class="relative z-10 w-full max-w-sm">
      <CardHeader class="items-start">
        <div class="mb-2 flex items-center gap-2 text-lg font-semibold tracking-tight">
          <BookIcon class="size-5 text-primary" />
          <span>boook<span class="text-primary">·</span>web</span>
        </div>
        <CardTitle>{{ mode === 'login' ? '登录图书馆系统' : '注册读者账号' }}</CardTitle>
        <CardDescription>馆藏检索 · 借阅闭环 · 看板与馆藏助手</CardDescription>
      </CardHeader>
      <CardContent>
        <form class="space-y-4" @submit.prevent="onSubmit">
          <div class="space-y-1.5">
            <label for="username" class="text-sm font-medium">用户名</label>
            <Input id="username" v-model="username" placeholder="admin / zhangsan / lisi" autocomplete="username" />
          </div>
          <div class="space-y-1.5">
            <label for="password" class="text-sm font-medium">密码</label>
            <Input id="password" v-model="password" type="password" placeholder="••••••" :autocomplete="mode === 'login' ? 'current-password' : 'new-password'" />
          </div>
          <div v-if="mode === 'register'" class="space-y-1.5">
            <label for="confirm-password" class="text-sm font-medium">确认密码</label>
            <Input id="confirm-password" v-model="confirmPassword" type="password" placeholder="••••••" autocomplete="new-password" />
          </div>
          <Button type="submit" class="w-full" :disabled="submitting">
            {{ submitting ? (mode === 'login' ? '登录中…' : '注册中…') : mode === 'login' ? '登录' : '注册' }}
          </Button>
          <!-- v1.0.1：登录 ⇄ 注册切换 -->
          <p class="text-center text-sm text-muted-foreground">
            <template v-if="mode === 'login'">没有账号？<button type="button" class="text-primary hover:underline" @click="switchMode('register')">去注册</button></template>
            <template v-else>已有账号？<button type="button" class="text-primary hover:underline" @click="switchMode('login')">返回登录</button></template>
          </p>
          <!-- v0.10：mock 提示仅离线 mock 模式显示，真实 API 模式不得误导（E2E 预检修复） -->
          <p v-if="USE_MOCK && mode === 'login'" class="text-center text-xs text-muted-foreground">
            mock 演示：输入用户名 admin / zhangsan / lisi，密码任意即可进入
          </p>
        </form>
      </CardContent>
    </Card>
  </div>
</template>
