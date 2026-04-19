<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { House, User, UserFilled, Goods, ShoppingCart, Box, Calendar, Menu, Star, Setting } from '@element-plus/icons-vue'

import { useAuthStore } from '@/stores/auth'
import type { MenuItem } from '@/types'

const props = defineProps<{ collapsed: boolean }>()

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const menuRef = ref()
const openedPaths = ref<string[]>([])

const menuTree = computed<MenuItem[]>(() => authStore.menuRoutes || [])

function resolveIcon(item: MenuItem) {
  const p = item.path || ''
  if (p.includes('user')) return User
  if (p.includes('role')) return UserFilled
  if (p.includes('flower')) return Goods
  if (p.includes('sales')) return ShoppingCart
  if (p.includes('inventory')) return Box
  if (p.includes('appointment')) return Calendar
  if (p.includes('vip')) return Star
  if (p.includes('system')) return Setting
  return Menu
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

function handleOpen(index: string) {
  if (!openedPaths.value.includes(index)) {
    openedPaths.value.push(index)
  }
}

function handleClose(index: string) {
  // If closing happens while collapsed, do not erase memory
  if (props.collapsed) return
  const i = openedPaths.value.indexOf(index)
  if (i > -1) {
    openedPaths.value.splice(i, 1)
  }
}

watch(() => props.collapsed, (val) => {
  if (!val) {
    // Restore opened submenus quickly after expanding
    setTimeout(() => {
      openedPaths.value.forEach(p => {
        menuRef.value?.open?.(p)
      })
    }, 10)
  }
})
</script>

<template>
  <el-menu
    ref="menuRef"
    :default-active="route.path"
    background-color="transparent"
    text-color="var(--jasmine-text-main)"
    active-text-color="var(--el-color-primary)"
    :collapse="collapsed"
    :collapse-transition="false"
    router
    @open="handleOpen"
    @close="handleClose"
    class="sidebar-menu"
  >
    <el-menu-item index="/dashboard" @click="go('/dashboard')">
      <el-icon><House /></el-icon>
      <span>首页</span>
    </el-menu-item>

    <template v-for="item in menuTree" :key="item.menuId">
      <el-sub-menu v-if="item.children?.length" :index="item.path">
        <template #title>
          <el-icon><component :is="resolveIcon(item)" /></el-icon>
          <span>{{ item.title }}</span>
        </template>
        <el-menu-item v-for="child in item.children" :key="child.menuId" :index="resolvePath(item.path, child.path)" @click="go(resolvePath(item.path, child.path))">
          <el-icon><component :is="resolveIcon(child)" /></el-icon>
          <span>{{ child.title }}</span>
        </el-menu-item>
      </el-sub-menu>

      <el-menu-item v-else :index="item.path" @click="go(item.path)">
        <el-icon><component :is="resolveIcon(item)" /></el-icon>
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
