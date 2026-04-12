import request from '@/utils/request'

export default {
  getFlowerList(searchModel) {
    return request({
      url: '/flower/list',
      method: 'get',
      params: {
        name: searchModel.name,
        pageNo: searchModel.pageNo,
        pageSize: searchModel.pageSize
      }
    })
  },
  addFlower(flower) {
    return request({
      url: '/flower',
      method: 'post',
      data: flower
    })
  },
  updateFlower(flower) {
    return request({
      url: '/flower',
      method: 'put',
      data: flower
    })
  },
  saveFlower(flower) {
    if (flower.id === null || flower.id === undefined) {
      return this.addFlower(flower)
    }
    return this.updateFlower(flower)
  },
  getFlowerById(id) {
    return request({
      url: `/flower/${id}`,
      method: 'get'
    })
  },
  deleteFlowerById(id) {
    return request({
      url: `/flower/${id}`,
      method: 'delete'
    })
  },
  getAllFlowerList() {
    return request({
      url: '/flower/all',
      method: 'get'
    })
  }
}
