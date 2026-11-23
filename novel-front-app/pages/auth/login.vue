<template>
  <view class="screen auth-screen">
    <text class="eyebrow">山崖书院 · 读者账户</text><text class="page-title">欢迎回来</text>
    <text class="muted intro">登录后可同步书架、历史与私人作品。</text>
    <view class="field"><text class="field-label">用户名</text><input v-model.trim="username" class="field-input" placeholder="请输入用户名" /></view>
    <view class="field"><text class="field-label">密码</text><input v-model="password" class="field-input" type="password" placeholder="请输入密码" /></view>
    <button class="primary-btn full" :disabled="busy || !username || !password" @click="submit">{{ busy ? '登录中…' : '登录' }}</button>
    <view class="auth-links"><text @click="register">注册账户</text><text @click="browseAsGuest">游客阅读</text></view>
  </view>
</template>
<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { saveSession, setEntryChoice, showError } from '../../utils/request.js'
const username = ref(''), password = ref(''), busy = ref(false), redirect = ref('')
onLoad((options) => { redirect.value = options.redirect ? decodeURIComponent(options.redirect) : '' })
const register = () => uni.navigateTo({ url: `/pages/auth/register${redirect.value ? `?redirect=${encodeURIComponent(redirect.value)}` : ''}` })
const browseAsGuest = () => { setEntryChoice('guest'); uni.reLaunch({ url: '/pages/index/index' }) }
async function submit() {
  if (busy.value) return
  busy.value = true
  try {
    const result = await api.login({ username: username.value, password: password.value })
    if (!result?.token) throw new Error('登录响应中没有 token')
    saveSession(result)
    uni.showToast({ title: '登录成功', icon: 'none' })
    uni.redirectTo({ url: redirect.value || '/pages/me/index' })
  } catch (error) { showError(error) }
  finally { busy.value = false }
}
</script>
<style scoped>.auth-screen { max-width: 620px; padding-top: 90rpx; }.auth-screen .page-title { margin-bottom: 20rpx; }.intro { display: block; margin: 0 0 68rpx; line-height: 1.6; }.full { width: 100%; }.auth-links { display: flex; justify-content: space-between; gap: 24rpx; margin-top: 36rpx; color: #ae4938; font-size: 25rpx; }</style>
