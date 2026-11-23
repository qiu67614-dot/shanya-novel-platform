<template>
  <view class="screen">
    <text class="eyebrow">书单</text><text class="page-title">榜单</text>
    <view class="chip-row"><view v-for="item in types" :key="item.value" class="chip" :class="{ active: type === item.value }" @click="select(item.value)">{{ item.label }}</view></view>
    <view v-if="loading" class="state">正在加载榜单…</view><view v-else-if="error" class="state state-error" @click="load">{{ error }} · 点击重试</view><view v-else-if="!books.length" class="state">暂无作品。</view>
    <view v-else><view v-for="(book, index) in books" :key="book.id" class="rank-row"><text class="rank-number">{{ index + 1 }}</text><view class="rank-book"><BookRow :book="book" /></view></view></view>
    <BottomNav active="books" />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import BookRow from '../../components/BookRow.vue'
import BottomNav from '../../components/BottomNav.vue'
const types = [{ value: 'visit', label: '人气榜' }, { value: 'update', label: '更新榜' }, { value: 'new', label: '新书榜' }]
const type = ref('visit'), books = ref([]), loading = ref(false), error = ref('')
async function load() {
  loading.value = true; error.value = ''
  try { books.value = await api.rank(type.value) || [] } catch (reason) { error.value = reason.message }
  finally { loading.value = false }
}
const select = (value) => { type.value = value; load() }
onLoad((options) => { type.value = types.some((item) => item.value === options.type) ? options.type : 'visit'; load() })
</script>

<style scoped>.rank-row { display: flex; align-items: flex-start; gap: 18rpx; }.rank-number { flex: 0 0 45rpx; padding-top: 45rpx; color: #ae4938; font-family: Georgia, serif; font-size: 31rpx; font-weight: 700; text-align: center; }.rank-row:nth-of-type(n+4) .rank-number { color: #77877c; }.rank-book { flex: 1; min-width: 0; }</style>
