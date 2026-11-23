import axios from 'axios'
import router from '@/router'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from '@/utils/auth'

const request = axios.create({
  baseURL: process.env.VUE_APP_BASE_API_URL || '/api',
  timeout: 10000,
  withCredentials: true,
  headers: { 'X-Requested-With': 'XMLHttpRequest' },
})

/** 只有幂等请求才自动重试：给 POST 重试会重复提交评论、点赞 */
const IDEMPOTENT_METHODS = ['get', 'head', 'options']
const MAX_RETRY_AFTER_SECONDS = 5
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

const retryAfterSeconds = (headers = {}) => {
  const raw = Number(headers['retry-after'] ?? headers['Retry-After'])
  return Number.isFinite(raw) && raw > 0 ? Math.min(raw, MAX_RETRY_AFTER_SECONDS) : 1
}

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = token
  return config
})

request.interceptors.response.use((response) => {
  const body = response.data
  if (!body || typeof body !== 'object') return Promise.reject(new Error('Invalid response'))
  if (body.code !== '00000') {
    if (body.code === 'A0230') {
      clearAuth()
      router.push({ name: 'login' })
    }
    if (body.message) ElMessage.error(body.message)
    return Promise.reject(body)
  }
  return body
}, async (error) => {
  const status = error.response?.status
  const config = error.config

  if (status === 401) {
    clearAuth()
    router.push({ name: 'login' })
  }

  if (status === 429) {
    const retryAfter = retryAfterSeconds(error.response?.headers)
    const method = (config?.method || 'get').toLowerCase()
    if (config && !config.__rateLimitRetried && IDEMPOTENT_METHODS.includes(method)) {
      config.__rateLimitRetried = true
      await sleep(retryAfter * 1000)
      return request(config)
    }
    ElMessage.warning(error.response?.data?.message || `操作过于频繁，请 ${retryAfter} 秒后重试`)
    return Promise.reject(error)
  }

  ElMessage.error(error.response?.data?.message || '网络异常，请稍后再试')
  return Promise.reject(error)
})

export default request
