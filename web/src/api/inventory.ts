import request from '@/utils/request'

export const inventoryApi = {
  list(params: Record<string, unknown>) {
    return request.get('/inventory/list', { params })
  },
  getById(id: number) {
    return request.get(`/inventory/${id}`)
  },
  save(payload: Record<string, unknown>) {
    if (payload.id == null) {
      return request.post('/inventory', payload)
    }
    return request.put('/inventory', payload)
  },
  remove(id: number) {
    return request.delete(`/inventory/${id}`)
  },
}
