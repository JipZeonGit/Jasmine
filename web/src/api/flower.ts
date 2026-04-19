import request from '@/utils/request'

export const flowerApi = {
  list(params: { name?: string; pageNo: number; pageSize: number }) {
    return request.get('/flower/list', { params })
  },
  getAll() {
    return request.get('/flower/all')
  },
  getById(id: number) {
    return request.get(`/flower/${id}`)
  },
  save(payload: Record<string, unknown>) {
    if (payload.id == null) {
      return request.post('/flower', payload)
    }
    return request.put('/flower', payload)
  },
  remove(id: number) {
    return request.delete(`/flower/${id}`)
  },
}
