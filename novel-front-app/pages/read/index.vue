<template>
  <view class="screen reading" :class="dark ? 'reader-dark' : 'reader-light'">
    <view class="reader-top"><text class="reader-top__book" @click="backToBook">‹　{{ data.book?.bookName || '返回作品' }}</text><text class="reader-top__catalog" @click="catalog">目录　☷</text></view>
    <GuestGate v-if="privateMode && !authenticated" title="私人作品" :redirect="`/pages/read/index?bookId=${bookId}&chapterId=${chapterId}&private=1`" />
    <view v-else-if="loading" class="state">正在加载正文…</view>
    <view v-else-if="error" class="state state-error" @click="load">{{ error }} · 点击重试</view>
    <view v-else class="reader-stage" @click="toggleTools">
      <text class="reader-kicker">{{ privateMode ? '私人阅读' : '山崖书院' }} · 第 {{ data.chapter?.chapterNum || '—' }} 章</text>
      <text class="reader-title">{{ data.chapter?.chapterName || '章节不存在' }}</text>
      <text class="reader-meta">{{ data.book?.bookName || '' }}　/　{{ data.chapter?.wordCount || 0 }} 字</text>
      <text class="reader-content" :style="{ fontSize: `${fontSize}rpx` }">{{ data.content || '暂无正文内容' }}</text>
      <view class="reader-end"><text>本章完</text><view /></view>
      <view class="chapter-actions" @click.stop><button :disabled="!data.previousChapterId || loading" @click="go(data.previousChapterId)">‹　上一章</button><button @click="catalog">目录</button><button :disabled="!data.nextChapterId || loading" @click="go(data.nextChapterId)">下一章　›</button></view>
    </view>
    <view v-if="toolsVisible && (!privateMode || authenticated)" class="reader-tools"><button aria-label="缩小字号" :disabled="fontSize <= 28" @click="changeFont(-2)">A−</button><text class="reader-tools__value">{{ fontSize }}</text><button aria-label="放大字号" :disabled="fontSize >= 46" @click="changeFont(2)">A+</button><view class="reader-tools__divider" /><button :aria-label="dark ? '切换日间模式' : '切换夜间模式'" @click="toggleTheme">{{ dark ? '日间' : '夜间' }}</button></view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { getToken } from '../../utils/request.js'
import GuestGate from '../../components/GuestGate.vue'

const bookId = ref(''), chapterId = ref(''), privateMode = ref(false)
const data = ref({}), loading = ref(false), error = ref('')
const fontSize = ref(uni.getStorageSync('novel_app_font_size') || 34)
const dark = ref(Boolean(uni.getStorageSync('novel_app_dark')))
const toolsVisible = ref(true)
const authenticated = ref(false)

async function load() {
  loading.value = true; error.value = ''; data.value = {}
  try {
    data.value = privateMode.value ? await api.importChapter(bookId.value, chapterId.value) : await api.chapter(chapterId.value)
    if (!privateMode.value && data.value.chapter?.bookId && String(data.value.chapter.bookId) !== bookId.value) throw new Error('章节不属于当前作品')
    if (privateMode.value) api.importProgress(bookId.value, data.value.chapter.id).catch(() => {})
    else if (getToken() && data.value.chapter?.id) api.progress({ bookId: bookId.value, chapterId: data.value.chapter.id }).catch(() => {})
    uni.pageScrollTo({ scrollTop: 0, duration: 0 })
  } catch (reason) { error.value = reason.message || '章节加载失败'; authenticated.value = Boolean(getToken()) }
  finally { loading.value = false }
}
const catalog = () => uni.navigateTo({ url: privateMode.value ? `/pages/imports/chapters?id=${bookId.value}` : `/pages/book/chapters?id=${bookId.value}` })
const backToBook = () => privateMode.value ? uni.navigateTo({ url: '/pages/imports/index' }) : uni.navigateTo({ url: `/pages/book/detail?id=${bookId.value}` })
const go = (id) => { if (!id) return; uni.redirectTo({ url: `/pages/read/index?bookId=${bookId.value}&chapterId=${id}${privateMode.value ? '&private=1' : ''}` }) }
const toggleTools = () => { toolsVisible.value = !toolsVisible.value }
const changeFont = (delta) => { fontSize.value = Math.min(46, Math.max(28, Number(fontSize.value) + delta)); uni.setStorageSync('novel_app_font_size', fontSize.value) }
const toggleTheme = () => { dark.value = !dark.value; uni.setStorageSync('novel_app_dark', dark.value) }
onLoad((options) => { bookId.value = String(options.bookId || ''); chapterId.value = String(options.chapterId || ''); privateMode.value = options.private === '1'; authenticated.value = Boolean(getToken()); if (privateMode.value && !authenticated.value) return; load() })
</script>

<style scoped>
.reader-light { background: #f7f7f2; color: #28312b; }
.reader-dark { background: #202925; color: #dce1d8; }
.reader-top { display: flex; align-items: center; justify-content: space-between; gap: 22rpx; min-height: 72rpx; margin-bottom: 54rpx; padding-bottom: 16rpx; border-bottom: 1rpx solid rgba(93,111,98,.26); color: #a04a39; font-size: 24rpx; }
.reader-dark .reader-top { color: #d4a898; }
.reader-top__book { max-width: 75%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.reader-top__catalog { flex: none; }
.reader-stage { min-height: 70vh; }
.reader-kicker { display: block; color: #8a5a4b; font-size: 21rpx; font-weight: 700; letter-spacing: 1rpx; }
.reader-dark .reader-kicker { color: #c2917e; }
.reader-title { display: block; margin-top: 20rpx; font-family: "Noto Serif CJK SC", "Source Han Serif SC", "Songti SC", "SimSun", serif; font-size: 44rpx; line-height: 1.46; font-weight: 700; word-break: break-word; }
.reader-meta { display: block; margin: 21rpx 0 52rpx; color: #758078; font-size: 23rpx; }
.reader-dark .reader-meta { color: #9ca99f; }
.reader-content { display: block; max-width: 39em; margin: 0 auto; font-family: "Noto Serif CJK SC", "Source Han Serif SC", "Songti SC", "SimSun", serif; line-height: 1.95; white-space: pre-wrap; word-break: break-word; }
.reader-end { display: flex; align-items: center; gap: 24rpx; margin: 75rpx 0 45rpx; color: #8a948a; font-size: 22rpx; }
.reader-end view { flex: 1; height: 1rpx; background: currentColor; opacity: .35; }
.chapter-actions { display: flex; gap: 10rpx; }
.chapter-actions button { flex: 1; min-height: 84rpx; padding: 0 8rpx; border: 1rpx solid #d7ded5; background: #fff; color: #435f4c; font-size: 23rpx; }
.reader-dark .chapter-actions button { border-color: #4e5d53; background: #28352e; color: #d5e0d4; }
.chapter-actions button[disabled] { opacity: .4; }
.reader-tools { position: fixed; z-index: 8; right: 0; bottom: 0; left: 0; display: flex; align-items: center; justify-content: space-evenly; width: 100%; max-width: 800px; min-height: 108rpx; margin: auto; padding: 10rpx 16rpx calc(10rpx + env(safe-area-inset-bottom)); border-top: 1rpx solid #d8dfd8; background: #fff; box-shadow: 0 -4rpx 25rpx rgba(32,42,33,.06); }
.reader-dark .reader-tools { border-color: #46584d; background: #2b382f; }
.reader-tools button { display: inline-flex; align-items: center; justify-content: center; min-width: 88rpx; min-height: 76rpx; padding: 0 14rpx; background: transparent; color: #344c3d; font-size: 27rpx; }
.reader-dark .reader-tools button { color: #d6e0d4; }
.reader-tools button[disabled] { opacity: .42; }
.reader-tools__value { color: #657366; font-size: 22rpx; }
.reader-tools__divider { width: 1rpx; height: 40rpx; background: #dce3dd; }
.reader-dark .reader-tools__divider { background: #596b5d; }
</style>
