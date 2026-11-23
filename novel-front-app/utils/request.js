const TOKEN_KEY = 'novel_app_token'
const USER_KEY = 'novel_app_user'
const SERVER_KEY = 'novel_app_server'
const ENTRY_CHOICE_KEY = 'novel_app_entry_choice'

export const getToken = () => uni.getStorageSync(TOKEN_KEY) || ''
export const getUser = () => uni.getStorageSync(USER_KEY) || null
export const getEntryChoice = () => uni.getStorageSync(ENTRY_CHOICE_KEY) || ''
export const setEntryChoice = (choice) => uni.setStorageSync(ENTRY_CHOICE_KEY, choice)
export const normalizeUser = (source = {}) => ({
  id: source.id ?? source.uid,
  username: source.username || '',
  nickName: source.nickName ?? source.nickname ?? '',
  userPhoto: source.userPhoto ?? source.userPhotp ?? source.avatar ?? '',
  userSex: source.userSex ?? source.gender ?? 0,
  userBirthday: source.userBirthday ?? '',
  userPhone: source.userPhone ?? '',
  userEmail: source.userEmail ?? source.email ?? '',
})
export const saveSession = (result) => {
  uni.setStorageSync(TOKEN_KEY, result.token)
  uni.setStorageSync(USER_KEY, normalizeUser({ ...result.user, id: result.user?.id ?? result.uid, nickName: result.user?.nickName ?? result.nickName ?? result.user?.nickname }))
  setEntryChoice('member')
}
export const saveUser = (user) => uni.setStorageSync(USER_KEY, normalizeUser(user))
export const clearSession = () => {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}
export const getServer = () => uni.getStorageSync(SERVER_KEY) || 'http://10.0.2.2:8080'
export const setServer = (value) => uni.setStorageSync(SERVER_KEY, value.replace(/\/$/, ''))
export const normalizeCaptcha = (source = {}) => ({
  key: source.captchaKey || source.sessionId || '',
  image: source.image || (source.img ? (source.img.startsWith('data:') ? source.img : `data:image/png;base64,${source.img}`) : ''),
})

const baseUrl = () => {
  // #ifdef H5
  return '/api'
  // #endif
  // #ifndef H5
  return getServer()
  // #endif
}

const queryString = (params) => {
  const values = Object.entries(params || {}).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  return values.length ? `?${values.map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`).join('&')}` : ''
}

export function request(path, { method = 'GET', data, params, auth = false } = {}) {
  if (auth && !getToken()) return Promise.reject(new Error('请先登录'))
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${baseUrl()}${path}${queryString(params)}`,
      method,
      data,
      timeout: 20000,
      header: { 'Content-Type': 'application/json', ...(getToken() ? { Authorization: getToken() } : {}) },
      success: (response) => {
        const body = response.data
        if (!body || body.code !== '00000') {
          if (body?.code === 'A0230') clearSession()
          reject(new Error(body?.message || `请求失败 (${response.statusCode})`))
        } else resolve(body.data)
      },
      fail: (error) => reject(new Error(error.errMsg || '连接服务器失败')),
    })
  })
}

export function uploadTxt(filePath, fields, onProgress, path = '/front/me/imports', coverPath = '') {
  if (!getToken()) return Promise.reject(new Error('请先登录'))
  if (!filePath && !coverPath) return Promise.reject(new Error('请选择 TXT 文件或封面'))
  return new Promise((resolve, reject) => {
    const files = [filePath && { name: 'file', uri: filePath }, coverPath && { name: 'cover', uri: coverPath }].filter(Boolean)
    const task = uni.uploadFile({
      url: `${baseUrl()}${path}`,
      ...(coverPath ? { files }
        : { filePath, name: 'file' }),
      formData: fields,
      header: { Authorization: getToken() },
      timeout: 120000,
      success: (response) => {
        try {
          const body = JSON.parse(response.data)
          if (body.code === '00000') resolve(body.data)
          else reject(new Error(body.message || '导入失败'))
        } catch (error) { reject(error) }
      },
      fail: (error) => reject(new Error(error.errMsg || '上传失败')),
    })
    if (onProgress) task.onProgressUpdate(({ progress }) => onProgress(progress))
  })
}

export function requireLogin(redirect = '') {
  if (getToken()) return true
  uni.navigateTo({ url: `/pages/auth/login${redirect ? `?redirect=${encodeURIComponent(redirect)}` : ''}` })
  return false
}

export const showError = (error) => uni.showToast({ title: error?.message || '操作失败', icon: 'none', duration: 2500 })
