<template>
  <ReaderLayout>
    <section class="page-heading">
      <div><p class="eyebrow">正式用户</p><h1>我的书架</h1></div>
      <router-link class="text-link" to="/books">继续找书</router-link>
    </section>
    <div v-if="loading" class="empty-state">正在加载书架...</div>
    <div v-else-if="error" class="empty-state"><p>{{ error }}</p><button class="secondary-button" @click="load">重新加载</button></div>
    <div v-else-if="!books.length" class="empty-state">书架还是空的，先去关注或阅读一本作品吧。</div>
    <section v-else class="book-grid">
      <article v-for="item in books" :key="item.book.id" class="book-card">
        <BookNameCover :title="item.book.bookName" :src="item.book.picUrl" />
        <div class="book-card__body">
          <router-link class="book-card__title" :to="`/book/${item.book.id}`">{{ item.book.bookName }}</router-link>
          <p class="book-card__meta">{{ item.book.authorName || '未知作者' }}</p>
          <p class="book-card__progress">读到：{{ item.lastChapterName || '尚未开始' }}</p>
          <div class="card-actions">
            <router-link class="primary-button" :to="readTarget(item)">{{ item.lastChapterId ? '继续阅读' : '开始阅读' }}</router-link>
            <button class="text-button" @click="remove(item.book.id)">移出书架</button>
          </div>
        </div>
      </article>
    </section>
  </ReaderLayout>
</template>

<script>
import { onMounted, ref } from 'vue'
import ReaderLayout from '@/components/common/ReaderLayout.vue'
import BookNameCover from '@/components/common/BookNameCover.vue'
import { getBookshelf, removeBookshelf } from '@/api/me'

export default {
  name: 'Bookshelf',
  components: { ReaderLayout, BookNameCover },
  setup() {
    const books = ref([])
    const loading = ref(false)
    const error = ref('')
    const load = async () => {
      loading.value = true
      error.value = ''
      try {
        const result = await getBookshelf()
        books.value = result.data || []
      } catch (requestError) {
        error.value = requestError.message || '书架加载失败，请稍后重试'
      } finally {
        loading.value = false
      }
    }
    const readTarget = (item) => ({
      name: 'read',
      params: {
        bookId: item.book.id,
        chapterId: item.lastChapterId || item.book.firstChapterId,
      },
    })
    const remove = async (bookId) => {
      await removeBookshelf(bookId)
      books.value = books.value.filter((item) => item.book.id !== bookId)
    }
    onMounted(load)
    return { books, loading, error, load, readTarget, remove }
  },
}
</script>



