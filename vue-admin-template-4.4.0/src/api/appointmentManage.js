import request from '@/utils/request'

export default {
  getAppointmentList(searchModel) {
    return request({
      url: '/appointment/list',
      method: 'get',
      params: {
        name: searchModel.name,
        phone: searchModel.phone,
        startTime: searchModel.dateRange && searchModel.dateRange[0] ? searchModel.dateRange[0] : '',
        endTime: searchModel.dateRange && searchModel.dateRange[1] ? searchModel.dateRange[1] : '',
        pageNo: searchModel.pageNo,
        pageSize: searchModel.pageSize
      }
    })
  },
  addAppointment(appointment) {
    return request({
      url: '/appointment',
      method: 'post',
      data: appointment
    })
  },
  updateAppointment(appointment) {
    return request({
      url: '/appointment',
      method: 'put',
      data: appointment
    })
  },
  getAppointmentById(id) {
    return request({
      url: `/appointment/${id}`,
      method: 'get'
    })
  },
  deleteAppointmentById(id) {
    return request({
      url: `/appointment/${id}`,
      method: 'delete'
    })
  },
  getAllAppointmentList() {
    return request({
      url: '/appointment/all',
      method: 'get'
    })
  }
}
