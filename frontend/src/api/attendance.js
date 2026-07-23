import request from './request'

// 考勤健康检查
export function getAttendanceHealthApi() {
  return request.get('/api/attendance/health')
}

// 获取今日打卡状态
export function getTodayStatusApi() {
  return request.get('/api/attendance/today')
}

// 获取当前用户适用的定位打卡配置
export function getLocationConfigApi() {
  return request.get('/api/attendance/location-config')
}

// 上班打卡
export function checkInApi(data) {
  return request.post('/api/attendance/check-in', data)
}

// 下班打卡
export function checkOutApi(data) {
  return request.post('/api/attendance/check-out', data)
}

// 分页查询个人考勤记录
export function getMyRecordsApi(params) {
  return request.get('/api/attendance/my-records', { params })
}

// 部门今日考勤实时监控
export function getDeptTodayOverviewApi(params) {
  return request.get('/api/attendance/dept-today', { params })
}

// 查询考勤规则列表
export function getRulesApi() {
  return request.get('/api/attendance/rules')
}

// 创建考勤规则
export function createRuleApi(data) {
  return request.post('/api/attendance/rules', data)
}

// 修改考勤规则参数（上班时间、迟到缓冲等）
export function updateRuleApi(id, data) {
  return request.put(`/api/attendance/rules/${id}`, data)
}

// 查询考勤组列表（绑定部门与规则）
export function getGroupsApi() {
  return request.get('/api/attendance/groups')
}

// 新增考勤组
export function createGroupApi(data) {
  return request.post('/api/attendance/groups', data)
}

// 修改考勤组（更新部门与规则绑定）
export function updateGroupApi(id, data) {
  return request.put(`/api/attendance/groups/${id}`, data)
}

// 提交补卡申请
export function recheckApi(data) {
  return request.post('/api/attendance/recheck', data)
}

// 查询个人补卡申请历史
export function getMyCorrectionsApi() {
  return request.get('/api/attendance/corrections')
}
