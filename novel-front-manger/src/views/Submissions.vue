<template>
  <section>
    <div class="page-head"><div><p class="eyebrow">SUBMISSIONS</p><h1>投稿审核</h1><p>查看待审作品，核对章节和投稿资料。</p></div><button class="button subtle" title="刷新列表" @click="load"><RefreshCw :size="17" />刷新</button></div>
    <div class="toolbar"><div class="segmented"><button v-for="option in filters" :key="option.value" :class="{ active: status === option.value }" @click="changeStatus(option.value)">{{ option.label }}</button></div><span class="count">共 {{ total }} 条</span></div>
    <div v-if="loading" class="state">正在加载投稿…</div>
    <div v-else-if="error" class="state error"><p>{{ error }}</p><button class="button subtle" @click="load">重试</button></div>
    <div v-else-if="!items.length" class="state">当前没有{{ filters.find(f => f.value === status)?.label }}投稿。</div>
    <div v-else class="table-wrap"><table><thead><tr><th>作品</th><th>投稿人</th><th>分类</th><th>规模</th><th>提交时间</th><th>状态</th><th></th></tr></thead><tbody><tr v-for="item in items" :key="item.id"><td><strong>{{ item.bookName }}</strong><small>{{ item.authorName }}</small></td><td>{{ item.username }}</td><td>{{ item.categoryName }}</td><td>{{ item.chapterCount || 0 }} 章 · {{ item.wordCount || 0 }} 字</td><td>{{ item.submittedAt || '—' }}</td><td><span class="status" :class="item.status.toLowerCase()">{{ statusLabel(item.status) }}</span></td><td><router-link class="row-link" :to="`/submissions/${item.id}`">查看 <ArrowRight :size="15" /></router-link></td></tr></tbody></table></div>
    <div v-if="pages > 1" class="pager"><button :disabled="page === 1" @click="changePage(page - 1)">上一页</button><span>{{ page }} / {{ pages }}</span><button :disabled="page === pages" @click="changePage(page + 1)">下一页</button></div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, RefreshCw } from 'lucide-vue-next'
import { submissions } from '../api/review.js'

const route = useRoute(), router = useRouter()
const filters = [{ value: 'PENDING', label: '待审核' }, { value: 'REJECTED', label: '已退稿' }, { value: 'APPROVED', label: '已通过' }, { value: 'CANCELLED', label: '已取消' }]
const status = computed(() => filters.some(f => f.value === route.query.status) ? route.query.status : 'PENDING')
const page = computed(() => Math.max(1, Number(route.query.page) || 1))
const total = ref(0), items = ref([]), loading = ref(false), error = ref('')
const pages = computed(() => Math.max(1, Math.ceil(total.value / 20)))
const statusLabel = (value) => ({ PENDING: '待审核', REJECTED: '已退稿', APPROVED: '已通过', CANCELLED: '已取消' }[value] || value)
const load = async () => {
  loading.value = true; error.value = ''
  try { const result = await submissions({ status: status.value, page: page.value, size: 20 }); items.value = result.list || []; total.value = result.total || 0 }
  catch (reason) { error.value = reason.message }
  finally { loading.value = false }
}
const changeStatus = (value) => router.push({ name: 'submissions', query: { status: value } })
const changePage = (value) => router.push({ name: 'submissions', query: { status: status.value, page: value } })
watch(() => route.fullPath, load, { immediate: true })
</script>
