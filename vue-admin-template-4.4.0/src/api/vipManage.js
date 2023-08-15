import request from '@/utils/request'

export default {
    // 分页查询会员列表
    getVipList(searchModel) {
        return request({
            url: '/vip/list',
            method: 'get',
            params: {
                name: searchModel.name,
                vid: searchModel.vid,
                phone: searchModel.phone,
                pageNo: searchModel.pageNo,
                pageSize: searchModel.pageSize
            }
        });
    },
    // 新增会员
    addVip(vip) {
        return request({
            url: '/vip',
            method: 'post',
            data: vip
        });
    },
    // 修改会员
    updateVip(vip) {
        return request({
            url: '/vip',
            method: 'put',
            data: vip
        });
    },
    // 保存会员数据
    saveVip(vip) {
        if (vip.id == null || vip.id == undefined) {
            return this.addVip(vip);
        }
        return this.updateVip(vip);
    },
    // 根据id查询会员
    getVipById(id) {
        return request({
            url: `/vip/${id}`,
            method: 'get'
        });
    },
    // 根据id逻辑删除会员
    deleteVipById(id) {
        return request({
            url: `/vip/${id}`,
            method: 'delete'
        });
    },
    // 获取全部会员
    getAllVipList() {
        return request({
            url: '/vip/all',
            method: 'get'
        });
    }

}