import { createRouter, createWebHashHistory } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { staticRoutes, buildDynamicRoutes } from './routes'

export const router = createRouter({
  history: createWebHashHistory(),
  routes: staticRoutes,
  scrollBehavior: () => ({ top: 0 }),
})

let dynamicRoutesReady = false

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  if (to.meta.public) {
    if (to.path === '/login' && authStore.isLoggedIn) {
      return '/dashboard'
    }
    return true
  }

  if (!authStore.isLoggedIn) {
    return `/login?redirect=${encodeURIComponent(to.fullPath)}`
  }

  if (!dynamicRoutesReady) {
    const userInfo = authStore.userInfo || await authStore.loadUserInfo()
    const dynamicRoutes = buildDynamicRoutes(userInfo.menuList || [])
    dynamicRoutes.forEach((route) => router.addRoute(route))
    dynamicRoutesReady = true
    return { path: to.fullPath, replace: true }
  }

  return true
})

export function resetDynamicRoutes() {
  dynamicRoutesReady = false
  router.getRoutes().forEach((route) => {
    if (route.name && !['Login', 'Root', 'Dashboard', 'Profile', 'NotFound'].includes(String(route.name))) {
      router.removeRoute(route.name)
    }
  })
}
