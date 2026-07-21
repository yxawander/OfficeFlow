import request from './request'

export function getNoticeHealthApi() {
  return request.get('/api/notice/health')
}

