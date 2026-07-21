import request from './request'

export function getAttendanceHealthApi() {
  return request.get('/api/attendance/health')
}

