<script setup lang="ts">
import { Fold, Expand, SwitchButton, User, Sunny, Moon } from '@element-plus/icons-vue'
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import AppSidebar from '@/layouts/components/AppSidebar.vue'
import TagsView from '@/layouts/components/TagsView.vue'
import { resetDynamicRoutes } from '@/router'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(false)
const isDark = ref(false)

onMounted(() => {
  const theme = localStorage.getItem('jasmine-theme')
  if (theme === 'dark') {
    isDark.value = true
    document.documentElement.classList.add('dark')
  }
})

function toggleDark() {
  isDark.value = !isDark.value
  if (isDark.value) {
    document.documentElement.classList.add('dark')
    localStorage.setItem('jasmine-theme', 'dark')
  } else {
    document.documentElement.classList.remove('dark')
    localStorage.setItem('jasmine-theme', 'light')
  }
}

const pageTitle = computed(() => route.meta.title || 'Jasmine')

async function handleLogout() {
  await authStore.logout()
  resetDynamicRoutes()
  router.replace('/login')
}
</script>

<template>
  <div class="layout-shell">
    <aside class="layout-sidebar" :class="{ collapsed }">
      <div class="brand">
        <span v-show="!collapsed">Jasmine</span>
        <span v-show="collapsed">J</span>
      </div>
      <AppSidebar :collapsed="collapsed" />
    </aside>
    <section class="layout-main">
      <header class="layout-header">
        <el-button text @click="collapsed = !collapsed">
          <el-icon><component :is="collapsed ? Expand : Fold" /></el-icon>
        </el-button>
        <div class="layout-breadcrumb">首页 / {{ pageTitle }}</div>
        <div class="layout-actions">
          <el-button text @click="toggleDark">
            <el-icon><component :is="isDark ? Moon : Sunny" /></el-icon>
          </el-button>
          <el-button text @click="router.push('/profile')">
            <el-icon><User /></el-icon>
            <span>{{ authStore.name || '个人信息' }}</span>
          </el-button>
          <el-button text type="danger" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            <span>退出登录</span>
          </el-button>
        </div>
      </header>
      <TagsView />
      <main class="layout-content">
        <router-view />
      </main>
    </section>
  </div>
</template>

<style scoped>
.layout-shell {
  display: flex;
  min-height: 100vh;
}

.layout-sidebar {
  width: 240px;
  flex-shrink: 0;
  background: var(--jasmine-card-bg);
  border-right: 1px solid var(--jasmine-border-solid);
  color: var(--jasmine-text-main);
  transition: width 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
  z-index: 10;
  overflow: hidden;
}

:global(html.dark) .layout-sidebar {
  background: #16161a;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.layout-sidebar.collapsed {
  width: 64px;
}

.brand {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  font-size: 20px;
  font-weight: 800;
  color: var(--jasmine-text-main);
  border-bottom: 1px solid var(--jasmine-border-solid);
  white-space: nowrap;
  transition: padding 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
}
.layout-sidebar.collapsed .brand {
  padding: 0;
  justify-content: center;
}

:global(html.dark) .brand {
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.layout-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.layout-header {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 16px;
  justify-content: space-between;
  padding: 0 24px;
  background: var(--jasmine-card-bg);
  border-bottom: 1px solid var(--jasmine-border-solid);
  transition: background 0.3s ease;
}

:global(html.dark) .layout-header {
  background: #232324;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.layout-breadcrumb {
  flex: 1;
  color: var(--jasmine-text-muted);
  font-size: 13px;
}

.layout-actions {
  display: flex;
  gap: 12px;
}

.layout-content {
  padding: 16px 24px;
}
</style>
