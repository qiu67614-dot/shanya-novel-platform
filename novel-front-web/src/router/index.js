import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'

const routes = [
  { path: '/', name: 'home', component: () => import('@/views/Home.vue') },
  { path: '/books', name: 'books', component: () => import('@/views/Books.vue') },
  { path: '/rank', name: 'rank', component: () => import('@/views/Rank.vue') },
  { path: '/book/:bookId', name: 'book', component: () => import('@/views/Book.vue') },
  { path: '/book/:bookId/chapters', name: 'chapters', component: () => import('@/views/Chapters.vue') },
  { path: '/read/:bookId/:chapterId', name: 'read', component: () => import('@/views/Read.vue') },
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue') },
  { path: '/register', name: 'register', component: () => import('@/views/Register.vue') },
  { path: '/me', name: 'me', component: () => import('@/views/Me.vue'), meta: { auth: true } },
  { path: '/me/bookshelf', name: 'bookshelf', component: () => import('@/views/Bookshelf.vue'), meta: { auth: true } },
  { path: '/me/history', name: 'history', component: () => import('@/views/History.vue'), meta: { auth: true } },
  { path: '/me/following', name: 'following', component: () => import('@/views/Following.vue'), meta: { auth: true } },
  { path: '/me/imports', name: 'imports', component: () => import('@/views/Imports.vue'), meta: { auth: true } },
  { path: '/me/publications', name: 'publications', component: () => import('@/views/Publications.vue'), meta: { auth: true } },
  { path: '/me/imports/:novelId/chapters', name: 'import-chapters', component: () => import('@/views/ImportChapters.vue'), meta: { auth: true } },
  { path: '/me/imports/:novelId/read/:chapterId', name: 'import-read', component: () => import('@/views/ImportRead.vue'), meta: { auth: true } },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  if (to.meta.auth && !getToken()) return { name: 'login', query: { redirect: to.fullPath } }
  return true
})

router.afterEach(() => window.scrollTo({ top: 0, behavior: 'smooth' }))

export default router
