import request from './request'

export function getFlowHealthApi() {
  return request.get('/api/flow/health')
}

// 提交申请 (LEAVE, OVERTIME, CORRECTION)
export function createApplyApi(data) {
  return request.post('/api/flow/applies', data)
}

// 获取我的申请列表
export function getMyAppliesApi(params) {
  return request.get('/api/flow/applies/my', { params })
}

// 获取待我审批列表 (主管/管理员)
export function getPendingAppliesApi(params) {
  return request.get('/api/flow/applies/pending', { params })
}

// 获取我已处理的审批列表
export function getProcessedAppliesApi(params) {
  return request.get('/api/flow/applies/processed', { params })
}

// 审批同意
export function approveApplyApi(id, data) {
  return request.post(`/api/flow/admin/applies/${id}/approve`, data)
}

// 审批驳回
export function rejectApplyApi(id, data) {
  return request.post(`/api/flow/admin/applies/${id}/reject`, data)
}

// 撤销申请
export function cancelApplyApi(id) {
  return request.put(`/api/flow/applies/${id}/cancel`)
}
