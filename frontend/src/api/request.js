import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code !== 'undefined' && body.code !== 200) {
      const message = normalizeErrorMessage({ response })
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }
    return body
  },
  (error) => {
    const message = normalizeErrorMessage(error)
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  }
)

function normalizeErrorMessage(error) {
  const status = error.response?.status
  const data = error.response?.data
  const serverMessage = data && typeof data === 'object' ? data.message : ''

  if (serverMessage) {
    return serverMessage
  }
  if (status === 400) {
    return '请求参数不正确，请检查后重试'
  }
  if (status === 401) {
    return '登录已过期，请重新登录'
  }
  if (status === 403) {
    return '当前账号没有权限执行此操作'
  }
  if (status === 404) {
    return '请求的接口不存在'
  }
  if (status === 503) {
    return '权限服务暂时不可用，请稍后重试'
  }
  if (status >= 500) {
    return '服务器处理失败，请稍后重试'
  }
  if (error.code === 'ECONNABORTED') {
    return '请求超时，请检查服务是否正常启动'
  }
  if (error.message === 'Network Error') {
    return '网络连接失败，请检查网关或后端服务是否启动'
  }
  return error.message || '请求失败'
}

export default request
