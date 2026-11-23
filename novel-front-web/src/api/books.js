import request from '@/utils/request'
import {
  mapResponse,
  normalizeBook,
  normalizeBookPage,
  normalizeCategory,
  normalizeChapterContent,
  normalizeChapterPage,
  normalizeCommentPage,
} from '@/utils/reader-fields'

export const getHome = () => request.get('/front/books/home')
  .then((response) => mapResponse(response, (data = {}) => ({
    ...data,
    latest: (data.latest || []).map(normalizeBook),
    hot: (data.hot || []).map(normalizeBook),
    updated: (data.updated || data.recentlyUpdated || []).map(normalizeBook),
  })))
export const getCategories = (params) => request.get('/front/categories', { params })
  .then((response) => mapResponse(response, (data = []) => data.map(normalizeCategory)))
export const listBooks = (params) => request.get('/front/books', {
  params: Object.fromEntries(Object.entries(params || {}).filter(([, value]) => value !== '' && value !== undefined && value !== null)),
})
  .then((response) => mapResponse(response, normalizeBookPage))
export const getRank = (type) => request.get(`/front/ranks/${type}`)
  .then((response) => mapResponse(response, (data = []) => data.map(normalizeBook)))
export const getBook = (bookId) => request.get(`/front/books/${bookId}`)
  .then((response) => mapResponse(response, normalizeBook))
export const getChapters = (bookId, params) => request.get(`/front/books/${bookId}/chapters`, { params })
  .then((response) => mapResponse(response, normalizeChapterPage))
export const getChapter = (chapterId) => request.get(`/front/chapters/${chapterId}`)
  .then((response) => mapResponse(response, normalizeChapterContent))
export const visitBook = (bookId) => request.post(`/front/books/${bookId}/visit`)

export const getComments = (bookId, params = {}) => request.get(`/front/books/${bookId}/comments`, { params })
  .then((response) => mapResponse(response, normalizeCommentPage))

export const getCommentReplies = (bookId, commentId, params = {}) => request.get(
  `/front/books/${bookId}/comments/${commentId}/replies`,
  { params },
).then((response) => mapResponse(response, normalizeCommentPage))
