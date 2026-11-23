<template>
  <router-view v-if="$route.name === 'login'" />
  <div v-else class="app-shell">
    <aside class="sidebar">
      <router-link class="brand" to="/submissions"><span class="brand-mark">山</span><span><strong>山崖书院</strong><small>内容管理</small></span></router-link>
      <nav class="side-nav" aria-label="管理导航">
        <router-link to="/submissions"><ClipboardCheck :size="18" /> 投稿审核</router-link>
        <router-link to="/withdrawals"><Inbox :size="18" /> 撤回申请</router-link>
        <router-link to="/publications"><LibraryBig :size="18" /> 已发布作品</router-link>
        <router-link to="/audit-logs"><ScrollText :size="18" /> 操作记录</router-link>
      </nav>
      <div class="sidebar-bottom"><span>{{ username }}</span><button type="button" title="退出登录" @click="logout"><LogOut :size="17" /><span>退出</span></button></div>
    </aside>
    <div class="shell-main">
      <header class="topbar"><button class="mobile-nav-toggle" type="button" :title="navOpen ? '关闭导航' : '打开导航'" :aria-expanded="navOpen" aria-controls="mobile-nav" @click="navOpen = !navOpen"><Menu :size="20" /></button><span>内容审核工作台</span><span class="topbar-status"><span></span> 管理员</span></header>
      <nav v-if="navOpen" id="mobile-nav" class="mobile-nav" aria-label="移动导航" @click="navOpen = false"><router-link to="/submissions">投稿审核</router-link><router-link to="/withdrawals">撤回申请</router-link><router-link to="/publications">已发布作品</router-link><router-link to="/audit-logs">操作记录</router-link><button @click="logout">退出登录</button></nav>
      <main class="page"><router-view /></main>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ClipboardCheck, Inbox, LibraryBig, ScrollText, LogOut, Menu } from 'lucide-vue-next'
import { clearAdminSession } from './api/client.js'

const router = useRouter()
const route = useRoute()
const username = ref(sessionStorage.getItem('novel_admin_username') || '管理员')
watch(() => route.fullPath, () => { username.value = sessionStorage.getItem('novel_admin_username') || '管理员' })
const navOpen = ref(false)
const logout = () => { clearAdminSession(); router.push('/login') }
</script>
