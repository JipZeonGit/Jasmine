import request from '@/utils/request'

export function getAllMenus() {
  return request.get('/menu')
}
