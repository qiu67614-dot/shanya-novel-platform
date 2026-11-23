<template>
  <view class="book-row" @click="open">
    <BookCover :title="book.bookName" :pic-url="book.picUrl" :tone="tone" />
    <view class="book-row__body">
      <text class="book-row__title">{{ book.bookName || '未命名作品' }}</text>
      <text class="book-row__meta">{{ book.authorName || '佚名' }} · {{ book.categoryName || '小说' }} · {{ book.bookStatus === 1 ? '完结' : '连载' }}</text>
      <text v-if="note" class="row-trail">{{ note }}</text>
      <text v-else-if="book.bookDesc" class="book-row__desc">{{ book.bookDesc }}</text>
    </view>
  </view>
</template>

<script setup>
import BookCover from './BookCover.vue'
const props = defineProps({ book: { type: Object, required: true }, note: { type: String, default: '' }, interactive: { type: Boolean, default: true } })
const tone = Number(props.book.id || 0) % 3 === 0 ? 'gold' : Number(props.book.id || 0) % 2 ? '' : 'alt'
const open = () => { if (props.interactive) uni.navigateTo({ url: `/pages/book/detail?id=${props.book.id}` }) }
</script>
