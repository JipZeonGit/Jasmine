<script setup lang="ts">
import { computed } from 'vue'

import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const greeting = computed(() => `${authStore.name || '管理员'}，您好！欢迎您使用 Jasmine 智慧花卉管理系统。`)
</script>

<template>
  <el-card class="page-card">
    <div style="font-size: 32px; font-weight: 700; line-height: 1.6;">{{ greeting }}</div>
  </el-card>
</template>
