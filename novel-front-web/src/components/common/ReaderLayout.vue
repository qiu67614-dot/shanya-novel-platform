<template>
  <div class="reader-app">
    <a class="skip-link" href="#main-content">跳到正文</a>
    <header class="reader-header">
      <div class="reader-header__inner">
        <router-link class="reader-brand" to="/" aria-label="山崖书院首页"><span class="brand-mark">山</span><span>山崖书院</span></router-link>
        <nav class="reader-nav" :class="{ 'is-open': menuOpen }" aria-label="主导航">
          <router-link to="/books" @click="closeMenu">找书</router-link>
          <router-link to="/rank" @click="closeMenu">榜单</router-link>
          <router-link v-if="loggedIn" to="/me/bookshelf" @click="closeMenu">书架</router-link>
          <router-link v-if="loggedIn" to="/me/history" @click="closeMenu">历史</router-link>
          <router-link v-if="loggedIn" to="/me/following" @click="closeMenu">关注</router-link>
          <router-link v-if="loggedIn" to="/me/imports" @click="closeMenu">我的导入</router-link>
          <router-link v-if="loggedIn" to="/me/publications" @click="closeMenu">我的公开</router-link>
        </nav>
        <form class="reader-search" role="search" @submit.prevent="search">
          <label class="visually-hidden" for="site-search">搜索书名或作者</label>
          <input id="site-search" v-model="keyword" placeholder="搜索书名或作者" />
          <button type="submit" aria-label="搜索小说">搜索 <span aria-hidden="true">↗</span></button>
        </form>
        <div class="reader-account">
          <router-link v-if="loggedIn" class="account-link" to="/me">{{ userName }}</router-link>
          <router-link v-else class="account-link" to="/login">登录</router-link>
          <button v-if="loggedIn" class="reader-link-button" type="button" @click="signOut">退出</button>
          <router-link v-else class="register-link" to="/register">注册</router-link>
        </div>
        <button class="menu-toggle" type="button" :aria-expanded="menuOpen" aria-label="切换导航" @click="menuOpen = !menuOpen"><span></span><span></span></button>
      </div>
    </header>
    <main id="main-content" class="reader-main" :class="{ 'reader-main--home': route.name === 'home', 'reader-main--reading': route.name === 'read' || route.name === 'import-read' }"><slot /></main>
    <footer class="reader-footer"><div class="reader-footer__inner"><router-link class="footer-brand" to="/">山崖书院</router-link><span>找书、阅读、继续上一次的章节。</span><nav aria-label="页脚导航"><router-link to="/books">书库</router-link><router-link to="/rank">榜单</router-link><router-link v-if="loggedIn" to="/me/bookshelf">我的书架</router-link><router-link v-if="loggedIn" to="/me/imports">我的导入</router-link><router-link v-if="loggedIn" to="/me/publications">我的公开</router-link></nav></div></footer>
  </div>
</template>
<script>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearAuth, getToken, getUser } from '@/utils/auth'
export default {
  name: 'ReaderLayout',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const keyword = ref('')
    const user = ref(getUser())
    const token = ref(getToken())
    const menuOpen = ref(false)
    const loggedIn = computed(() => Boolean(token.value))
    const userName = computed(() => user.value?.nickName || '我的账户')
    const closeMenu = () => { menuOpen.value = false }
    const search = () => { closeMenu(); router.push({ path: '/books', query: { q: keyword.value.trim() || undefined } }) }
    const signOut = async () => {
      clearAuth()
      user.value = null
      token.value = null
      closeMenu()
      await router.push('/')
    }
    watch(() => route.fullPath, () => {
      user.value = getUser()
      token.value = getToken()
      menuOpen.value = false
    })
    return { route, keyword, loggedIn, userName, menuOpen, closeMenu, search, signOut }
  },
}
</script>
