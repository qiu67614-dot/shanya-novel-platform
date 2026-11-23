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
}, (error) => Promise.reject(new Error(error.response?.data?.message || error.message || '网络异常')))

export const notifyError = (error) => ElMessage.error(error?.message || '操作失败')
export default client
