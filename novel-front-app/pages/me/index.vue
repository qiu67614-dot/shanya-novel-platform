<template>
  <view class="screen"><text class="eyebrow">读者空间</text><text class="page-title">我的</text>
    <view v-if="token" class="identity" @click="open('/pages/me/profile')"><view class="avatar">{{ (user.nickName || user.username || '书').slice(0, 1) }}</view><view><text class="identity-name">{{ user.nickName || user.username || '书友' }}</text><text class="muted">查看和编辑个人资料 ›</text></view></view>
    <view v-else class="identity" @click="open('/pages/auth/login')"><view class="avatar">书</view><view><text class="identity-name">登录后继续阅读</text><text class="muted">同步进度、书架与私人作品 ›</text></view></view>
    <view class="menu"><view @click="open('/pages/bookshelf/index')">我的书架 <text>›</text></view><view @click="open('/pages/history/index')">阅读历史 <text>›</text></view><view @click="open('/pages/following/index')">关注更新 <text>›</text></view><view @click="open('/pages/imports/index')">我的导入 <text>›</text></view><view @click="open('/pages/imports/publications')">我的公开 <text>›</text></view></view>
    <view class="menu"><view @click="open('/pages/me/server')">服务器地址 <text>›</text></view></view>
    <button v-if="token" class="quiet-btn logout" @click="logout">退出登录</button>
    <BottomNav active="me" />
  </view>
</template>
<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { clearSession, getToken, getUser, saveUser } from '../../utils/request.js'
import BottomNav from '../../components/BottomNav.vue'
const token = ref(''), user = ref({})
const open = (url) => uni.navigateTo({ url })
onShow(async () => { token.value = getToken(); user.value = getUser() || {}; if (token.value) { try { user.value = await api.me() || {}; saveUser(user.value) } catch {} } })
const logout = () => { clearSession(); token.value = ''; user.value = {}; uni.showToast({ title: '已退出', icon: 'none' }) }
</script>
<style scoped>.identity { display: flex; align-items: center; gap: 25rpx; min-height: 168rpx; margin-bottom: 36rpx; padding: 20rpx 0 32rpx; border-bottom: 1rpx solid #dce3dd; }.avatar { display: flex; align-items: center; justify-content: center; width: 106rpx; height: 106rpx; border-radius: 53rpx; background: #37614e; color: #fff; font-family: "Songti SC", serif; font-size: 42rpx; }.identity-name { display: block; margin-bottom: 8rpx; font-family: "Songti SC", serif; font-size: 34rpx; font-weight: 700; }.menu { margin: 20rpx 0 36rpx; border-top: 1rpx solid #dce3dd; }.menu view { display: flex; justify-content: space-between; align-items: center; min-height: 98rpx; padding: 0 5rpx; border-bottom: 1rpx solid #dce3dd; }.menu text { color: #88968c; font-family: Georgia, serif; font-size: 33rpx; }.logout { width: 100%; margin-top: 30rpx; }</style>
