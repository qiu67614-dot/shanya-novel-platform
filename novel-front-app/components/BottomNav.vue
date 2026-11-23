<template>
  <view class="bottom-nav">
    <view v-for="item in items" :key="item.path" class="bottom-nav__item" :class="{ active: active === item.key }" @click="open(item)">
      <text class="bottom-nav__symbol" aria-hidden="true">{{ item.symbol }}</text><text>{{ item.label }}</text>
    </view>
  </view>
</template>

<script setup>
defineProps({ active: { type: String, default: '' } })
const items = [
  { key: 'home', label: '首页', symbol: '⌂', path: '/pages/index/index' },
  { key: 'books', label: '书库', symbol: '▤', path: '/pages/books/index' },
  { key: 'shelf', label: '书架', symbol: '▥', path: '/pages/bookshelf/index' },
  { key: 'me', label: '我的', symbol: '○', path: '/pages/me/index' },
]
const open = (item) => uni.redirectTo({ url: item.path })
</script>
