<template>
  <view class="guest-gate">
    <text class="guest-gate__title">登录后查看{{ title }}</text>
    <text class="guest-gate__copy">游客可以继续浏览书库、阅读公开章节。</text>
    <view class="guest-gate__actions">
      <button class="primary-btn" @click="login">去登录</button>
      <button class="quiet-btn" @click="browse">游客阅读</button>
    </view>
  </view>
</template>

<script setup>
import { setEntryChoice } from '../utils/request.js'

const props = defineProps({
  title: { type: String, required: true },
  redirect: { type: String, default: '' },
})

const login = () => uni.navigateTo({ url: `/pages/auth/login${props.redirect ? `?redirect=${encodeURIComponent(props.redirect)}` : ''}` })
const browse = () => {
  setEntryChoice('guest')
  uni.reLaunch({ url: '/pages/index/index' })
}
</script>

<style scoped>
.guest-gate { padding: 85rpx 0 40rpx; border-top: 1rpx solid #dce3dd; text-align: center; }
.guest-gate__title { display: block; color: #27342d; font-family: "Songti SC", serif; font-size: 36rpx; font-weight: 700; }
.guest-gate__copy { display: block; margin: 20rpx 0 35rpx; color: #66736e; font-size: 25rpx; line-height: 1.6; }
.guest-gate__actions { display: flex; justify-content: center; gap: 14rpx; }
.guest-gate__actions button { flex: 1; max-width: 260rpx; }
</style>
