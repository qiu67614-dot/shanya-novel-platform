<template>
  <view class="screen"><text class="eyebrow">个人阅读</text><text class="page-title">我的书架</text>
    <GuestGate v-if="!authenticated" title="书架" redirect="/pages/bookshelf/index" />
    <template v-else>
    <view v-if="loading" class="state">正在加载书架…</view><view v-else-if="error" class="state state-error" @click="load">{{ error }} · 点击重试</view><view v-else-if="!items.length" class="state">书架还是空的。去书库找一本书吧。</view>
    <view v-for="item in items" :key="item.book.id" class="entry"><BookRow :book="item.book" :note="item.lastChapterName ? `读到 ${item.lastChapterName}` : '尚未开始阅读'" /><view class="entry-actions"><text @click="continueRead(item)">继续阅读 ›</text><text @click="remove(item)">移出书架</text></view></view>
    </template>
    <BottomNav active="shelf" />
  </view>
</template>
<script setup>
import { ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { getToken, showError } from '../../utils/request.js'
import BottomNav from '../../components/BottomNav.vue'
import BookRow from '../../components/BookRow.vue'
import GuestGate from '../../components/GuestGate.vue'
const items = ref([]), loading = ref(false), error = ref('')
const authenticated = ref(false)
async function load() {
  authenticated.value = Boolean(getToken())
  if (!authenticated.value) { uni.stopPullDownRefresh(); return }
  loading.value = true; error.value = ''
  try { items.value = await api.bookshelf() || [] } catch (reason) { error.value = reason.message; authenticated.value = Boolean(getToken()) }
  finally { loading.value = false; uni.stopPullDownRefresh() }
}
async function continueRead(item) {
  let id = item.lastChapterId || item.book.firstChapterId
  try { if (!id) id = (await api.chapters(item.book.id, { page: 1, size: 1 })).list?.[0]?.id }
  catch (reason) { return showError(reason) }
  uni.navigateTo({ url: id ? `/pages/read/index?bookId=${item.book.id}&chapterId=${id}` : `/pages/book/chapters?id=${item.book.id}` })
}
function remove(item) { uni.showModal({ title: '移出书架', content: `移出《${item.book.bookName}》？`, success: async ({ confirm }) => { if (!confirm) return; try { await api.removeShelf(item.book.id); load() } catch (reason) { showError(reason) } } }) }
onShow(load); onPullDownRefresh(load)
</script>
<style scoped>.entry { border-bottom: 1rpx solid #dce3dd; }.entry :deep(.book-row) { border-bottom: 0; }.entry-actions { display: flex; justify-content: space-between; gap: 14rpx; min-height: 60rpx; padding: 0 0 18rpx 150rpx; color: #ae4938; font-size: 24rpx; }.entry-actions text:last-child { color: #6b786d; }</style>
