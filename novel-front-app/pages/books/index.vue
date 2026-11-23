<template>
  <view class="screen">
    <text class="eyebrow">公开书库</text><text class="page-title">找一本好书</text>
    <view class="search-row"><input v-model.trim="q" class="field-input" placeholder="书名或作者" confirm-type="search" @confirm="search" /><button class="primary-btn" @click="search">搜索</button></view>
    <scroll-view scroll-x class="chip-row"><view class="chip" :class="{ active: !categoryId }" @click="selectCategory('')">全部分类</view><view v-for="category in categories" :key="category.id" class="chip" :class="{ active: String(categoryId) === String(category.id) }" @click="selectCategory(category.id)">{{ category.categoryName }}</view></scroll-view>
    <view class="filters"><view class="chip-row"><view v-for="item in statuses" :key="item.value" class="chip" :class="{ active: status === item.value }" @click="setStatus(item.value)">{{ item.label }}</view></view><picker :range="sortLabels" :value="sortIndex" @change="setSort"><view class="sort-picker">{{ sortLabels[sortIndex] }} ▾</view></picker></view>
    <view v-if="loading" class="state">正在找书…</view><view v-else-if="error" class="state state-error" @click="load">{{ error }} · 点击重试</view><view v-else-if="!books.length" class="state">没有找到符合条件的作品。</view>
    <view v-else class="book-list"><BookRow v-for="book in books" :key="book.id" :book="book" /></view>
    <view v-if="total > size" class="pager"><button :disabled="page <= 1" @click="changePage(-1)">上一页</button><text>{{ page }} / {{ Math.ceil(total / size) }}</text><button :disabled="page * size >= total" @click="changePage(1)">下一页</button></view>
    <BottomNav active="books" />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import BookRow from '../../components/BookRow.vue'
import BottomNav from '../../components/BottomNav.vue'

const q = ref(''), categoryId = ref(''), status = ref(''), sort = ref('update')
const page = ref(1), size = 20, total = ref(0), books = ref([]), categories = ref([])
const loading = ref(false), error = ref('')
const statuses = [{ value: '', label: '全部' }, { value: '0', label: '连载' }, { value: '1', label: '完结' }]
const sortValues = ['update', 'visit', 'new'], sortLabels = ['最近更新', '人气最高', '最新上架']
const sortIndex = ref(0)
async function load() {
  loading.value = true; error.value = ''
  try {
    const data = await api.books({ q: q.value, categoryId: categoryId.value, status: status.value, sort: sort.value, page: page.value, size })
    books.value = data.list || []; total.value = data.total || 0
  } catch (reason) { error.value = reason.message || '书库加载失败' }
  finally { loading.value = false; uni.stopPullDownRefresh() }
}
const search = () => { page.value = 1; load() }
const selectCategory = (value) => { categoryId.value = value; search() }
const setStatus = (value) => { status.value = value; search() }
const setSort = (event) => { sortIndex.value = Number(event.detail.value); sort.value = sortValues[sortIndex.value]; search() }
const changePage = (offset) => { page.value += offset; load(); uni.pageScrollTo({ scrollTop: 0 }) }
onLoad(async (options) => {
  q.value = options.q || ''; sort.value = sortValues.includes(options.sort) ? options.sort : 'update'; sortIndex.value = sortValues.indexOf(sort.value)
  try { categories.value = await api.categories() || [] } catch { categories.value = [] }
  load()
})
onPullDownRefresh(load)
</script>

<style scoped>
.search-row { display: flex; gap: 12rpx; margin-bottom: 27rpx; }
.search-row input { flex: 1; width: 0; }
.search-row button { flex: none; min-width: 126rpx; }
.filters { display: flex; align-items: flex-start; justify-content: space-between; gap: 12rpx; margin: 8rpx 0 14rpx; padding-bottom: 16rpx; border-bottom: 1rpx solid #dce3dd; }
.filters .chip-row { flex: 1; }
.sort-picker { min-height: 66rpx; padding: 17rpx 8rpx; color: #45594d; font-size: 23rpx; white-space: nowrap; }
</style>
