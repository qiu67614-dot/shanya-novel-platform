<template>
  <ReaderLayout>
    <article class="reader">
      <div class="reader-top">
        <router-link :to="`/book/${route.params.bookId}`">
          {{ data.book?.bookName || '返回作品' }}
        </router-link>
        <router-link :to="`/book/${route.params.bookId}/chapters`">目录</router-link>
      </div>

      <header>
        <h1>{{ data.chapter?.chapterName || (loading ? '正在加载...' : '章节不存在') }}</h1>
        <p v-if="data.chapter?.id">
          {{ data.book?.authorName || '未知作者' }} · {{ data.chapter.wordCount || 0 }} 字 · {{ data.chapter.updateTime || '' }}
        </p>
      </header>

      <div v-if="loading" class="empty-state">正在加载正文...</div>
      <div v-else-if="loadError" class="empty-state error-state">{{ loadError }}</div>
      <div v-else-if="data.content" class="content">{{ data.content }}</div>
      <div v-else class="empty-state">暂无正文内容</div>

      <nav class="chapter-nav">
        <button
          :disabled="!data.previousChapterId || navigating || loading"
          @click="go(data.previousChapterId)"
        >上一章</button>
        <router-link :to="`/book/${route.params.bookId}/chapters`">章节目录</router-link>
        <button
          :disabled="!data.nextChapterId || navigating || loading"
          @click="go(data.nextChapterId)"
        >下一章</button>
      </nav>
    </article>
  </ReaderLayout>
</template>

<script>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ReaderLayout from '@/components/common/ReaderLayout.vue'
import { getChapter } from '@/api/books'
import { updateProgress } from '@/api/me'
import { isLoggedIn } from '@/utils/auth'

const emptyChapter = () => ({
  book: {},
  chapter: {},
  content: '',
  previousChapterId: null,
  nextChapterId: null,
})

export default {
  name: 'Read',
  components: { ReaderLayout },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const data = reactive(emptyChapter())
    const loading = ref(false)
    const navigating = ref(false)
    const loadError = ref('')

    const load = async () => {
      loading.value = true
      loadError.value = ''
      Object.assign(data, emptyChapter())

      try {
        const result = await getChapter(route.params.chapterId)
        Object.assign(data, result.data || {})

        if (data.book?.id && data.chapter?.id && isLoggedIn()) {
          updateProgress({ bookId: data.book.id, chapterId: data.chapter.id }).catch(() => {})
        }
        window.scrollTo({ top: 0, behavior: 'smooth' })
      } catch (error) {
        loadError.value = error?.message || '章节加载失败，请稍后重试'
      } finally {
        loading.value = false
      }
    }

    const go = async (chapterId) => {
      const numericChapterId = Number(chapterId)
      if (!Number.isInteger(numericChapterId) || numericChapterId <= 0 || navigating.value) {
        return
      }

      navigating.value = true
      try {
        await router.push({
          name: 'read',
          params: {
            bookId: data.chapter?.bookId || route.params.bookId,
            chapterId: String(numericChapterId),
          },
        })
      } catch (error) {
        ElMessage.error('章节跳转失败，请稍后重试')
      } finally {
        navigating.value = false
      }
    }

    onMounted(load)
    watch(() => route.params.chapterId, load)

    return {
      route,
      data,
      loading,
      navigating,
      loadError,
      go,
    }
  },
}
</script>


