import request from './request'

export function getDashboardOverviewApi() {
  return request.get('/attendance/dashboard/overview')
}

export function getWeeklyTrendApi() {
  return request.get('/attendance/dashboard/weekly-trend')
}

export function getDeptHeatmapApi() {
  return request.get('/attendance/dashboard/dept-heatmap')
}

export function getFlowDistributionApi() {
  return request.get('/attendance/dashboard/flow-distribution')
}

