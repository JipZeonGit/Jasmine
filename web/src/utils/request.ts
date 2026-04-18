import axios from 'axios'
import NProgress from 'nprogress'
import { ElMessage } from 'element-plus'

import { getToken, removeToken } from './auth'
import type { ResultEnvelope } from '@/types'

NProgress.configure({ showSpinner: false })

const service: any = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/prod-api',
  timeout: 10000,
})

const AUTH_FREE_ENDPOINTS = ['/user/login']

service.interceptors.request.use((config) => {
  NProgress.start()
  const token = getToken()
  const url = config.url || ''
  const shouldAttachToken = token && !AUTH_FREE_ENDPOINTS.some((endpoint) => url.endsWith(endpoint))
  if (shouldAttachToken) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response: any) => {
    NProgress.done()
    const res = response.data as ResultEnvelope<unknown>
    if (res.code !== 20000) {
      ElMessage.error(res.message || '请求失败')
      if ([20003, 50008, 50012, 50014, 403].includes(res.code)) {
        removeToken()
        window.location.hash = '#/login'
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
