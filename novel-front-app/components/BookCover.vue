<template>
  <view class="book-cover" :class="[tone, { 'book-cover--image': coverUrl && !failed }]">
    <image v-if="coverUrl && !failed" class="book-cover__image" :src="coverUrl" mode="aspectFill" @error="failed = true" />
    <text v-else class="book-cover__title">{{ title || '未命名作品' }}</text>
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { getServer } from '../utils/request.js'
const props = defineProps({ title: { type: String, default: '' }, picUrl: { type: String, default: '' }, tone: { type: String, default: '' } })
const failed = ref(false)
watch(() => props.picUrl, () => { failed.value = false })
const coverUrl = computed(() => {
  if (!props.picUrl) return ''
  if (/^https?:\/\//i.test(props.picUrl)) return props.picUrl
  // #ifdef H5
  return `/api/${props.picUrl.replace(/^\/+/, '')}`
  // #endif
  // #ifndef H5
  return `${getServer()}/${props.picUrl.replace(/^\/+/, '')}`
  // #endif
})
</script>

<style scoped>
.book-cover__image { position: absolute; inset: 0; width: 100%; height: 100%; }
.book-cover--image::after { display: none; }
</style>
