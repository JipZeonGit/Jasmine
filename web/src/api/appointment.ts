import request from '@/utils/request'

export const appointmentApi = {
  list(params: Record<string, unknown>) {
    return request.get('/appointment/list', { params })
  },
  getById(id: number) {
    return request.get(`/appointment/${id}`)
  },
  create(payload: Record<string, unknown>) {
    return request.post('/appointment', payload)
  },
  update(payload: Record<string, unknown>) {
    return request.put('/appointment', payload)
  },
  remove(id: number) {
    return request.delete(`/appointment/${id}`)
  },
}
