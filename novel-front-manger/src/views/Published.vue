<template>
  <section>
    <div class="page-head"><div><p class="eyebrow">PUBLICATIONS</p><h1>已发布作品</h1><p>管理审核通过的公开副本。</p></div><button class="button subtle" @click="load"><RefreshCw :size="17" /> 刷新</button></div>
    <div class="toolbar"><span class="count">共 {{ total }} 本</span></div>
    <div v-if="loading" class="state">正在加载作品…</div><div v-else-if="error" class="state error"><p>{{ error }}</p><button class="button subtle" @click="load">重试</button></div><div v-else-if="!items.length" class="state">还没有已通过审核的作品。</div>
    <div v-else class="table-wrap"><table><thead><tr><th>作品</th><th>投稿人</th><th>分类</th><th>规模</th><th>审核时间</th><th>公开状态</th><th>操作</th></tr></thead><tbody><tr v-for="item in items" :key="item.id"><td><strong>{{ item.bookName }}</strong><small>{{ item.authorName }} · #{{ item.publicBookId }}</small></td><td>{{ item.username }}</td><td>{{ item.categoryName }}</td><td>{{ item.chapterCount || 0 }} 章</td><td>{{ formatDate(item.reviewedAt) }}</td><td><span class="status" :class="item.publicStatus === 'ACTIVE' ? 'approved' : 'cancelled'">{{ item.publicStatus === 'ACTIVE' ? '公开中' : '已下架' }}</span></td><td><div class="row-actions"><router-link class="row-link" :to="`/submissions/${item.id}`">详情 <ArrowRight :size="15" /></router-link><button v-if="item.publicStatus === 'ACTIVE'" class="text-action danger-text" :disabled="busyId === item.id" @click="remove(item)">下架</button></div></td></tr></tbody></table></div>
    <div v-if="pages > 1" class="pager"><button :disabled="page === 1" @click="changePage(page - 1)">上一页</button><span>{{ page }} / {{ pages }}</span><button :disabled="page === pages" @click="changePage(page + 1)">下一页</button></div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, RefreshCw } from 'lucide-vue-next'
import { publications, takeDown } from '../api/review.js'
import { notifyError } from '../api/client.js'

const route = useRoute(), router = useRouter()
const page = computed(() => Math.max(1, Number(route.query.page) || 1))
const items = ref([]), total = ref(0), loading = ref(false), error = ref(''), busyId = ref(null)
const pages = computed(() => Math.max(1, Math.ceil(total.value / 20)))
const formatDate = (value) => value ? String(value).replace('T', ' ').slice(0, 19) : '—'
async function load() {
  loading.value = true; error.value = ''
  try { const result = await publications({ page: page.value, size: 20 }); items.value = result.list || []; total.value = result.total || 0 }
  catch (reason) { error.value = reason.message }
  finally { loading.value = false }
}
const changePage = (value) => router.push({ name: 'publications', query: { page: value } })
async function remove(item) {
  let reason
  try { reason = (await ElMessageBox.prompt('下架后，读者将无法访问该作品。私人原件不受影响。', `下架《${item.bookName}》`, { inputType: 'textarea', confirmButtonText: '确认下架', cancelButtonText: '取消', inputValidator: (value) => Boolean(value?.trim()) && value.length <= 500 || '请填写不超过 500 字的原因' })).value.trim() }
  catch { return }
  busyId.value = item.id
  try { await takeDown(item.publicBookId, reason); ElMessage.success('作品已下架'); await load() }
  catch (requestError) { notifyError(requestError); await load() }
  finally { busyId.value = null }
}
watch(() => route.fullPath, load, { immediate: true })
</script>
