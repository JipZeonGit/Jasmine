import request from '@/utils/request'
import { buildCreateIdempotencyKey } from '@/utils/idempotency'

export default {
  getInventoryList(searchModel) {
    return request({
      url: '/inventory/list',
      method: 'get',
      params: {
        name: searchModel.name,
        num: searchModel.num,
        bizType: searchModel.bizType,
        startTime: searchModel.dateRange && searchModel.dateRange[0] ? searchModel.dateRange[0] : '',
        endTime: searchModel.dateRange && searchModel.dateRange[1] ? searchModel.dateRange[1] : '',
        pageNo: searchModel.pageNo,
        pageSize: searchModel.pageSize
      }
    })
  },
  addInventory(inventory) {
    return request({
      url: '/inventory',
      method: 'post',
      headers: {
        'X-Idempotency-Key': buildCreateIdempotencyKey('inventory:create', inventory)
      },
      data: inventory
    })
  },
  updateInventory(inventory) {
    return request({
      url: '/inventory',
      method: 'put',
      data: inventory
    })
  },
  saveInventory(inventory) {
    if (inventory.id === null || inventory.id === undefined) {
      return this.addInventory(inventory)
    }
    return this.updateInventory(inventory)
  },
  getInventoryById(id) {
    return request({
      url: `/inventory/${id}`,
      method: 'get'
    })
  },
  deleteInventoryById(id) {
    return request({
      url: `/inventory/${id}`,
      method: 'delete'
    })
  },
  getAllInventoryList() {
    return request({
      url: '/inventory/all',
      method: 'get'
    })
  }
}
