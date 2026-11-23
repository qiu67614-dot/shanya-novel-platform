<template>
  <ReaderLayout><section class="page-heading"><div><p class="eyebrow">读者选择</p><h1>排行榜</h1></div></section><div class="rank-tabs"><button v-for="tab in tabs" :key="tab.type" :class="{ active: type === tab.type }" @click="type = tab.type; load()">{{ tab.label }}</button></div><section class="rank-list"><article v-for="(book, index) in books" :key="book.id" class="rank-row"><b>{{ String(index + 1).padStart(2, '0') }}</b><BookNameCover :title="book.bookName" :src="book.picUrl" /><div><router-link :to="`/book/${book.id}`">{{ book.bookName }}</router-link><p>{{ book.authorName }} · {{ book.categoryName || '未分类' }}</p><small>{{ book.lastChapterName || '暂无章节' }}</small></div><span>{{ book.visitCount || 0 }} 次阅读</span></article><div v-if="!books.length" class="empty-state">暂无榜单数据</div></section></ReaderLayout>
</template>
<script>
import { onMounted, ref } from 'vue'; import ReaderLayout from '@/components/common/ReaderLayout.vue'; import BookNameCover from '@/components/common/BookNameCover.vue'; import { getRank } from '@/api/books'
export default { name: 'Rank', components: { ReaderLayout, BookNameCover }, setup() { const tabs = [{ type: 'visit', label: '阅读榜' }, { type: 'update', label: '更新榜' }, { type: 'new', label: '新书榜' }]; const type = ref('visit'); const books = ref([]); const load = async () => { const result = await getRank(type.value); books.value = result.data || [] }; onMounted(load); return { tabs, type, books, load } } }
</script>


