import { createRouter, createWebHistory } from 'vue-router'
import { adminToken } from './api/client.js'

const routes = [
  { path: '/login', name: 'login', component: () => import('./views/Login.vue') },
  { path: '/', redirect: '/submissions' },
  { path: '/submissions', name: 'submissions', component: () => import('./views/Submissions.vue'), meta: { auth: true } },
  { path: '/submissions/:id', name: 'submission', component: () => import('./views/SubmissionDetail.vue'), meta: { auth: true } },
  { path: '/withdrawals', name: 'withdrawals', component: () => import('./views/Withdrawals.vue'), meta: { auth: true } },
  { path: '/publications', name: 'publications', component: () => import('./views/Published.vue'), meta: { auth: true } },
  { path: '/audit-logs', name: 'audit-logs', component: () => import('./views/AuditLogs.vue'), meta: { auth: true } },
]
const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach((to) => to.meta.auth && !adminToken() ? { name: 'login', query: { redirect: to.fullPath } } : true)
export default router
