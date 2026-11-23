<template>
  <view class="screen home-screen">
    <view class="home-head"><text class="eyebrow">THE READING ROOM</text><text class="home-name">山崖书院</text><text class="home-line">翻开一页，山色入窗。</text></view>

    <view class="home-scene" :class="{ 'home-scene--moving': active && !paused && !motionDisabled }">
      <image class="home-scene__layer home-scene__back" src="/static/home/study-back.png" mode="aspectFill" />
      <image class="home-scene__layer home-scene__light" src="/static/home/study-light.png" mode="aspectFill" />
      <image class="home-scene__layer home-scene__front" src="/static/home/study-front.png" mode="aspectFill" />
      <image class="home-scene__layer home-scene__page" src="/static/home/study-page.png" mode="aspectFill" />
      <button v-if="!motionDisabled" class="motion-control" :aria-label="paused ? '播放首页画面' : '暂停首页画面'" :title="paused ? '播放画面' : '暂停画面'" @click.stop="toggleMotion">{{ paused ? '▶' : 'Ⅱ' }}</button>
    </view>

    <view v-if="featured" class="featured" @click="open(`/pages/book/detail?id=${featured.id}`)">
      <view class="featured__label"><text>今日热门</text><text class="featured__rule" /></view>
      <view class="featured__main"><view class="featured__copy"><text class="featured__title">{{ featured.bookName }}</text><text class="featured__meta">{{ featured.authorName || '佚名' }} · {{ featured.categoryName || '小说' }}</text></view><text class="featured__arrow">↗</text></view>
    </view>
    <view v-else class="featured featured--empty" @click="open('/pages/books/index')"><text>找一本书，开始阅读</text><text class="featured__arrow">↗</text></view>

    <view class="search-entry" @click="open('/pages/books/index')"><text>搜索书名或作者</text><text class="search-entry__icon">⌕</text></view>
    <view v-if="loading" class="state">正在加载书库…</view>
    <view v-else-if="error" class="state state-error" @click="load">{{ error }} · 点击重试</view>
    <template v-else>
      <view class="section-head"><text class="section-title">最近上新</text><text class="link-text" @click="open('/pages/books/index?sort=new')">全部作品　↗</text></view>
      <BookRow v-for="book in latest.slice(0, 4)" :key="book.id" :book="book" />
      <view class="section-head"><text class="section-title">热门作品</text><text class="link-text" @click="open('/pages/rank/index?type=visit')">查看榜单　↗</text></view>
      <BookRow v-for="book in hot.slice(1, 5)" :key="book.id" :book="book" />
      <view class="section-head"><text class="section-title">最近更新</text><text class="link-text" @click="open('/pages/rank/index?type=update')">更新榜　↗</text></view>
      <BookRow v-for="book in updated.slice(0, 4)" :key="book.id" :book="book" :note="book.lastChapterName || ''" />
    </template>
    <BottomNav active="home" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onHide, onPullDownRefresh, onShow, onUnload } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { getEntryChoice, getToken, setEntryChoice } from '../../utils/request.js'
import BookRow from '../../components/BookRow.vue'
import BottomNav from '../../components/BottomNav.vue'

const latest = ref([]), hot = ref([]), updated = ref([])
const loading = ref(false), error = ref(''), active = ref(false)
const paused = ref(Boolean(uni.getStorageSync('novel_home_motion_paused')))
const deviceBenchmark = Number(uni.getSystemInfoSync().benchmarkLevel || 0)
const motionDisabled = ref(deviceBenchmark > 0 && deviceBenchmark < 15)
const featured = computed(() => hot.value[0] || null)
const open = (url) => uni.navigateTo({ url })
const toggleMotion = () => { paused.value = !paused.value; uni.setStorageSync('novel_home_motion_paused', paused.value) }

async function load() {
  loading.value = true; error.value = ''
  try {
    const data = await api.home()
    latest.value = data.latest || []; hot.value = data.hot || []; updated.value = data.updated || []
  } catch (reason) { error.value = reason.message || '首页加载失败' }
  finally { loading.value = false; uni.stopPullDownRefresh() }
}

function offerEntryChoice() {
  if (getToken() || getEntryChoice()) return
  setEntryChoice('prompted')
  uni.showModal({
    title: '欢迎来到山崖书院',
    content: '登录可同步书架和阅读进度，也可以直接以游客身份阅读公开作品。',
    confirmText: '去登录',
    cancelText: '游客阅读',
    success: ({ confirm }) => {
      if (confirm) uni.navigateTo({ url: '/pages/auth/login' })
      else setEntryChoice('guest')
    },
  })
}

onShow(() => {
  active.value = true
  load()
  // #ifdef APP-PLUS
  offerEntryChoice()
  // #endif
})
onHide(() => { active.value = false })
onUnload(() => { active.value = false })
onPullDownRefresh(load)
</script>

<style scoped>
.home-screen { padding-top: 30rpx; }
.home-head { padding: 0 0 24rpx; }
.home-name { display: block; margin: 2rpx 0 4rpx; font-family: "Noto Serif CJK SC", "Songti SC", "SimSun", serif; font-size: 56rpx; font-weight: 800; line-height: 1.25; }
.home-line { color: #53615b; font-size: 24rpx; }
.home-scene { position: relative; width: 100%; height: clamp(245px, 52vw, 410px); overflow: hidden; background: #cbd8cf; }
.home-scene__layer { position: absolute; inset: 0; width: 100%; height: 100%; pointer-events: none; }
.home-scene__light { opacity: .52; }
.home-scene__page { transform-origin: 50.5% 77%; }
.home-scene--moving .home-scene__back { animation: scene-drift 16s ease-in-out infinite alternate; }
.home-scene--moving .home-scene__light { animation: scene-light 7s ease-in-out infinite alternate; }
.home-scene--moving .home-scene__page { animation: scene-page 5.5s ease-in-out infinite alternate; }
.motion-control { position: absolute; top: 16rpx; right: 16rpx; display: flex; align-items: center; justify-content: center; width: 78rpx; height: 78rpx; padding: 0; border: 1rpx solid rgba(255,255,255,.72); border-radius: 50%; background: rgba(30,43,37,.78); color: #fff; font-size: 29rpx; line-height: 1; }
.featured { display: flex; flex-direction: column; min-height: 142rpx; padding: 22rpx 25rpx 24rpx; background: #293d35; color: #fff; }
.featured__label { display: flex; align-items: center; gap: 16rpx; color: #d0d9d0; font-size: 20rpx; }
.featured__rule { flex: 1; height: 1rpx; background: #667a6d; }
.featured__main { display: flex; align-items: center; justify-content: space-between; gap: 18rpx; margin-top: 6rpx; }
.featured__copy { flex: 1; min-width: 0; }
.featured__title { display: block; overflow: hidden; font-family: "Noto Serif CJK SC", "Songti SC", "SimSun", serif; font-size: 33rpx; font-weight: 700; line-height: 1.3; text-overflow: ellipsis; white-space: nowrap; }
.featured__meta { display: block; margin-top: 5rpx; color: #d1d8d1; font-size: 21rpx; }
.featured__arrow { flex: none; font-family: Georgia, serif; font-size: 38rpx; }
.featured--empty { flex-direction: row; align-items: center; justify-content: space-between; font-size: 27rpx; }
.search-entry { display: flex; align-items: center; justify-content: space-between; min-height: 89rpx; margin-top: 18rpx; padding: 0 24rpx; border: 1rpx solid #dce3dd; background: #fff; color: #68766d; font-size: 25rpx; }
.search-entry__icon { color: #ae4938; font-family: Georgia, serif; font-size: 48rpx; line-height: 1; }
.home-screen .section-head:first-of-type { margin-top: 40rpx; }
@keyframes scene-drift { from { transform: scale(1.01) translateX(-.35%); } to { transform: scale(1.035) translateX(.35%); } }
@keyframes scene-light { from { opacity: .24; } to { opacity: .62; } }
@keyframes scene-page { from { transform: rotate(0deg) translateY(0); } to { transform: rotate(-1.2deg) translateY(-3px); } }
@media (max-width: 480px) { .home-scene { height: 246px; } .home-name { font-size: 49rpx; } }
@media (max-width: 360px), (max-height: 680px) { .home-scene { height: 198px; } }
@media (prefers-reduced-motion: reduce) { .home-scene--moving .home-scene__layer { animation: none; } }
</style>
