import request from '@/utils/request'

export const salesApi = {
  list(params: Record<string, unknown>) {
    return request.get('/sales/list', { params })
  },
  getById(id: number) {
    return request.get(`/sales/${id}`)
  },
  getTodaySummary() {
    return request.get('/sales/today-summary')
  },
  save(payload: Record<string, unknown>) {
    if (payload.id == null) {
      return request.post('/sales', payload)
    }
    return request.put('/sales', payload)
  },
  remove(id: number) {
    return request.delete(`/sales/${id}`)
  },
}
