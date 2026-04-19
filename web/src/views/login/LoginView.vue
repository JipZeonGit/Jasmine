<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, User } from '@element-plus/icons-vue'

import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const passwordType = ref<'password' | 'text'>('password')
const formRef = ref()
const form = ref({ username: 'admin', password: '123456' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码不能少于 6 位', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form.value)
    await authStore.loadUserInfo()
    router.replace((route.query.redirect as string) || '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-shell">
    <div class="login-backdrop" />
    <el-card class="login-card" shadow="never">
      <div class="login-eyebrow">Jasmine Flower Shop</div>
      <div class="login-title">欢迎使用小茉莉花店管理系统</div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" :type="passwordType" placeholder="密码" @keyup.enter="handleLogin">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" :loading="loading" class="login-button" @click="handleLogin">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-shell {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(13, 18, 28, 0.68), rgba(19, 31, 44, 0.38)),
    url('@/assets/bg.jpg') center center / cover no-repeat;
}

.login-backdrop {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 18% 22%, rgba(255, 255, 255, 0.18), transparent 32%),
    radial-gradient(circle at 82% 20%, rgba(107, 166, 255, 0.18), transparent 28%),
    linear-gradient(180deg, rgba(6, 10, 18, 0.18), rgba(6, 10, 18, 0.5));
  backdrop-filter: blur(4px);
}

.login-card {
  position: relative;
  z-index: 1;
  width: 400px;
  padding: 12px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(248, 250, 252, 0.88);
  box-shadow: 0 24px 60px rgba(7, 14, 23, 0.22);
}

.login-eyebrow {
  margin-bottom: 10px;
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  text-align: center;
  color: #5d6b7e;
}

.login-title {
  font-size: 28px;
  font-weight: 600;
  text-align: center;
  margin-bottom: 12px;
  color: #132235;
}

.login-button {
  width: 100%;
  margin-top: 6px;
}

:deep(.el-input__wrapper) {
  min-height: 44px;
  background: rgba(255, 255, 255, 0.9);
}

html.dark .login-card {
  background: rgba(19, 28, 40, 0.82);
  border-color: rgba(255, 255, 255, 0.12);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.36);
}

html.dark .login-eyebrow {
  color: rgba(191, 207, 227, 0.78);
}

html.dark .login-title {
  color: #f8fbff !important;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.28);
}

html.dark :deep(.el-input__wrapper) {
  background: rgba(8, 14, 24, 0.72);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.06) inset;
}

html.dark :deep(.el-input__inner) {
  color: #eef4ff;
}

html.dark :deep(.el-input__prefix-inner) {
  color: rgba(191, 207, 227, 0.72);
}

@media (max-width: 640px) {
  .login-shell {
    padding: 20px;
    background-position: 58% center;
  }

  .login-card {
    width: 100%;
    max-width: 400px;
  }
}
</style>
