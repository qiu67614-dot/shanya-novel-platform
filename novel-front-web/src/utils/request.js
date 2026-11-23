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
}, (error) => {
  if (error.response?.status === 401) {
    clearAuth()
    router.push({ name: 'login' })
  }
  ElMessage.error(error.response?.data?.message || '网络异常，请稍后再试')
  return Promise.reject(error)
})

export default request
