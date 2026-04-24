<script setup lang="ts">
import { Fold, Expand, SwitchButton, User, Sunny, Moon, Bell } from '@element-plus/icons-vue'
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { inventoryApi, inventoryAlertApi } from '@/api/inventory'
import { siteMessageApi, type SiteMessage } from '@/api/siteMessage'
import AppSidebar from '@/layouts/components/AppSidebar.vue'
import TagsView from '@/layouts/components/TagsView.vue'
import { resetDynamicRoutes } from '@/router'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(false)
const isDark = ref(false)

const lowStockCount = ref(0)
const unreadSiteMessageCount = ref(0)
const totalMessageCount = computed(() => lowStockCount.value + unreadSiteMessageCount.value)

let pollTimer: ReturnType<typeof setInterval>

const activeTab = ref('siteMessage')

const alertList = ref<any[]>([])
const siteMessageList = ref<SiteMessage[]>([])
const loadingMessages = ref(false)

async function fetchMessageCounts() {
  try {
    const [stockRes, msgRes] = await Promise.all([
      inventoryApi.getLowStockCount(),
      siteMessageApi.getUnreadCount()
    ])
    lowStockCount.value = Number(stockRes.data) || 0
    unreadSiteMessageCount.value = Number(msgRes.data) || 0
  } catch (error) {
    console.error('获取消息数量失败', error)
  }
}

async function fetchAllMessages() {
  if (totalMessageCount.value === 0) {
    alertList.value = []
    siteMessageList.value = []
    return
  }
  loadingMessages.value = true
  try {
    const [stockRes, msgRes] = await Promise.all([
      inventoryAlertApi.list({ alertStatus: 'LOW_STOCK', pageNo: 1, pageSize: 10 }),
      siteMessageApi.list({ pageNo: 1, pageSize: 10 })
    ])
    
    alertList.value = stockRes.data?.rows || []
    siteMessageList.value = msgRes.data?.rows || []
    
    // 同步更新本地角标数量，修复跨页面状态变更后出现的角标与内容不一致（幽灵数字）问题
    if (stockRes.data?.total !== undefined) {
      lowStockCount.value = Number(stockRes.data.total)
    }
    if (msgRes.data?.total !== undefined) {
      unreadSiteMessageCount.value = Number(msgRes.data.total)
    }
    
    // 如果当前选中的 tab 没有消息，且另一个 tab 有消息，则自动切换
    if (activeTab.value === 'siteMessage' && unreadSiteMessageCount.value === 0 && lowStockCount.value > 0) {
      activeTab.value = 'inventory'
    } else if (activeTab.value === 'inventory' && lowStockCount.value === 0 && unreadSiteMessageCount.value > 0) {
      activeTab.value = 'siteMessage'
    }
  } catch (error) {
    console.error('获取消息列表失败', error)
  } finally {
    loadingMessages.value = false
  }
}

async function handleMarkAsRead(id: number) {
  try {
    await siteMessageApi.markAsRead(id)
    await fetchMessageCounts()
    await fetchAllMessages()
  } catch (error) {
    console.error('标记已读失败', error)
  }
}

async function handleMarkAllAsRead() {
  try {
    await siteMessageApi.markAllAsRead()
    await fetchMessageCounts()
    await fetchAllMessages()
  } catch (error) {
    console.error('全部标记已读失败', error)
  }
}

onMounted(() => {
  fetchMessageCounts()
  pollTimer = setInterval(fetchMessageCounts, 60000)

  const theme = localStorage.getItem('jasmine-theme')
  if (theme === 'dark') {
    isDark.value = true
    document.documentElement.classList.add('dark')
  }
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
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
          <el-popover
            placement="bottom"
            :width="320"
            trigger="hover"
            @show="fetchAllMessages"
            :disabled="totalMessageCount === 0"
            popper-style="border-radius: 12px; padding: 0;"
          >
            <template #reference>
              <el-badge :value="totalMessageCount" :hidden="totalMessageCount === 0" class="alert-badge">
                <el-button text>
                  <el-icon><Bell /></el-icon>
                </el-button>
              </el-badge>
            </template>
            <div v-loading="loadingMessages" class="notification-center">
              <el-tabs v-model="activeTab" class="notification-tabs">
                <el-tab-pane :name="'siteMessage'" v-if="unreadSiteMessageCount > 0 || activeTab === 'siteMessage'">
                  <template #label>
                    <span>待办提醒 <el-badge :value="unreadSiteMessageCount" :hidden="unreadSiteMessageCount === 0" class="tab-badge" /></span>
                  </template>
                  <div v-if="siteMessageList.length === 0" class="empty-alert">暂无待办提醒</div>
                  <el-scrollbar v-else max-height="300px">
                    <div v-for="item in siteMessageList" :key="item.id" class="alert-item">
                      <div class="alert-header">
                        <span class="message-title">{{ item.title }}</span>
                        <el-button size="small" type="primary" link @click="handleMarkAsRead(item.id)">已知悉</el-button>
                      </div>
                      <div class="alert-body message-content" :title="item.content">
                        {{ item.content }}
                      </div>
                    </div>
                  </el-scrollbar>
                  <div class="alert-footer" v-if="siteMessageList.length > 0">
                    <el-button type="primary" link @click="handleMarkAllAsRead">全部已知悉</el-button>
                  </div>
                </el-tab-pane>

                <el-tab-pane :name="'inventory'" v-if="lowStockCount > 0 || activeTab === 'inventory'">
                  <template #label>
                    <span>库存预警 <el-badge :value="lowStockCount" :hidden="lowStockCount === 0" class="tab-badge" type="danger" /></span>
                  </template>
                  <div v-if="alertList.length === 0" class="empty-alert">暂无低库存预警</div>
                  <el-scrollbar v-else max-height="300px">
                    <div v-for="item in alertList" :key="item.id" class="alert-item">
                      <div class="alert-header">
                        <span class="flower-name">{{ item.flowerNameSnapshot }}</span>
                        <span class="stock-status">库存告急</span>
                      </div>
                      <div class="alert-body">
                        安全库存: {{ item.safeStock }} &nbsp;|&nbsp; 
                        <span style="color: #f56c6c;">当前: {{ item.currentStock }}</span>
                      </div>
                    </div>
                  </el-scrollbar>
                  <div class="alert-footer">
                    <el-button type="primary" link @click="router.push('/custom/inventoryManage')">前往库存管理</el-button>
                  </div>
                </el-tab-pane>
              </el-tabs>
            </div>
          </el-popover>
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
  align-items: center;
}

.alert-badge {
  line-height: initial;
  display: flex;
}

.layout-content {
  padding: 16px 24px;
}

.notification-center {
  padding: 0;
}

.notification-tabs {
  --el-tabs-header-height: 48px;
}

.notification-tabs :deep(.el-tabs__nav-wrap) {
  padding: 0 16px;
  margin-bottom: 0;
}

.notification-tabs :deep(.el-tabs__item) {
  font-weight: 600;
}

.tab-badge {
  margin-left: 4px;
}
.tab-badge :deep(.el-badge__content) {
  transform: translateY(-50%) scale(0.9);
}

.empty-alert {
  text-align: center;
  color: var(--jasmine-text-muted);
  padding: 32px 0;
  font-size: 13px;
}

.alert-item {
  padding: 12px 16px;
  border-bottom: 1px solid var(--jasmine-border-light, #ebeef5);
  transition: background-color 0.2s ease;
}
.alert-item:hover {
  background-color: var(--jasmine-bg-subtle, #f5f7fa);
}
.alert-item:last-child {
  border-bottom: none;
}

.alert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.flower-name, .message-title {
  font-weight: 600;
  color: var(--jasmine-text-main);
  font-size: 14px;
}

.stock-status {
  font-size: 12px;
  color: #f56c6c;
  background: rgba(245, 108, 108, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
}

.alert-body {
  font-size: 13px;
  color: var(--jasmine-text-regular);
}

.message-content {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.5;
  color: var(--jasmine-text-muted);
}

.alert-footer {
  text-align: center;
  border-top: 1px solid var(--jasmine-border-light, #ebeef5);
  padding: 8px 0;
  background-color: var(--jasmine-card-bg);
  position: sticky;
  bottom: 0;
  z-index: 1;
}

:global(html.dark) .alert-item:hover {
  background-color: #2b2b2c;
}
</style>
