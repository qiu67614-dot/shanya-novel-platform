<template>
  <ReaderLayout>
    <section class="page-heading"><div><p class="eyebrow">阅读入口</p><h1>找一本想看的小说</h1></div><span>{{ pagination.total }} 本作品</span></section>
    <section class="toolbar">
      <input v-model="filters.q" placeholder="书名、作者" @keyup.enter="search" />
      <select v-model="filters.categoryId" @change="search"><option value="">全部分类</option><option v-for="category in categories" :key="category.id" :value="category.id">{{ category.categoryName }}</option></select>
      <select v-model="filters.status" @change="search"><option value="">全部状态</option><option value="0">连载中</option><option value="1">已完结</option></select>
      <select v-model="filters.sort" @change="search"><option value="update">最近更新</option><option value="visit">最多阅读</option><option value="new">最新上架</option></select>
      <button class="primary-button" @click="search">搜索</button>
    </section>
    <div v-if="loading" class="empty-state">正在加载书库...</div>
    <div v-else-if="error" class="empty-state"><p>{{ error }}</p><button class="secondary-button" @click="loadBooks">重新加载</button></div>
    <div v-else-if="!books.length" class="empty-state">暂时没有找到作品</div>
    <section v-else class="book-grid"><article v-for="book in books" :key="book.id" class="book-card"><BookNameCover :title="book.bookName" :src="book.picUrl" /><div class="book-card__body"><router-link :to="`/book/${book.id}`" class="book-card__title">{{ book.bookName }}</router-link><p class="book-card__meta">{{ book.authorName }} · {{ book.categoryName || '未分类' }}</p><p class="book-card__desc">{{ book.bookDesc || '暂无简介' }}</p><router-link class="text-link" :to="`/book/${book.id}`">查看详情</router-link></div></article></section>
    <div class="pagination"><button :disabled="pagination.page <= 1" @click="changePage(pagination.page - 1)">上一页</button><span>第 {{ pagination.page }} / {{ pages }} 页</span><button :disabled="pagination.page >= pages" @click="changePage(pagination.page + 1)">下一页</button></div>
  </ReaderLayout>
</template>

<script>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ReaderLayout from '@/components/common/ReaderLayout.vue'
import BookNameCover from '@/components/common/BookNameCover.vue'
import { getCategories, listBooks } from '@/api/books'

export default { name: 'Books', components: { ReaderLayout, BookNameCover }, setup() {
  const route = useRoute(); const router = useRouter(); const loading = ref(false); const books = ref([]); const categories = ref([]); const error = ref('')
  const filters = reactive({ q: route.query.q || '', categoryId: route.query.categoryId || '', status: route.query.status || '', sort: route.query.sort || 'update' }); const pagination = reactive({ page: Number(route.query.page) || 1, size: 20, total: 0 })
  const pages = computed(() => Math.max(1, Math.ceil(pagination.total / pagination.size)))
  const updateQuery = () => router.replace({ query: { q: filters.q || undefined, categoryId: filters.categoryId || undefined, status: filters.status || undefined, sort: filters.sort || undefined, page: pagination.page > 1 ? pagination.page : undefined } })
  const loadBooks = async () => { loading.value = true; error.value = ''; try { const result = await listBooks({ ...filters, page: pagination.page, size: pagination.size }); const data = result.data || {}; books.value = data.list || []; Object.assign(pagination, { page: data.page || 1, size: data.size || 20, total: data.total || 0 }) } catch (requestError) { error.value = requestError.message || '书库加载失败，请稍后重试' } finally { loading.value = false } }
  const search = () => { pagination.page = 1; updateQuery(); loadBooks() }
  const changePage = (page) => { pagination.page = page; updateQuery(); loadBooks() }
  onMounted(async () => { try { const result = await getCategories(); categories.value = result.data || [] } catch (requestError) { error.value = requestError.message || '分类加载失败，请稍后重试' } await loadBooks() })
  return { books, categories, filters, pagination, pages, loading, error, loadBooks, search, changePage }
} }
</script>



