import request from '@/utils/request'

export default {
    // 分页查询预约列表
    getAppointmentList(searchModel) {
        return request({
            url: '/appointment/list',
            method: 'get',
            params: {
                name: searchModel.name,
                phone: searchModel.phone,
                date: searchModel.date,
                pageNo: searchModel.pageNo,
                pageSize: searchModel.pageSize
            }
        });
    },
    // 新增预约
    addAppointment(vid, phone, date, content) {
        return request({
            url: '/appointment',
            method: 'post',
            params: {
                vid: vid,
                phone: phone,
                date: date,
                content: content
            }
        });
    },
    // 修改预约
    updateAppointment(appointment) {
        return request({
            url: '/appointment',
            method: 'put',
            data: appointment
        });
    },
    // 根据id查询预约
    getAppointmentById(id) {
        return request({
            url: `/appointment/${id}`,
            method: 'get'
        });
    },
    // 根据id逻辑删除预约
    deleteAppointmentById(id) {
        return request({
            url: `/appointment/${id}`,
            method: 'delete'
        });
    },
    // 获取全部预约
    getAllAppointmentList() {
        return request({
            url: '/appointment/all',
            method: 'get'
        });
    }

}