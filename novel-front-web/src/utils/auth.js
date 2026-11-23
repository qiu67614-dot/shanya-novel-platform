import { normalizeUser } from '@/utils/reader-fields'

const TOKEN_KEY = 'Authorization'
const USER_KEY = 'reader_user'
const UID_KEY = 'reader_uid'

export const getToken = () => localStorage.getItem(TOKEN_KEY)
export const setToken = (token) => localStorage.setItem(TOKEN_KEY, token)
export const getUser = () => {
  try { return normalizeUser(JSON.parse(localStorage.getItem(USER_KEY) || 'null')) } catch { return null }
}
export const setUser = (user) => {
  const normalized = normalizeUser(user)
  if (normalized) localStorage.setItem(USER_KEY, JSON.stringify(normalized))
  else localStorage.removeItem(USER_KEY)
}
export const getUid = () => localStorage.getItem(UID_KEY)
export const setUid = (uid) => localStorage.setItem(UID_KEY, String(uid))
export const clearAuth = () => {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(UID_KEY)
}
export const isLoggedIn = () => Boolean(getToken())
