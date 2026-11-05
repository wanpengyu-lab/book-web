<script setup lang="ts">
/**
 * /login：登录表单（3D 背景位留空，v0.9 接 TresJS 粒子场）
 * mock 阶段不校验密码：admin / zhangsan / lisi 任一用户名 + 任意密码即可进入
 */
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '@/api/types'
import Button from '@/components/ui/button/Button.vue'
import Card from '@/components/ui/card/Card.vue'
import CardContent from '@/components/ui/card/CardContent.vue'
import CardDescription from '@/components/ui/card/CardDescription.vue'
import CardHeader from '@/components/ui/card/CardHeader.vue'
import CardTitle from '@/components/ui/card/CardTitle.vue'
import Input from '@/components/ui/input/Input.vue'
import { BookIcon } from '@/components/icons'
import { toastError } from '@/components/ui/toast/use-toast'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const username = ref('')
const password = ref('')
const submitting = ref(false)

async function onSubmit(): Promise<void> {
  if (!username.value.trim() || !password.value) {
    toastError('请输入用户名与密码', '登录')
    return
  }
  submitting.value = true
  try {
    await userStore.login(username.value.trim(), password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (e) {
    toastError(e instanceof ApiError ? e.message : '登录失败，请稍后重试', '登录')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="relative flex min-h-screen items-center justify-center px-4">
    <!-- 3D 背景位：v0.9 挂 TresJS 粒子场（路由级懒加载 chunk） -->
    <div class="pointer-events-none absolute inset-0" aria-hidden="true"></div>

    <Card class="w-full max-w-sm">
      <CardHeader class="items-start">
        <div class="mb-2 flex items-center gap-2 text-lg font-semibold tracking-tight">
          <BookIcon class="size-5 text-primary" />
          <span>boook<span class="text-primary">·</span>web</span>
        </div>
        <CardTitle>登录图书馆系统</CardTitle>
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
            <Input id="password" v-model="password" type="password" placeholder="••••••" autocomplete="current-password" />
          </div>
          <Button type="submit" class="w-full" :disabled="submitting">
            {{ submitting ? '登录中…' : '登录' }}
          </Button>
          <p class="text-center text-xs text-muted-foreground">
            mock 演示：输入用户名 admin / zhangsan / lisi，密码任意即可进入
          </p>
        </form>
      </CardContent>
    </Card>
  </div>
</template>
