<template>
  <section>
    <div class="page-head"><div><p class="eyebrow">WITHDRAWALS</p><h1>撤回申请</h1><p>管理员批准后，公开作品将从书库下架。</p></div><button class="button subtle" @click="load"><RefreshCw :size="17" /> 刷新</button></div>
    <div class="toolbar"><div class="segmented"><button v-for="option in filters" :key="option.value" :class="{ active: status === option.value }" @click="changeStatus(option.value)">{{ option.label }}</button></div><span class="count">共 {{ total }} 条</span></div>
    <div v-if="loading" class="state">正在加载申请…</div><div v-else-if="error" class="state error"><p>{{ error }}</p><button class="button subtle" @click="load">重试</button></div><div v-else-if="!items.length" class="state">当前没有撤回申请。</div>
    <div v-else class="table-wrap"><table><thead><tr><th>作品</th><th>申请人</th><th>撤回原因</th><th>申请时间</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="item in items" :key="item.id"><td><strong>{{ item.bookName }}</strong><small>作品 #{{ item.publicBookId }}</small></td><td>{{ item.username }}</td><td class="reason-cell">{{ item.reason || '未填写' }}</td><td>{{ formatDate(item.createTime) }}</td><td><span class="status" :class="item.status.toLowerCase()">{{ statusText(item.status) }}</span></td><td><div v-if="item.status === 'PENDING'" class="row-actions"><button class="text-action" :disabled="busyId === item.id" @click="accept(item)">批准</button><button class="text-action danger-text" :disabled="busyId === item.id" @click="decline(item)">驳回</button></div><span v-else>—</span></td></tr></tbody></table></div>
    <div v-if="pages > 1" class="pager"><button :disabled="page === 1" @click="changePage(page - 1)">上一页</button><span>{{ page }} / {{ pages }}</span><button :disabled="page === pages" @click="changePage(page + 1)">下一页</button></div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshCw } from 'lucide-vue-next'
import { approveWithdrawal, rejectWithdrawal, withdrawals } from '../api/review.js'
import { notifyError } from '../api/client.js'

const route = useRoute(), router = useRouter()
const filters = [{ value: 'PENDING', label: '待处理' }, { value: 'APPROVED', label: '已批准' }, { value: 'REJECTED', label: '已驳回' }, { value: 'CLOSED', label: '作品已下架' }]
const status = computed(() => filters.some((f) => f.value === route.query.status) ? route.query.status : 'PENDING')
const page = computed(() => Math.max(1, Number(route.query.page) || 1))
const items = ref([]), total = ref(0), loading = ref(false), error = ref(''), busyId = ref(null)
const pages = computed(() => Math.max(1, Math.ceil(total.value / 20)))
const statusText = (value) => ({ PENDING: '待处理', APPROVED: '已批准', REJECTED: '已驳回', CLOSED: '作品已下架' }[value] || value)
const formatDate = (value) => value ? String(value).replace('T', ' ').slice(0, 19) : '—'
async function load() {
  loading.value = true; error.value = ''
  try { const result = await withdrawals({ status: status.value, page: page.value, size: 20 }); items.value = result.list || []; total.value = result.total || 0 }
  catch (reason) { error.value = reason.message }
  finally { loading.value = false }
}
const changeStatus = (value) => router.push({ name: 'withdrawals', query: { status: value } })
const changePage = (value) => router.push({ name: 'withdrawals', query: { status: status.value, page: value } })
async function accept(item) {
  try { await ElMessageBox.confirm(`批准撤回《${item.bookName}》？批准后公开作品立即下架。`, '确认撤回', { confirmButtonText: '批准撤回', cancelButtonText: '取消', type: 'warning' }) }
  catch { return }
  busyId.value = item.id
  try { await approveWithdrawal(item.id); ElMessage.success('已批准撤回'); await load() }
  catch (reason) { notifyError(reason); await load() }
  finally { busyId.value = null }
}
async function decline(item) {
  let reason
  try { reason = (await ElMessageBox.prompt('填写驳回原因。', `驳回《${item.bookName}》的申请`, { inputType: 'textarea', confirmButtonText: '驳回申请', cancelButtonText: '取消', inputValidator: (value) => Boolean(value?.trim()) && value.length <= 500 || '请填写不超过 500 字的原因' })).value.trim() }
  catch { return }
  busyId.value = item.id
  try { await rejectWithdrawal(item.id, reason); ElMessage.success('已驳回申请'); await load() }
  catch (requestError) { notifyError(requestError); await load() }
  finally { busyId.value = null }
}
watch(() => route.fullPath, load, { immediate: true })
</script>
