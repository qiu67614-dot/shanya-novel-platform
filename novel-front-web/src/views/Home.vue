<template>
  <ReaderLayout>
    <section class="home-hero" data-enter>
      <div class="home-hero__inner">
        <div class="hero-copy">
          <p class="eyebrow">在线小说阅读</p>
          <h1>山崖书院</h1>
          <p class="hero-lead">找一本想看的小说，从第一页读起。游客可直接阅读，登录后接着上次的章节继续。</p>
          <form class="hero-search" role="search" @submit.prevent="searchBooks">
            <label class="visually-hidden" for="home-search">搜索书名或作者</label>
            <input id="home-search" v-model.trim="searchTerm" type="search" placeholder="搜索书名或作者" />
            <button type="submit">找书 <span aria-hidden="true">↗</span></button>
          </form>
          <div class="hero-actions"><router-link to="/books">浏览书库</router-link><router-link to="/rank">查看榜单</router-link></div>
        </div>
        <div class="home-hero__bottom">
          <div v-if="featured" class="hero-featured">
            <span class="hero-featured__label">热门作品</span>
            <router-link :to="'/book/' + featured.id">{{ featured.bookName }}</router-link>
            <span class="hero-featured__author">{{ featured.authorName || '佚名' }}</span>
          </div>
          <router-link v-else class="hero-featured" to="/books">浏览全部作品 <span aria-hidden="true">↗</span></router-link>
          <span class="hero-bottom-note">卷一 · 从这里开始</span>
        </div>
      </div>
    </section>
    <div v-if="state.loading" class="empty-state">正在加载书库…</div>
    <div v-else-if="state.error" class="empty-state"><p>{{ state.error }}</p><button class="secondary-button" @click="load">重新加载</button></div>
    <template v-else>
      <section v-for="section in sections" :key="section.key" class="home-section">
        <div class="home-section__inner">
          <div class="section-heading"><div><span class="section-index">{{ section.index }}</span><h2>{{ section.title }}</h2></div><router-link :to="section.href">{{ section.link }} <span aria-hidden="true">↗</span></router-link></div>
          <div v-if="home[section.key].length" class="home-grid">
            <article v-for="book in home[section.key].slice(0, 5)" :key="book.id" class="home-card">
              <router-link :to="'/book/' + book.id" :aria-label="'查看《' + book.bookName + '》'"><BookNameCover :title="book.bookName" :src="book.picUrl" /></router-link>
              <div class="home-card__text"><router-link :to="'/book/' + book.id">{{ book.bookName }}</router-link><p>{{ book.authorName || '佚名' }} · {{ book.categoryName || '未分类' }}</p></div>
            </article>
          </div>
          <div v-else class="empty-state">暂无作品</div>
        </div>
      </section>
    </template>
  </ReaderLayout>
</template>
<script>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import ReaderLayout from '@/components/common/ReaderLayout.vue'
import BookNameCover from '@/components/common/BookNameCover.vue'
import { getHome } from '@/api/books'
export default {
  name: 'Home',
  components: { ReaderLayout, BookNameCover },
  setup() {
    const router = useRouter()
    const searchTerm = ref('')
    const home = reactive({ latest: [], hot: [], updated: [] })
    const state = reactive({ loading: true, error: '' })
    const featured = computed(() => home.hot[0] || home.latest[0] || home.updated[0])
    const sections = [
      { key: 'hot', index: '01', title: '热门阅读', link: '查看榜单', href: '/rank' },
      { key: 'updated', index: '02', title: '最近更新', link: '浏览书库', href: '/books?sort=update' },
      { key: 'latest', index: '03', title: '最新上架', link: '浏览书库', href: '/books?sort=new' },
    ]
    const searchBooks = () => router.push({ path: '/books', query: { q: searchTerm.value || undefined } })
    const load = async () => {
      state.loading = true
      state.error = ''
      try {
        const result = await getHome()
        Object.assign(home, { latest: [], hot: [], updated: [], ...(result.data || {}) })
      } catch (error) {
        state.error = error.message || '首页加载失败，请稍后重试'
      } finally { state.loading = false }
    }
    onMounted(load)
    return { home, sections, featured, load, state, searchTerm, searchBooks }
  },
}
</script>
