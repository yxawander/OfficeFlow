import request from './request'

export function getFlowHealthApi() {
  return request.get('/flow/health')
}

// 提交申请 (LEAVE, OVERTIME, CORRECTION)
export function createApplyApi(data) {
  return request.post('/flow/applies', data)
}

// 获取申请详情
export function getApplyDetailApi(id) {
  return request.get(`/flow/applies/${id}`)
}

// 搜索我的申请（关键词模糊匹配标题和原因）
export function searchAppliesApi(params) {
  return request.get('/flow/applies/search', { params })
}

// 获取我的申请列表
export function getMyAppliesApi(params) {
  return request.get('/flow/applies/my', { params })
}

// 获取待我审批列表 (主管/管理员)
export function getPendingAppliesApi(params) {
  return request.get('/flow/applies/pending', { params })
}

// 获取我已处理的审批列表
export function getProcessedAppliesApi(params) {
  return request.get('/flow/applies/processed', { params })
}

// 审批同意
export function approveApplyApi(id, data) {
  return request.post(`/flow/admin/applies/${id}/approve`, data)
}

// 审批驳回
export function rejectApplyApi(id, data) {
  return request.post(`/flow/admin/applies/${id}/reject`, data)
}

// 撤销申请
export function cancelApplyApi(id) {
  return request.put(`/flow/applies/${id}/cancel`)
}

// 上传附件
export function uploadFlowAttachmentApi(formData) {
  return request.post('/flow/attachments/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 删除附件
export function deleteFlowAttachmentApi(id) {
  return request.delete(`/flow/attachments/${id}`)
}
