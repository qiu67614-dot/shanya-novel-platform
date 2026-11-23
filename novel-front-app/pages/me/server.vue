<template>
  <view class="screen"><text class="eyebrow">连接设置</text><text class="page-title">服务器地址</text>
    <text class="muted note">Android 模拟器默认使用 10.0.2.2。真机请填写电脑的局域网 IP 和后端端口。</text>
    <view class="field"><text class="field-label">后端地址</text><input v-model.trim="address" class="field-input" placeholder="http://192.168.1.10:8080" /></view>
    <button class="primary-btn save" @click="save">保存</button>
    <text class="muted note">H5 开发环境固定通过 /api 代理，修改此地址只影响 App。</text>
  </view>
</template>
<script setup>
import { ref } from 'vue'
import { getServer, setServer, showError } from '../../utils/request.js'
const address = ref(getServer())
const save = () => { if (!/^https?:\/\/[^\s/]+(?::\d+)?(?:\/[^\s]*)?$/i.test(address.value)) return showError(new Error('请输入完整的 http 或 https 地址')); setServer(address.value); uni.showToast({ title: '已保存', icon: 'none' }) }
</script>
<style scoped>.note { display: block; max-width: 40em; margin: 0 0 35rpx; line-height: 1.7; }.save { width: 100%; margin: 10rpx 0 35rpx; }</style>
