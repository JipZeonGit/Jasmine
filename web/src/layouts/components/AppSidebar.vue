<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { House, User, UserFilled, Goods, ShoppingCart, Box, Calendar } from '@element-plus/icons-vue'

import { useAuthStore } from '@/stores/auth'
import type { MenuItem } from '@/types'

defineProps<{ collapsed: boolean }>()

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const iconMap = {
  dashboard: House,
  user: User,
  role: UserFilled,
  flower: Goods,
  sales: ShoppingCart,
  inventory: Box,
  appointment: Calendar,
}

const menuTree = computed<MenuItem[]>(() => authStore.menuRoutes || [])

function resolveIcon(icon?: string) {
  const name = icon?.replace(/^el-icon-/, '')?.toLowerCase?.()
  return iconMap[name as keyof typeof iconMap] || Goods
}

function go(path: string) {
  router.push(path)
}

function resolvePath(parentPath: string, path: string) {
  if (path.startsWith('/')) {
    return path
  }
  const normalizedParent = parentPath.endsWith('/') ? parentPath.slice(0, -1) : parentPath
  return `${normalizedParent}/${path}`
}
</script>

<template>
  <el-menu
    :default-active="route.path"
    background-color="transparent"
    text-color="#dbeafe"
    active-text-color="#ffffff"
    :collapse="collapsed"
    router
    class="sidebar-menu"
  >
    <el-menu-item index="/dashboard" @click="go('/dashboard')">
      <el-icon><House /></el-icon>
      <span>首页</span>
    </el-menu-item>

    <template v-for="item in menuTree" :key="item.menuId">
      <el-sub-menu v-if="item.children?.length" :index="item.path">
        <template #title>
          <el-icon><component :is="resolveIcon(item.icon)" /></el-icon>
          <span>{{ item.title }}</span>
        </template>
        <el-menu-item v-for="child in item.children" :key="child.menuId" :index="resolvePath(item.path, child.path)" @click="go(resolvePath(item.path, child.path))">
          <el-icon><component :is="resolveIcon(child.icon)" /></el-icon>
          <span>{{ child.title }}</span>
        </el-menu-item>
      </el-sub-menu>

      <el-menu-item v-else :index="item.path" @click="go(item.path)">
        <el-icon><component :is="resolveIcon(item.icon)" /></el-icon>
        <span>{{ item.title }}</span>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<style scoped>
.sidebar-menu {
  border-right: none;
}
</style>
