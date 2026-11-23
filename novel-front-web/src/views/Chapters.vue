<template><ReaderLayout><section class="page-heading"><div><p class="eyebrow">章节目录</p><h1>{{ book.bookName || '作品' }}</h1></div><router-link class="text-link" :to="`/book/${route.params.bookId}`">返回详情</router-link></section><div class="chapter-list"><router-link v-for="chapter in chapters" :key="chapter.id" :to="`/read/${route.params.bookId}/${chapter.id}`"><span>第{{ chapter.chapterNum }}章</span>{{ chapter.chapterName }}<small>{{ chapter.wordCount || 0 }} 字</small></router-link><div v-if="!chapters.length" class="empty-state">暂无章节</div></div></ReaderLayout></template>
<script>
import { onMounted, ref } from 'vue'; import { useRoute } from 'vue-router'; import ReaderLayout from '@/components/common/ReaderLayout.vue'; import { getBook, getChapters } from '@/api/books'
export default { name: 'Chapters', components: { ReaderLayout }, setup() { const route = useRoute(); const book = ref({}); const chapters = ref([]); onMounted(async () => { const [bookResult, chapterResult] = await Promise.all([getBook(route.params.bookId), getChapters(route.params.bookId, { page: 1, size: 10000 })]); book.value = bookResult.data || {}; chapters.value = chapterResult.data?.list || chapterResult.data || [] }); return { route, book, chapters } } }
</script>



