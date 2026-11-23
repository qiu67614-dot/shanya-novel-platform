<template><ReaderLayout><section class="page-heading"><div><p class="eyebrow">章节目录</p><h1>{{ book.bookName || '作品' }}</h1></div><router-link class="text-link" :to="`/book/${route.params.bookId}`">返回详情</router-link></section><div class="chapter-list"><router-link v-for="chapter in chapters" :key="chapter.id" :to="`/read/${route.params.bookId}/${chapter.id}`"><span>第{{ chapter.chapterNum }}章</span>{{ chapter.chapterName }}<small>{{ chapter.wordCount || 0 }} 字</small></router-link><div v-if="!chapters.length" class="empty-state">暂无章节</div></div></ReaderLayout></template>
<script>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import ReaderLayout from '@/components/common/ReaderLayout.vue'
import { getBook, getChapters } from '@/api/books'

// 与后端 ChapterServiceImpl.MAX_PAGE_SIZE 保持一致，单次不超过 200 条
const PAGE_SIZE = 200
// 兜底上限：最多 50 页（1 万章），避免异常数据导致无限翻页
const MAX_PAGES = 50

// 目录页需要展示整本，但不再用一个 size=10000 的请求一次拉完，
// 改为分页顺序拉取后合并，单次响应体和数据库单查询都被限制在 200 条以内
async function loadAllChapters(bookId) {
  const all = []
  for (let page = 1; page <= MAX_PAGES; page += 1) {
    const result = await getChapters(bookId, { page, size: PAGE_SIZE })
    const data = result.data || {}
    const list = data.list || (Array.isArray(data) ? data : [])
    all.push(...list)
    const total = Number(data.total || 0)
    // 本页不足一页，或已取满 total，说明已经到底
    if (list.length < PAGE_SIZE || (total > 0 && all.length >= total)) break
  }
  return all
}

export default {
  name: 'Chapters',
  components: { ReaderLayout },
  setup() {
    const route = useRoute()
    const book = ref({})
    const chapters = ref([])
    onMounted(async () => {
      const [bookResult, allChapters] = await Promise.all([
        getBook(route.params.bookId),
        loadAllChapters(route.params.bookId),
      ])
      book.value = bookResult.data || {}
      chapters.value = allChapters
    })
    return { route, book, chapters }
  },
}
</script>
