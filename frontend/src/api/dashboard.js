import request from './request'

export function getReportHealthApi() {
  return request.get('/api/report/health')
}

