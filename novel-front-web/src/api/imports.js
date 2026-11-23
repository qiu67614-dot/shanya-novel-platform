import request from '@/utils/request'

const base = '/front/me/imports'

export const uploadPrivateNovel = (file, options = {}, onProgress, cover) => {
  const form = new FormData()
  form.append('file', file)
  if (cover) form.append('cover', cover)
  Object.entries(options).forEach(([key, value]) => {
    if (value !== null && value !== undefined) form.append(key, String(value))
  })
  return request.post(base, form, {
    timeout: 120000,
    onUploadProgress: (event) => {
      if (event.total && onProgress) onProgress(Math.round(event.loaded * 100 / event.total))
    },
  })
}

export const getPrivateNovels = (params = {}) => request.get(base, { params })
export const getPrivateNovel = (novelId) => request.get(`${base}/${novelId}`)
export const getPrivateChapters = (novelId, params = {}) =>
  request.get(`${base}/${novelId}/chapters`, { params })
export const getPrivateChapter = (novelId, chapterId) =>
  request.get(`${base}/${novelId}/chapters/${chapterId}`)
export const savePrivateProgress = (novelId, chapterId) =>
  request.put(`${base}/${novelId}/progress`, { chapterId })
export const deletePrivateNovel = (novelId) => request.delete(`${base}/${novelId}`)
export const resubmitNovel = (novelId, options, file, cover) => {
  const form = new FormData()
  Object.entries(options).forEach(([key, value]) => {
    if (value !== null && value !== undefined) form.append(key, String(value))
  })
  if (file) form.append('file', file)
  if (cover) form.append('cover', cover)
  return request.post(`${base}/${novelId}/resubmit`, form, { timeout: 120000 })
}
export const cancelSubmission = (novelId) => request.post(`${base}/${novelId}/cancel-submission`)
export const getPublications = () => request.get('/front/me/publications')
export const requestWithdrawal = (bookId, reason) =>
  request.post(`/front/me/publications/${bookId}/withdrawals`, { reason })
