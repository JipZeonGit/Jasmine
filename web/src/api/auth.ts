import request from '@/utils/request'
import type { LoginPayload } from '@/types'

export function login(payload: LoginPayload) {
  return request.post('/user/login', payload)
}

export function refreshToken() {
  return request.post('/user/refresh', {})
}

export function getUserInfo() {
  return request.get('/user/info')
}

export function logout() {
  return request.post('/user/logout')
}

export function changePassword(payload: { oldPassword: string; newPassword: string }) {
  return request.put('/user/changePassword', payload)
}
