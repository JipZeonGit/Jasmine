import request from '@/utils/request'

export const vipApi = {
  list(params: { vid?: string; name?: string; phone?: string; pageNo: number; pageSize: number }) {
    return request.get('/vip/list', { params })
  },
  getAll() {
    return request.get('/vip/all')
  },
  getById(id: number) {
    return request.get(`/vip/${id}`)
  },
  save(payload: Record<string, unknown>) {
    if (payload.id == null) {
      return request.post('/vip', payload)
    }
    return request.put('/vip', payload)
  },
  remove(id: number) {
    return request.delete(`/vip/${id}`)
  },
}
