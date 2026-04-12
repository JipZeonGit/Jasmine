import request from '@/utils/request'

export default {
  getSalesList(searchModel) {
    return request({
      url: '/sales/list',
      method: 'get',
      params: {
        orderNo: searchModel.orderNo,
        startTime: searchModel.dateRange && searchModel.dateRange[0] ? searchModel.dateRange[0] : '',
        endTime: searchModel.dateRange && searchModel.dateRange[1] ? searchModel.dateRange[1] : '',
        pageNo: searchModel.pageNo,
        pageSize: searchModel.pageSize
      }
    })
  },
  getTodaySummary() {
    return request({
      url: '/sales/today-summary',
      method: 'get'
    })
  },
  addSales(sales) {
    return request({
      url: '/sales',
      method: 'post',
      data: sales
    })
  },
  updateSales(sales) {
    return request({
      url: '/sales',
      method: 'put',
      data: sales
    })
  },
  saveSales(sales) {
    if (sales.id === null || sales.id === undefined) {
      return this.addSales(sales)
    }
    return this.updateSales(sales)
  },
  getSalesById(id) {
    return request({
      url: `/sales/${id}`,
      method: 'get'
    })
  },
  deleteSalesById(id) {
    return request({
      url: `/sales/${id}`,
      method: 'delete'
    })
  },
  getAllSalesList() {
    return request({
      url: '/sales/all',
      method: 'get'
    })
  }
}
