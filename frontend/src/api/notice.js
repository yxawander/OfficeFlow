import request from './request'

// ===================== 用户侧接口 =====================
// 公告列表（分页，默认仅已发布）
export function getNoticeListApi(params) {
  return request.get('/api/notice/notices', { params })
}

// 公告详情
export function getNoticeDetailApi(id) {
  return request.get(`/api/notice/notices/${id}`)
}

// 标记阅读状态（readStatus: 0 未读 / 1 已读）
export function markNoticeReadApi(id, readStatus = 1) {
  return request.post(`/api/notice/notices/${id}/read-status`, { readStatus })
}

// 批量标记已读
export function batchReadNoticeApi(ids) {
  return request.post('/api/notice/notices/batch-read', { noticeIds: ids })
}

// 未读统计
export function getUnreadCountApi(params) {
  return request.get('/api/notice/notices/unread-count', { params })
}

// ===================== 管理侧接口 =====================
// 新建公告（返回公告 id）
export function createNoticeApi(data) {
  return request.post('/api/notice/admin/notices', data)
}

// 编辑公告
export function updateNoticeApi(id, data) {
  return request.put(`/api/notice/admin/notices/${id}`, data)
}

// 发布
export function publishNoticeApi(id) {
  return request.post(`/api/notice/admin/notices/${id}/publish`)
}

// 下线
export function offlineNoticeApi(id) {
  return request.post(`/api/notice/admin/notices/${id}/offline`)
}

// 删除
export function deleteNoticeApi(id) {
  return request.delete(`/api/notice/admin/notices/${id}`)
}

// 管理列表（分页，含状态/阅读率）
export function getAdminNoticeListApi(params) {
  return request.get('/api/notice/admin/notices', { params })
}

// 阅读统计
export function getNoticeReadDetailApi(id) {
  return request.get(`/api/notice/admin/notices/${id}/read-details`)
}

// 上传附件（multipart），返回 AttachmentVO
export function uploadNoticeAttachmentApi(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/notice/admin/attachments/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 删除附件
export function deleteNoticeAttachmentApi(id) {
  return request.delete(`/api/notice/admin/attachments/${id}`)
}
