<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const formRef = ref()
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const rules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '长度不能少于 6 位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请再次输入新密码', trigger: 'blur' }, {
    validator: (_: unknown, value: string, callback: (error?: Error) => void) => {
      if (value !== form.newPassword) {
        callback(new Error('两次输入的新密码不一致'))
        return
      }
      callback()
    }, trigger: 'blur',
  }],
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.changePassword({ username: authStore.name, oldPassword: form.oldPassword, newPassword: form.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    await authStore.logout()
    window.location.hash = '#/login'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-card class="page-card">
    <template #header>个人信息</template>
    <p><strong>用户名：</strong>{{ authStore.name }}</p>
    <p><strong>手机号：</strong>{{ authStore.phone || '暂无手机号' }}</p>
    <p><strong>电子邮箱：</strong>{{ authStore.email || '暂无邮箱' }}</p>
    <p>
      <strong>用户角色：</strong>
      <el-tag v-for="role in authStore.roles" :key="role" style="margin-right: 8px;">{{ role }}</el-tag>
    </p>
  </el-card>
  <el-card class="page-card" style="max-width: 640px;">
    <template #header>修改密码</template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="旧密码" prop="oldPassword"><el-input v-model="form.oldPassword" type="password" show-password /></el-form-item>
      <el-form-item label="新密码" prop="newPassword"><el-input v-model="form.newPassword" type="password" show-password /></el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="form.confirmPassword" type="password" show-password /></el-form-item>
      <el-form-item><el-button type="primary" :loading="loading" @click="submit">提交</el-button></el-form-item>
    </el-form>
  </el-card>
</template>
