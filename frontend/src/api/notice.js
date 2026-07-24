import request from './request'

// ===================== 用户侧接口 =====================
// 公告列表（分页，默认仅已发布）
export function getNoticeListApi(params) {
  return request.get('/notice/notices', { params })
}

// 公告搜索（ES全文检索，自动降级MySQL）
export function searchNoticeApi(params) {
  return request.get('/notice/notices/search', { params })
}

// 公告详情
export function getNoticeDetailApi(id) {
  return request.get(`/notice/notices/${id}`)
}

// 标记阅读状态（readStatus: 0 未读 / 1 已读）
export function markNoticeReadApi(id, readStatus = 1) {
  return request.post(`/notice/notices/${id}/read-status`, { readStatus })
}

// 批量标记已读
export function batchReadNoticeApi(ids) {
  return request.post('/notice/notices/batch-read', { noticeIds: ids })
}

// 未读统计
export function getUnreadCountApi(params) {
  return request.get('/notice/notices/unread-count', { params })
}

// ===================== 管理侧接口 =====================
// 新建公告（返回公告 id）
export function createNoticeApi(data) {
  return request.post('/notice/admin/notices', data)
}

// 编辑公告
export function updateNoticeApi(id, data) {
  return request.put(`/notice/admin/notices/${id}`, data)
}

// 发布
export function publishNoticeApi(id) {
  return request.post(`/notice/admin/notices/${id}/publish`)
}

// 下线
export function offlineNoticeApi(id) {
  return request.post(`/notice/admin/notices/${id}/offline`)
}

// 删除
export function deleteNoticeApi(id) {
  return request.delete(`/notice/admin/notices/${id}`)
}

// 管理列表（分页，含状态/阅读率）
export function getAdminNoticeListApi(params) {
  return request.get('/notice/admin/notices', { params })
}

// 管理侧公告搜索（ES全文检索，自动降级MySQL）
export function searchAdminNoticeApi(params) {
  return request.get('/notice/admin/notices/search', { params })
}

// 阅读统计
export function getNoticeReadDetailApi(id) {
  return request.get(`/notice/admin/notices/${id}/read-details`)
}

// 上传附件（multipart），返回 AttachmentVO
export function uploadNoticeAttachmentApi(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/notice/admin/attachments/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 删除附件
export function deleteNoticeAttachmentApi(id) {
  return request.delete(`/notice/admin/attachments/${id}`)
}
