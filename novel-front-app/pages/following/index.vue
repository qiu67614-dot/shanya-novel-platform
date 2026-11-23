<template>
  <view class="screen"><text class="eyebrow">追更</text><text class="page-title">关注的书</text>
    <GuestGate v-if="!authenticated" title="关注的书" redirect="/pages/following/index" />
    <template v-else>
    <view v-if="loading" class="state">正在加载关注…</view><view v-else-if="error" class="state state-error" @click="load">{{ error }} · 点击重试</view><view v-else-if="!items.length" class="state">关注作品后，新章节会显示在这里。</view>
    <view v-for="item in items" :key="item.book.id" class="entry"><BookRow :book="item.book" :note="item.book.lastChapterName || ''" /><view class="entry-actions"><text v-if="item.hasUpdate" class="badge">有更新</text><text @click="open(item)">阅读更新 ›</text><text v-if="item.hasUpdate" @click="mark(item)">标记已读</text><text @click="remove(item)">取消关注</text></view></view>
    </template>
  </view>
</template>
<script setup>
import { ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { getToken, showError } from '../../utils/request.js'
import BookRow from '../../components/BookRow.vue'
import GuestGate from '../../components/GuestGate.vue'
const items = ref([]), loading = ref(false), error = ref('')
const authenticated = ref(false)
async function load() { authenticated.value = Boolean(getToken()); if (!authenticated.value) { uni.stopPullDownRefresh(); return } loading.value = true; error.value = ''; try { items.value = await api.following() || [] } catch (reason) { error.value = reason.message; authenticated.value = Boolean(getToken()) } finally { loading.value = false; uni.stopPullDownRefresh() } }
const open = (item) => uni.navigateTo({ url: item.book.lastChapterId ? `/pages/read/index?bookId=${item.book.id}&chapterId=${item.book.lastChapterId}` : `/pages/book/detail?id=${item.book.id}` })
async function mark(item) { try { await api.markRead(item.book.id); item.hasUpdate = false } catch (reason) { showError(reason) } }
async function remove(item) { try { await api.unfollow(item.book.id); items.value = items.value.filter((row) => row.book.id !== item.book.id) } catch (reason) { showError(reason) } }
onLoad(load); onPullDownRefresh(load)
</script>
<style scoped>.entry { border-bottom: 1rpx solid #dce3dd; }.entry :deep(.book-row) { border-bottom: 0; }.entry-actions { display: flex; flex-wrap: wrap; gap: 20rpx; align-items: center; min-height: 62rpx; padding: 0 0 20rpx 150rpx; color: #ae4938; font-size: 23rpx; }.entry-actions text:last-child { color: #6b786d; }</style>
