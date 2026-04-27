import axios from 'axios'
import NProgress from 'nprogress'
import { ElMessage } from 'element-plus'

import { getToken, removeToken, setToken } from './auth'
import type { ResultEnvelope } from '@/types'

NProgress.configure({ showSpinner: false })

const service: any = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/prod-api',
  timeout: 10000,
  withCredentials: true,
})

const AUTH_FREE_ENDPOINTS = ['/user/login', '/user/refresh']
const AUTH_ERROR_CODES = [20003, 50008, 50012, 50014, 403]
let refreshPromise: Promise<string> | null = null

function isAuthFreeEndpoint(url: string) {
  return AUTH_FREE_ENDPOINTS.some((endpoint) => url.endsWith(endpoint))
}

function redirectToLogin() {
  removeToken()
  window.location.replace(`${window.location.origin}${window.location.pathname}#/login`)
}

async function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = service
      .post('/user/refresh', {})
      .then((res: ResultEnvelope<{ token: string }>) => {
        setToken(res.data.token)
        return res.data.token
      })
      .finally(() => {
        refreshPromise = null
      })
  }

  return refreshPromise
}

service.interceptors.request.use((config) => {
  NProgress.start()
  const token = getToken()
  const url = config.url || ''
  const shouldAttachToken = token && !isAuthFreeEndpoint(url)
  if (shouldAttachToken) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  async (response: any) => {
    NProgress.done()
    const res = response.data as ResultEnvelope<unknown>
    const originalConfig = response.config || {}
    const url = originalConfig.url || ''
    if (res.code !== 20000) {
      const shouldTryRefresh =
        AUTH_ERROR_CODES.includes(res.code) &&
        !isAuthFreeEndpoint(url) &&
        !originalConfig.__retry

      if (shouldTryRefresh) {
        try {
          originalConfig.__retry = true
          const token = await refreshAccessToken()
          originalConfig.headers = originalConfig.headers || {}
          originalConfig.headers.Authorization = `Bearer ${token}`
          return service(originalConfig)
        } catch (error) {
          redirectToLogin()
          return Promise.reject(error)
        }
      }

      ElMessage.error(res.message || '请求失败')
      if (AUTH_ERROR_CODES.includes(res.code)) {
        redirectToLogin()
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    NProgress.done()
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  },
)

export default service
