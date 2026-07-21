import request from './request'

export function loginApi(data) {
  return request.post('/api/user/auth/login', data)
}

export function getUserHealthApi() {
  return request.get('/api/user/health')
}

