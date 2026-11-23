<template>
  <ReaderLayout>
    <section class="page-heading">
      <div><p class="eyebrow">阅读记录</p><h1>阅读历史</h1></div>
      <router-link class="text-link" to="/books">找书</router-link>
    </section>
    <div v-if="loading" class="empty-state">正在加载阅读历史...</div>
    <div v-else-if="error" class="empty-state"><p>{{ error }}</p><button class="secondary-button" @click="load">重新加载</button></div>
    <div v-else-if="!history.length" class="empty-state">还没有阅读记录。</div>
    <section v-else class="history-list">
      <article v-for="entry in history" :key="`${entry.book.id}-${entry.chapterId}`" class="history-row">
        <BookNameCover :title="entry.book.bookName" :src="entry.book.picUrl" />
        <div class="history-row__body">
          <router-link class="history-title" :to="`/book/${entry.book.id}`">{{ entry.book.bookName }}</router-link>
          <p>{{ entry.book.authorName || '未知作者' }} · {{ entry.chapterName || '未知章节' }}</p>
          <small>最近阅读：{{ entry.readAt || '暂无时间' }}</small>
        </div>
        <router-link class="primary-button" :to="{ name: 'read', params: { bookId: entry.book.id, chapterId: entry.chapterId } }">继续阅读</router-link>
      </article>
    </section>
    <div v-if="pages > 1" class="pagination"><button :disabled="page <= 1" @click="changePage(page - 1)">上一页</button><span>第 {{ page }} / {{ pages }} 页</span><button :disabled="page >= pages" @click="changePage(page + 1)">下一页</button></div>
  </ReaderLayout>
</template>

<script>
import { computed, onMounted, ref } from 'vue'
import ReaderLayout from '@/components/common/ReaderLayout.vue'
import BookNameCover from '@/components/common/BookNameCover.vue'
import { getHistory } from '@/api/me'

export default {
  name: 'History',
  components: { ReaderLayout, BookNameCover },
  setup() {
    const history = ref([])
    const page = ref(1)
    const size = ref(20)
    const total = ref(0)
    const loading = ref(false)
    const error = ref('')
    const pages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))
    const load = async () => {
      loading.value = true
      error.value = ''
      try {
        const result = await getHistory({ page: page.value, size: size.value })
        const data = result.data || {}
        history.value = data.list || []
        page.value = data.page || 1
        size.value = data.size || 20
        total.value = data.total || 0
      } catch (requestError) {
        error.value = requestError.message || '阅读历史加载失败，请稍后重试'
      } finally {
        loading.value = false
      }
    }
    const changePage = (nextPage) => { page.value = nextPage; load() }
    onMounted(load)
    return { history, page, pages, loading, error, load, changePage }
  },
}
</script>



