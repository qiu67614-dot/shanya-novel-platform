import request from '@/utils/request'
import { mapResponse, normalizeAuthData, normalizeCaptcha } from '@/utils/reader-fields'

export const getCaptcha = () => request.get('/front/auth/captcha')
  .then((response) => mapResponse(response, normalizeCaptcha))
export const login = (payload) => request.post('/front/auth/login', payload)
  .then((response) => mapResponse(response, normalizeAuthData))
export const register = (payload) => request.post('/front/auth/register', payload)
  .then((response) => mapResponse(response, normalizeAuthData))
