import request from './request'

// 手动触发生成月度考勤报表
export function generateMonthlyReportApi(month) {
  return request.post('/api/attendance/monthly-reports/generate', null, { params: { month } })
}

// 查询月度考勤报表列表
export function getMonthlyReportsApi(params) {
  return request.get('/api/attendance/monthly-reports', { params })
}

// 手动触发全员月度工资结算
export function generateMonthlySalaryApi(month) {
  return request.post('/api/attendance/salary/generate', null, { params: { month } })
}

// 管理员/HR 查询全员工资结算单列表
export function getSalaryStatementsApi(params) {
  return request.get('/api/attendance/salary/statements', { params })
}

// 员工查询个人特定月份工资条
export function getMySalaryStatementApi(params) {
  return request.get('/api/attendance/salary/my', { params })
}

// 批量发布/发放工资单
export function publishSalaryStatementsApi(ids) {
  return request.post('/api/attendance/salary/publish', ids)
}
