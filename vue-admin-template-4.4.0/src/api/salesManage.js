import request from '@/utils/request'

export default {
    // 分页查询销售列表
    getSalesList(searchModel) {
        return request({
            url: '/sales/list',
            method: 'get',
            params: {
                date: searchModel.date,
                pageNo: searchModel.pageNo,
                pageSize: searchModel.pageSize
            }
        });
    },
    // 新增销售数据
    addSales(sales) {
        return request({
            url: '/sales',
            method: 'post',
            data: sales
        });
    },
    // 修改销售数据
    updateSales(sales) {
        return request({
            url: '/sales',
            method: 'put',
            data: sales
        });
    },
    // 保存销售数据
    saveSales(sales) {
        if (sales.id == null || sales.id == undefined) {
            return this.addSales(sales);
        }
        return this.updateSales(sales);
    },
    // 根据id查询销售数据
    getSalesById(id) {
        return request({
            url: `/sales/${id}`,
            method: 'get'
        });
    },
    // 根据id逻辑删除销售数据
    deleteSalesById(id) {
        return request({
            url: `/sales/${id}`,
            method: 'delete'
        });
    },
    // 获取全部销售数据
    getAllSalesList() {
        return request({
            url: '/sales/all',
            method: 'get'
        });
    }

}