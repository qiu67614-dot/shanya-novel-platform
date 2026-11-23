import axios from 'axios'
import { ElMessage } from 'element-plus'

export const adminToken = () => sessionStorage.getItem('novel_admin_token')
export const setAdminSession = (data) => {
  sessionStorage.setItem('novel_admin_token', data.token)
  sessionStorage.setItem('novel_admin_username', data.username)
}
export const clearAdminSession = () => {
  sessionStorage.removeItem('novel_admin_token')
  sessionStorage.removeItem('novel_admin_username')
}

const client = axios.create({ baseURL: '/api', timeout: 30000 })

/** 只有幂等请求才自动重试：审核、删除这类 POST/PUT 重试会造成重复操作 */
const IDEMPOTENT_METHODS = ['get', 'head', 'options']
const MAX_RETRY_AFTER_SECONDS = 5
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

const retryAfterSeconds = (headers = {}) => {
  const raw = Number(headers['retry-after'] ?? headers['Retry-After'])
  return Number.isFinite(raw) && raw > 0 ? Math.min(raw, MAX_RETRY_AFTER_SECONDS) : 1
}

client.interceptors.request.use((config) => {
  const token = adminToken()
  if (token) config.headers.Authorization = token
  return config
})
client.interceptors.response.use((response) => {
  const body = response.data
  if (body?.code !== '00000') {
    if (body?.code === 'A0403') {
      clearAdminSession()
      if (window.location.pathname !== '/login') window.location.assign('/login')
    }
    return Promise.reject(new Error(body?.message || '操作失败'))
  }
  return body.data
}, async (error) => {
  const status = error.response?.status
  const config = error.config

  if (status === 429) {
    const retryAfter = retryAfterSeconds(error.response?.headers)
    const method = (config?.method || 'get').toLowerCase()
    if (config && !config.__rateLimitRetried && IDEMPOTENT_METHODS.includes(method)) {
      config.__rateLimitRetried = true
      await sleep(retryAfter * 1000)
      return client(config)
    }
    return Promise.reject(new Error(error.response?.data?.message || `操作过于频繁，请 ${retryAfter} 秒后重试`))
  }

  return Promise.reject(new Error(error.response?.data?.message || error.message || '网络异常'))
})

export const notifyError = (error) => ElMessage.error(error?.message || '操作失败')
export default client
