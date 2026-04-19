import request from '@/utils/request'

export const roleApi = {
  list(params: Record<string, unknown>) {
    return request.get('/role/list', { params })
  },
  getAll() {
    return request.get('/role/all')
  },
  getById(id: number) {
    return request.get(`/role/${id}`)
  },
  save(payload: Record<string, unknown>) {
    if (payload.roleId == null) {
      return request.post('/role', payload)
    }
    return request.put('/role', payload)
  },
  remove(id: number) {
    return request.delete(`/role/${id}`)
  },
}
