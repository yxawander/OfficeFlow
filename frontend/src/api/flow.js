import request from './request'

export function getFlowHealthApi() {
  return request.get('/api/flow/health')
}

