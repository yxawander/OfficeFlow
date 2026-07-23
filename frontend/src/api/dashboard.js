import request from './request'

export function getDashboardOverviewApi() {
  return request.get('/api/attendance/dashboard/overview')
}

export function getWeeklyTrendApi() {
  return request.get('/api/attendance/dashboard/weekly-trend')
}

export function getDeptHeatmapApi() {
  return request.get('/api/attendance/dashboard/dept-heatmap')
}

export function getFlowDistributionApi() {
  return request.get('/api/attendance/dashboard/flow-distribution')
}

