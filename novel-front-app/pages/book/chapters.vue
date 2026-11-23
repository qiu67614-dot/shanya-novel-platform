<template>
  <view class="screen">
    <text class="eyebrow">{{ privateMode ? '私人书库' : '公开作品' }}</text><text class="page-title">{{ title || '章节目录' }}</text>
    <GuestGate v-if="privateMode && !authenticated" title="私人目录" :redirect="`/pages/book/chapters?id=${id}&private=1`" />
    <template v-else>
    <text class="muted">共 {{ total }} 章</text>
    <view v-if="loading && !chapters.length" class="state">正在加载目录…</view><view v-else-if="error" class="state state-error" @click="load">{{ error }} · 点击重试</view><view v-else-if="!chapters.length" class="state">暂无章节。</view>
    <view v-for="item in chapters" :key="item.id" class="chapter-item" @click="open(item)"><text class="chapter-number">{{ item.chapterNum }}</text><text class="chapter-name">{{ item.chapterName }}</text><text class="muted">›</text></view>
    <button v-if="chapters.length < total" class="load-more" :disabled="loading" @click="nextPage">{{ loading ? '加载中…' : '加载更多章节' }}</button>
    </template>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { getToken } from '../../utils/request.js'
import GuestGate from '../../components/GuestGate.vue'
const id = ref(''), title = ref(''), privateMode = ref(false), chapters = ref([]), total = ref(0), page = ref(1), loading = ref(false), error = ref('')
const authenticated = ref(false)
async function load() {
  loading.value = true; error.value = ''
  try {
    const data = privateMode.value ? await api.importChapters(id.value, { page: page.value, size: 100 }) : await api.chapters(id.value, { page: page.value, size: 100 })
    chapters.value = page.value === 1 ? data.list || [] : [...chapters.value, ...(data.list || [])]
    total.value = data.total || 0
  } catch (reason) { error.value = reason.message || '目录加载失败'; authenticated.value = Boolean(getToken()) }
  finally { loading.value = false }
}
const nextPage = () => { page.value++; load() }
const open = (item) => uni.navigateTo({ url: `/pages/read/index?bookId=${id.value}&chapterId=${item.id}${privateMode.value ? '&private=1' : ''}` })
onLoad(async (options) => {
  id.value = String(options.id || ''); privateMode.value = options.private === '1'
  authenticated.value = Boolean(getToken())
  if (privateMode.value && !authenticated.value) return
  try { title.value = (privateMode.value ? await api.import(id.value) : await api.book(id.value)).bookName || '章节目录' } catch { title.value = '章节目录' }
  load()
})
</script>

<style scoped>.chapter-item { display: flex; align-items: center; gap: 20rpx; min-height: 104rpx; padding: 16rpx 0; border-bottom: 1rpx solid #dce3dd; }.chapter-number { flex: 0 0 56rpx; color: #ae4938; font-family: Georgia, serif; font-size: 23rpx; font-weight: 700; text-align: center; }.chapter-name { flex: 1; min-width: 0; font-size: 27rpx; line-height: 1.5; }.load-more { width: 100%; min-height: 84rpx; margin: 34rpx auto; border: 1rpx solid #dce3dd; background: #fff; color: #37614e; font-size: 25rpx; }</style>
