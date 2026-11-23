import defaultCover from '@/assets/images/default.gif'

export const resolveImageUrl = (url) => {
  if (!url) return defaultCover
  if (/^(https?:|data:|blob:)/i.test(url)) return url
  const base = process.env.VUE_APP_BASE_API_URL || '/api'
  return `${base}${url.startsWith('/') ? url : `/${url}`}`
}
