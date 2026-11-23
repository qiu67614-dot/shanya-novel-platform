<template>
  <view class="screen"><text class="eyebrow">公开投稿</text><text class="page-title">我的公开</text>
    <GuestGate v-if="!authenticated" title="公开投稿" redirect="/pages/imports/publications" />
    <template v-else>
    <view v-if="loading" class="state">正在加载投稿…</view><view v-else-if="error" class="state state-error" @click="load">{{ error }} · 点击重试</view><view v-else-if="!items.length" class="state">还没有公开作品。</view>
    <view v-for="item in items" :key="item.publicBookId || item.id" class="publication"><text class="publication-title">{{ item.bookName }}</text><text class="muted">{{ item.authorName || '佚名' }} · {{ item.publicStatus === 'ACTIVE' ? '已公开' : '已下架' }}</text><view class="publication-actions"><text v-if="item.publicBookId" @click="open(item)">查看公开页</text><text v-if="item.publicBookId && item.publicStatus === 'ACTIVE'" @click="withdraw(item)">申请撤回</text></view></view>
    </template>
  </view>
</template>
<script setup>
import { ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { getToken, showError } from '../../utils/request.js'
import GuestGate from '../../components/GuestGate.vue'
const items = ref([]), loading = ref(false), error = ref('')
const authenticated = ref(false)
async function load() { authenticated.value = Boolean(getToken()); if (!authenticated.value) { uni.stopPullDownRefresh(); return } loading.value = true; error.value = ''; try { items.value = await api.publications() || [] } catch (reason) { error.value = reason.message; authenticated.value = Boolean(getToken()) } finally { loading.value = false; uni.stopPullDownRefresh() } }
const open = (item) => uni.navigateTo({ url: `/pages/book/detail?id=${item.publicBookId}` })
function withdraw(item) { uni.showModal({ title: '申请撤回', content: `申请将《${item.bookName}》从公开书库撤回？`, editable: true, placeholderText: '撤回原因（可选）', success: async ({ confirm, content }) => { if (!confirm) return; try { await api.withdraw(item.publicBookId, content || ''); uni.showToast({ title: '申请已提交', icon: 'none' }); load() } catch (reason) { showError(reason) } } }) }
onLoad(load); onPullDownRefresh(load)
</script>
<style scoped>.publication { padding: 30rpx 0; border-bottom: 1rpx solid #dce3dd; }.publication-title { display: block; margin-bottom: 10rpx; font-family: "Songti SC", serif; font-size: 32rpx; font-weight: 700; }.publication-actions { display: flex; gap: 30rpx; margin-top: 20rpx; color: #ae4938; font-size: 24rpx; }</style>
