import request from '@/utils/request'
import {
  mapResponse,
  normalizeBookshelfEntry,
  normalizeFollowingEntry,
  normalizeHistoryPage,
  normalizeUser,
} from '@/utils/reader-fields'

export const getMe = () => request.get('/front/me/me')
  .then((response) => mapResponse(response, normalizeUser))
export const updateMe = (payload) => request.put('/front/me/me', payload)
  .then((response) => mapResponse(response, normalizeUser))

export const getBookshelf = () => request.get('/front/me/bookshelf')
  .then((response) => mapResponse(response, (data = []) => data.map(normalizeBookshelfEntry)))

export const saveBookshelf = (bookId, payload) => request.put(`/front/me/bookshelf/${bookId}`, payload)
  .then((response) => mapResponse(response, normalizeBookshelfEntry))

export const removeBookshelf = (bookId) => request.delete(`/front/me/bookshelf/${bookId}`)

export const getHistory = (params = {}) => request.get('/front/me/history', { params })
  .then((response) => mapResponse(response, normalizeHistoryPage))

export const updateProgress = (payload) => request.put('/front/me/progress', payload)

export const getFollowing = () => request.get('/front/me/following')
  .then((response) => mapResponse(response, (data = []) => data.map(normalizeFollowingEntry)))

export const followBook = (bookId) => request.put(`/front/me/following/${bookId}`)
  .then((response) => mapResponse(response, normalizeFollowingEntry))

export const unfollowBook = (bookId) => request.delete(`/front/me/following/${bookId}`)

export const markFollowingRead = (bookId) => request.put(`/front/me/following/${bookId}/read`)
  .then((response) => mapResponse(response, normalizeFollowingEntry))

export const createComment = (bookId, payload) => request.post(`/front/me/books/${bookId}/comments`, payload)

export const deleteComment = (commentId) => request.delete(`/front/me/comments/${commentId}`)
