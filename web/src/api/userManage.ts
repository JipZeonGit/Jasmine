import request from '@/utils/request'

export const userManageApi = {
  list(params: Record<string, unknown>) {
    return request.get('/user/list', { params })
  },
  getById(id: number) {
    return request.get(`/user/${id}`)
  },
  save(payload: Record<string, unknown>) {
    if (payload.id == null) {
      return request.post('/user', payload)
    }
    return request.put('/user', payload)
  },
  remove(id: number) {
    return request.delete(`/user/${id}`)
  },
}
