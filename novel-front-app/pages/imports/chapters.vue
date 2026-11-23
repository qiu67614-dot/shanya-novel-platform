<template>
  <view class="screen"><text class="eyebrow">私人书库</text><text class="page-title">{{ novel.bookName || '章节目录' }}</text><text class="muted">{{ total }} 章 · {{ novel.encoding || '' }}</text>
    <GuestGate v-if="!authenticated" title="私人目录" :redirect="`/pages/imports/chapters?id=${id}`" />
    <template v-else>
    <view v-if="loading && !chapters.length" class="state">正在加载目录…</view><view v-else-if="error" class="state state-error" @click="load">{{ error }} · 点击重试</view><view v-else-if="!chapters.length" class="state">暂无章节。</view>
    <view v-for="item in chapters" :key="item.id" class="chapter-item" @click="open(item)"><text class="chapter-number">{{ item.chapterNum }}</text><text class="chapter-name">{{ item.chapterName }}</text><text class="muted">›</text></view>
    <button v-if="chapters.length < total" class="quiet-btn more" :disabled="loading" @click="more">加载更多章节</button>
    </template>
  </view>
</template>
<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { getToken } from '../../utils/request.js'
import GuestGate from '../../components/GuestGate.vue'
const id = ref(''), novel = ref({}), chapters = ref([]), total = ref(0), page = ref(1), loading = ref(false), error = ref('')
const authenticated = ref(false)
async function load() { loading.value = true; error.value = ''; try { const data = await api.importChapters(id.value, { page: page.value, size: 100 }); chapters.value = page.value === 1 ? data.list || [] : [...chapters.value, ...(data.list || [])]; total.value = data.total || 0 } catch (reason) { error.value = reason.message; authenticated.value = Boolean(getToken()) } finally { loading.value = false } }
const more = () => { page.value++; load() }
const open = (item) => uni.navigateTo({ url: `/pages/read/index?bookId=${id.value}&chapterId=${item.id}&private=1` })
onLoad(async (options) => { id.value = String(options.id || ''); authenticated.value = Boolean(getToken()); if (!authenticated.value) return; try { novel.value = await api.import(id.value) || {} } catch (reason) { error.value = reason.message; authenticated.value = Boolean(getToken()); return } load() })
</script>
<style scoped>.chapter-item { display: flex; align-items: center; gap: 20rpx; min-height: 104rpx; padding: 16rpx 0; border-bottom: 1rpx solid #dce3dd; }.chapter-number { flex: 0 0 56rpx; color: #ae4938; font-family: Georgia, serif; font-size: 23rpx; font-weight: 700; text-align: center; }.chapter-name { flex: 1; min-width: 0; font-size: 27rpx; line-height: 1.5; }.more { width: 100%; margin-top: 30rpx; }</style>
