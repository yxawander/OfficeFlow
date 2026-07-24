import request from './request'

// 考勤健康检查
export function getAttendanceHealthApi() {
  return request.get('/attendance/health')
}

// 获取今日打卡状态
export function getTodayStatusApi() {
  return request.get('/attendance/today')
}

// 获取当前用户适用的定位打卡配置
export function getLocationConfigApi() {
  return request.get('/attendance/location-config')
}

// 上班打卡
export function checkInApi(data) {
  return request.post('/attendance/check-in', data)
}

// 下班打卡
export function checkOutApi(data) {
  return request.post('/attendance/check-out', data)
}

// 分页查询个人考勤记录
export function getMyRecordsApi(params) {
  return request.get('/attendance/my-records', { params })
}

// 部门今日考勤实时监控
export function getDeptTodayOverviewApi(params) {
  return request.get('/attendance/dept-today', { params })
}

// 查询考勤规则列表
export function getRulesApi() {
  return request.get('/attendance/rules')
}

// 创建考勤规则
export function createRuleApi(data) {
  return request.post('/attendance/rules', data)
}

// 修改考勤规则参数（上班时间、迟到缓冲等）
export function updateRuleApi(id, data) {
  return request.put(`/attendance/rules/${id}`, data)
}

// 查询考勤组列表（绑定部门与规则）
export function getGroupsApi() {
  return request.get('/attendance/groups')
}
