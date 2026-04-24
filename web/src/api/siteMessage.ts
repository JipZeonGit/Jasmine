import request from '@/utils/request'

export interface SiteMessage {
  id: number
  bizType: string
  bizId: string
  title: string
  content: string
  isRead: number
  createdAt: string
}

export const siteMessageApi = {
  // 获取未读数量
  getUnreadCount() {
    return request.get('/site-message/unread-count')
  },

  // 分页获取站内信
  list(params: { pageNo?: number; pageSize?: number }) {
    return request.get('/site-message/list', { params })
  },

  // 标记单条已读
  markAsRead(id: number) {
    return request.put(`/site-message/read/${id}`)
  },

  // 标记全部已读
  markAllAsRead() {
    return request.put('/site-message/read-all')
  }
}
