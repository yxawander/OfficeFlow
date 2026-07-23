import request from './request'

export function loginApi(data) {
  return request.post('/api/user/login', data)
}

export function logoutApi() {
  return request.post('/api/user/logout')
}

export function getProfileApi() {
  return request.get('/api/user/profile')
}

export function updateProfileApi(data) {
  return request.put('/api/user/profile', data)
}

export function changePasswordApi(data) {
  return request.put('/api/user/password', data)
}

export function getCurrentMenusApi() {
  return request.get('/api/user/menus/current')
}

export function getUserHealthApi() {
  return request.get('/api/user/health')
}

export function getUserPageApi(params) {
  return request.get('/api/user/users', { params })
}

export function getUserOptionsApi() {
  return request.get('/api/user/users/options')
}

export function createUserApi(data) {
  return request.post('/api/user/users', data)
}

export function updateUserApi(id, data) {
  return request.put(`/api/user/users/${id}`, data)
}

export function deleteUserApi(id) {
  return request.delete(`/api/user/users/${id}`)
}

export function updateUserStatusApi(id, status) {
  return request.put(`/api/user/users/${id}/status`, { status })
}

export function resetUserPasswordApi(id, password) {
  return request.put(`/api/user/users/${id}/password`, { password })
}

export function assignUserRolesApi(id, ids) {
  return request.put(`/api/user/users/${id}/roles`, { ids })
}

export function getDeptTreeApi() {
  return request.get('/api/user/depts/tree')
}

export function createDeptApi(data) {
  return request.post('/api/user/depts', data)
}

export function updateDeptApi(id, data) {
  return request.put(`/api/user/depts/${id}`, data)
}

export function deleteDeptApi(id) {
  return request.delete(`/api/user/depts/${id}`)
}

export function getPostListApi() {
  return request.get('/api/user/posts')
}

export function createPostApi(data) {
  return request.post('/api/user/posts', data)
}

export function updatePostApi(id, data) {
  return request.put(`/api/user/posts/${id}`, data)
}

export function deletePostApi(id) {
  return request.delete(`/api/user/posts/${id}`)
}

export function getRoleListApi() {
  return request.get('/api/user/roles')
}

export function createRoleApi(data) {
  return request.post('/api/user/roles', data)
}

export function updateRoleApi(id, data) {
  return request.put(`/api/user/roles/${id}`, data)
}

export function deleteRoleApi(id) {
  return request.delete(`/api/user/roles/${id}`)
}

export function assignRoleMenusApi(id, ids) {
  return request.put(`/api/user/roles/${id}/menus`, { ids })
}

export function assignRoleApiPermissionsApi(id, ids) {
  return request.put(`/api/user/roles/${id}/api-permissions`, { ids })
}

export function getMenuListApi() {
  return request.get('/api/user/menus')
}

export function getApiPermissionListApi() {
  return request.get('/api/user/api-permissions')
}

export function createApiPermissionApi(data) {
  return request.post('/api/user/api-permissions', data)
}

export function updateApiPermissionApi(id, data) {
  return request.put(`/api/user/api-permissions/${id}`, data)
}

export function deleteApiPermissionApi(id) {
  return request.delete(`/api/user/api-permissions/${id}`)
}

export function getLoginLogsApi() {
  return request.get('/api/user/logs/login')
}

export function getOperationLogsApi() {
  return request.get('/api/user/logs/operations')
}
