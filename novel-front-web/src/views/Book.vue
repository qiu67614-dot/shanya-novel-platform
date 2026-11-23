<template>
  <ReaderLayout>
    <div v-if="book.id" class="book-detail-page">
      <section class="book-hero">
        <BookNameCover :title="book.bookName" :src="book.picUrl" />
        <div>
          <p class="eyebrow">{{ book.categoryName || '小说' }}</p>
          <h1>{{ book.bookName }}</h1>
          <p class="author">作者：{{ book.authorName || '佚名' }}</p>
          <p class="desc">{{ book.bookDesc || '暂无简介' }}</p>
          <div class="actions">
            <router-link
              class="primary-button"
              :to="book.firstChapterId
                ? { name: 'read', params: { bookId: book.id, chapterId: book.firstChapterId } }
                : { name: 'chapters', params: { bookId: book.id } }"
            >
              开始阅读
            </router-link>
            <router-link class="secondary-button" :to="{ name: 'chapters', params: { bookId: book.id } }">查看目录</router-link>
            <button class="secondary-button" type="button" @click="toggleShelf">
              {{ inBookshelf ? '已在书架' : '加入书架' }}
            </button>
            <button class="secondary-button" type="button" @click="toggleFollow">
              {{ followState.following ? '已关注' : '关注本书' }}
              <span v-if="followState.hasUpdate" class="update-badge">有更新</span>
            </button>
          </div>
        </div>
      </section>

      <section class="book-facts">
        <span>状态：{{ book.bookStatus === 1 ? '已完结' : '连载中' }}</span>
        <span>字数：{{ book.wordCount || 0 }}</span>
        <span>阅读：{{ book.visitCount || 0 }}</span>
        <span>最新：{{ book.lastChapterName || '暂无章节' }}</span>
      </section>

      <CommentSection :book-id="book.id" />
    </div>
    <div v-else class="empty-state">正在加载作品...</div>
  </ReaderLayout>
</template>

<script>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElNotification } from 'element-plus'
import ReaderLayout from '@/components/common/ReaderLayout.vue'
import BookNameCover from '@/components/common/BookNameCover.vue'
import CommentSection from '@/components/book/CommentSection.vue'
import { getBook, visitBook } from '@/api/books'
import { followBook, getBookshelf, getFollowing, removeBookshelf, saveBookshelf, unfollowBook } from '@/api/me'
import { getToken } from '@/utils/auth'

export default {
  name: 'Book',
  components: { ReaderLayout, BookNameCover, CommentSection },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const book = ref({})
    const inBookshelf = ref(false)
    const followState = reactive({ following: false, hasUpdate: false })

    const load = async () => {
      const id = route.params.bookId
      const result = await getBook(id)
      book.value = result.data || {}
      if (getToken()) {
        try {
          const [shelf, following] = await Promise.all([getBookshelf(), getFollowing()])
          inBookshelf.value = (shelf.data || []).some((item) => String(item.book?.id) === String(id))
          const entry = (following.data || []).find((item) => String(item.book?.id) === String(id))
          Object.assign(followState, { following: Boolean(entry), hasUpdate: Boolean(entry?.hasUpdate) })
          if (entry?.hasUpdate) ElNotification({ title: '关注作品有更新', message: `《${book.value.bookName}》已发布新章节`, type: 'success' })
        } catch (error) {
          // 鉴权失效由请求拦截器处理。
        }
      }
      visitBook(id).catch(() => {})
    }

    const toggleShelf = async () => {
      if (!getToken()) {
        ElMessage.info('登录后可以使用书架')
        router.push({ name: 'login', query: { redirect: route.fullPath } })
        return
      }
      if (inBookshelf.value) {
        await removeBookshelf(book.value.id)
        inBookshelf.value = false
        ElMessage.success('已移出书架')
      } else {
        await saveBookshelf(book.value.id)
        inBookshelf.value = true
        ElMessage.success('已加入书架')
      }
    }

    const toggleFollow = async () => {
      if (!getToken()) {
        ElMessage.info('登录后可以关注作品并接收更新提醒')
        router.push({ name: 'login', query: { redirect: route.fullPath } })
        return
      }
      if (followState.following) {
        await unfollowBook(book.value.id)
        Object.assign(followState, { following: false, hasUpdate: false })
        ElMessage.success('已取消关注')
      } else {
        const result = await followBook(book.value.id)
        Object.assign(followState, { following: true, hasUpdate: Boolean(result.data?.hasUpdate) })
        ElMessage.success('已关注，作品更新时会提醒你')
      }
    }

    onMounted(load)
    return { book, inBookshelf, followState, toggleShelf, toggleFollow }
  },
}
</script>
