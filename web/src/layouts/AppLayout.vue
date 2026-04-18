<script setup lang="ts">
import { Fold, Expand, SwitchButton, User } from '@element-plus/icons-vue'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import AppSidebar from '@/layouts/components/AppSidebar.vue'
import { resetDynamicRoutes } from '@/router'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(false)

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
      <div class="brand">Jasmine</div>
      <AppSidebar :collapsed="collapsed" />
    </aside>
    <section class="layout-main">
      <header class="layout-header">
        <el-button text @click="collapsed = !collapsed">
          <el-icon><component :is="collapsed ? Expand : Fold" /></el-icon>
        </el-button>
        <div class="layout-breadcrumb">首页 / {{ pageTitle }}</div>
        <div class="layout-actions">
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
      <main class="layout-content">
        <router-view />
      </main>
    </section>
  </div>
</template>

<style scoped>
.layout-shell {
  display: grid;
  grid-template-columns: 260px 1fr;
  min-height: 100vh;
}

.layout-sidebar {
  background: linear-gradient(180deg, #1f3653 0%, #1b2d44 100%);
  color: #f9fafb;
  transition: width .2s ease;
}

.layout-sidebar.collapsed {
  width: 84px;
}

.brand {
  padding: 20px 24px;
  font-size: 24px;
  font-weight: 700;
}

.layout-main {
  min-width: 0;
}

.layout-header {
  height: 68px;
  display: flex;
  align-items: center;
  gap: 16px;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.layout-breadcrumb {
  flex: 1;
  color: #64748b;
}

.layout-actions {
  display: flex;
  gap: 12px;
}

.layout-content {
  padding: 24px;
}
</style>
