import { defineStore } from 'pinia'

import { changePassword, getUserInfo, login, logout } from '@/api/auth'
import type { MenuItem, UserInfoVO } from '@/types'
import { getToken, removeToken, setToken } from '@/utils/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    userInfo: null as UserInfoVO | null,
    menuRoutes: [] as MenuItem[],
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    name: (state) => state.userInfo?.name || '',
    phone: (state) => state.userInfo?.phone || '',
    email: (state) => state.userInfo?.email || '',
    roles: (state) => state.userInfo?.roles || [],
  },
  actions: {
    async login(payload: { username: string; password: string }) {
      const response = await login(payload)
      this.token = response.data.token
      setToken(response.data.token)
    },
    async loadUserInfo() {
      const response = await getUserInfo()
      this.userInfo = response.data
      this.menuRoutes = response.data.menuList || []
      return response.data
    },
    async logout() {
      if (this.token) {
        try {
          await logout()
        } catch (e) {
          // Ignore backend errors because we should force logout locally anyway
        }
      }
      this.resetSession()
    },
    async changePassword(payload: { username: string; oldPassword: string; newPassword: string }) {
      return changePassword(payload)
    },
    resetSession() {
      this.token = ''
      this.userInfo = null
      this.menuRoutes = []
      removeToken()
    },
  },
})
