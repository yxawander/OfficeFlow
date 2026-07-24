import request from './request'

export function loginApi(data) {
  return request.post('/user/login', data)
}

export function logoutApi() {
  return request.post('/user/logout')
}

export function getProfileApi() {
  return request.get('/user/profile')
}

export function updateProfileApi(data) {
  return request.put('/user/profile', data)
}

export function changePasswordApi(data) {
  return request.put('/user/password', data)
}

export function getCurrentMenusApi() {
  return request.get('/user/menus/current')
}

export function getUserHealthApi() {
  return request.get('/user/health')
}

export function getUserPageApi(params) {
  return request.get('/user/users', { params })
}

export function getUserOptionsApi() {
  return request.get('/user/users/options')
}

export function createUserApi(data) {
  return request.post('/user/users', data)
}

export function updateUserApi(id, data) {
  return request.put(`/user/users/${id}`, data)
}

export function deleteUserApi(id) {
  return request.delete(`/user/users/${id}`)
}

export function updateUserStatusApi(id, status) {
  return request.put(`/user/users/${id}/status`, { status })
}

export function resetUserPasswordApi(id, password) {
  return request.put(`/user/users/${id}/password`, { password })
}

export function assignUserRolesApi(id, ids) {
  return request.put(`/user/users/${id}/roles`, { ids })
}

export function getDeptTreeApi() {
  return request.get('/user/depts/tree')
}

export function createDeptApi(data) {
  return request.post('/user/depts', data)
}

export function updateDeptApi(id, data) {
  return request.put(`/user/depts/${id}`, data)
}

export function deleteDeptApi(id) {
  return request.delete(`/user/depts/${id}`)
}

export function getPostListApi() {
  return request.get('/user/posts')
}

export function createPostApi(data) {
  return request.post('/user/posts', data)
}

export function updatePostApi(id, data) {
  return request.put(`/user/posts/${id}`, data)
}

export function deletePostApi(id) {
  return request.delete(`/user/posts/${id}`)
}

export function getRoleListApi() {
  return request.get('/user/roles')
}

export function createRoleApi(data) {
  return request.post('/user/roles', data)
}

export function updateRoleApi(id, data) {
  return request.put(`/user/roles/${id}`, data)
}

export function deleteRoleApi(id) {
  return request.delete(`/user/roles/${id}`)
}

export function assignRoleMenusApi(id, ids) {
  return request.put(`/user/roles/${id}/menus`, { ids })
}

export function assignRoleApiPermissionsApi(id, ids) {
  return request.put(`/user/roles/${id}/api-permissions`, { ids })
}

export function getMenuListApi() {
  return request.get('/user/menus')
}

export function getApiPermissionListApi() {
  return request.get('/user/api-permissions')
}

export function createApiPermissionApi(data) {
  return request.post('/user/api-permissions', data)
}

export function updateApiPermissionApi(id, data) {
  return request.put(`/user/api-permissions/${id}`, data)
}

export function deleteApiPermissionApi(id) {
  return request.delete(`/user/api-permissions/${id}`)
}

export function getLoginLogsApi() {
  return request.get('/user/logs/login')
}

export function getOperationLogsApi() {
  return request.get('/user/logs/operations')
}
