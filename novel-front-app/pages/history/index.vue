<template>
  <view class="screen"><text class="eyebrow">个人阅读</text><text class="page-title">阅读历史</text>
    <GuestGate v-if="!authenticated" title="阅读历史" redirect="/pages/history/index" />
    <template v-else>
    <view v-if="loading && !items.length" class="state">正在加载历史…</view><view v-else-if="error" class="state state-error" @click="load">{{ error }} · 点击重试</view><view v-else-if="!items.length" class="state">还没有阅读记录。</view>
    <view v-for="item in items" :key="item.book.id" class="entry" @click="open(item)"><BookRow :book="item.book" :interactive="false" :note="item.chapterName ? `读到 ${item.chapterName}` : '阅读记录'" /><text class="entry-date">{{ format(item.readAt) }} · 继续阅读 ›</text></view>
    <button v-if="items.length < total" class="secondary-btn more" :disabled="loading" @click="more">{{ loading ? '加载中…' : '加载更多' }}</button>
    </template>
  </view>
</template>
<script setup>
import { ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { getToken } from '../../utils/request.js'
import BookRow from '../../components/BookRow.vue'
import GuestGate from '../../components/GuestGate.vue'
const items = ref([]), page = ref(1), total = ref(0), loading = ref(false), error = ref('')
const authenticated = ref(false)
const format = (value) => String(value || '').replace('T', ' ').slice(0, 16)
async function load() {
  authenticated.value = Boolean(getToken())
  if (!authenticated.value) { uni.stopPullDownRefresh(); return }
  loading.value = true; error.value = ''
  try { const data = await api.history({ page: page.value, size: 20 }); items.value = page.value === 1 ? data.list || [] : [...items.value, ...(data.list || [])]; total.value = data.total || 0 }
  catch (reason) { error.value = reason.message; authenticated.value = Boolean(getToken()) }
  finally { loading.value = false; uni.stopPullDownRefresh() }
}
const more = () => { page.value++; load() }
const open = (item) => uni.navigateTo({ url: item.chapterId ? `/pages/read/index?bookId=${item.book.id}&chapterId=${item.chapterId}` : `/pages/book/detail?id=${item.book.id}` })
onLoad(load); onPullDownRefresh(() => { page.value = 1; load() })
</script>
<style scoped>.entry { position: relative; }.entry :deep(.book-row) { padding-bottom: 52rpx; }.entry-date { position: absolute; right: 0; bottom: 16rpx; color: #ae4938; font-size: 22rpx; }.more { width: 100%; margin-top: 30rpx; }</style>
