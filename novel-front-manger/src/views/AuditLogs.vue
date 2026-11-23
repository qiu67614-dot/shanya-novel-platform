<template>
  <section>
    <div class="page-head"><div><p class="eyebrow">AUDIT TRAIL</p><h1>操作记录</h1><p>查看投稿、撤回和下架的审核记录。</p></div><button class="button subtle" @click="load"><RefreshCw :size="17" /> 刷新</button></div>
    <div class="toolbar"><span class="count">共 {{ total }} 条</span></div>
    <div v-if="loading" class="state">正在加载记录…</div><div v-else-if="error" class="state error"><p>{{ error }}</p><button class="button subtle" @click="load">重试</button></div><div v-else-if="!items.length" class="state">还没有操作记录。</div>
    <div v-else class="table-wrap"><table><thead><tr><th>时间</th><th>管理员</th><th>操作</th><th>对象</th><th>原因</th></tr></thead><tbody><tr v-for="item in items" :key="item.id"><td>{{ formatDate(item.createTime) }}</td><td>{{ item.adminUsername }}</td><td><span class="status" :class="item.action === 'APPROVE' ? 'approved' : item.action === 'REJECT' ? 'rejected' : 'cancelled'">{{ actionText(item) }}</span></td><td>{{ targetText(item.targetType) }} #{{ item.targetId }}</td><td class="reason-cell">{{ item.reason || '—' }}</td></tr></tbody></table></div>
    <div v-if="pages > 1" class="pager"><button :disabled="page === 1" @click="changePage(page - 1)">上一页</button><span>{{ page }} / {{ pages }}</span><button :disabled="page === pages" @click="changePage(page + 1)">下一页</button></div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { RefreshCw } from 'lucide-vue-next'
import { auditLogs } from '../api/review.js'

const route = useRoute(), router = useRouter()
const page = computed(() => Math.max(1, Number(route.query.page) || 1))
const items = ref([]), total = ref(0), loading = ref(false), error = ref('')
const pages = computed(() => Math.max(1, Math.ceil(total.value / 20)))
const formatDate = (value) => value ? String(value).replace('T', ' ').slice(0, 19) : '—'
const targetText = (value) => ({ SUBMISSION: '投稿', WITHDRAWAL: '撤回申请', PUBLIC_BOOK: '公开作品' }[value] || value)
const actionText = (item) => ({ APPROVE: '通过', REJECT: '驳回', TAKE_DOWN: '下架' }[item.action] || item.action)
async function load() {
  loading.value = true; error.value = ''
  try { const result = await auditLogs({ page: page.value, size: 20 }); items.value = result.list || []; total.value = result.total || 0 }
  catch (reason) { error.value = reason.message }
  finally { loading.value = false }
}
const changePage = (value) => router.push({ name: 'audit-logs', query: { page: value } })
watch(() => route.fullPath, load, { immediate: true })
</script>
