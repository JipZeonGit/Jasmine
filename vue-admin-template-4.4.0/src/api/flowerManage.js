import request from '@/utils/request'

export default {
    // 分页查询花卉列表
    getFlowerList(searchModel) {
        return request({
            url: '/flower/list',
            method: 'get',
            params: {
                name: searchModel.name,
                pageNo: searchModel.pageNo,
                pageSize: searchModel.pageSize
            }
        });
    },
    // 新增花卉
    addFlower(flower) {
        return request({
            url: '/flower',
            method: 'post',
            data: flower
        });
    },
    // 修改花卉
    updateFlower(flower) {
        return request({
            url: '/flower',
            method: 'put',
            data: flower
        });
    },
    // 保存花卉数据
    saveFlower(flower) {
        if (flower.id == null || flower.id == undefined) {
            return this.addFlower(flower);
        }
        return this.updateFlower(flower);
    },
    // 根据id查询花卉
    getFlowerById(id) {
        return request({
            url: `/flower/${id}`,
            method: 'get'
        });
    },
    // 根据id逻辑删除花卉
    deleteFlowerById(id) {
        return request({
            url: `/flower/${id}`,
            method: 'delete'
        });
    },
    // 获取全部花卉
    getAllFlowerList() {
        return request({
            url: '/flower/all',
            method: 'get'
        });
    }

}