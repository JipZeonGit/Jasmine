import request from '@/utils/request'

export default{
  // 分页查询角色列表
  getAllMenu(){
    return request({
      url: '/menu',
      method: 'get',
    });
  },
}