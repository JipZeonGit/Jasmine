import request from '@/utils/request'

export default {
    // 分页查询仓库列表
    getInventoryList(searchModel) {
        return request({
            url: '/inventory/list',
            method: 'get',
            params: {
                name: searchModel.name,
                num: searchModel.num,
                pageNo: searchModel.pageNo,
                pageSize: searchModel.pageSize
            }
        });
    },
    // 新增仓库数据
    addInventory(inventory) {
        return request({
            url: '/inventory',
            method: 'post',
            data: inventory
        });
    },
    // 修改仓库数据
    updateInventory(inventory) {
        return request({
            url: '/inventory',
            method: 'put',
            data: inventory
        });
    },
    // 保存仓库数据
    saveInventory(inventory) {
        if (inventory.id == null || inventory.id == undefined) {
            return this.addInventory(inventory);
        }
        return this.updateInventory(inventory);
    },
    // 根据id查询仓库数据
    getInventoryById(id) {
        return request({
            url: `/inventory/${id}`,
            method: 'get'
        });
    },
    // 根据id逻辑删除仓库数据
    deleteInventoryById(id) {
        return request({
            url: `/inventory/${id}`,
            method: 'delete'
        });
    },
    //获取全部仓库数据
    getAllInventoryList() {
        return request({
            url: '/inventory/all',
            method: 'get'
        });
    }

}