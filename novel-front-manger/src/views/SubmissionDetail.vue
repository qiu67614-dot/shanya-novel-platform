<template>
  <section>
    <div class="page-head">
      <div><router-link class="back-link" to="/submissions"><ArrowLeft :size="16" /> 返回投稿列表</router-link><h1>稿件审核</h1><p v-if="item">投稿编号 #{{ item.id }} · {{ item.username }} · {{ formatDate(item.submittedAt) }}</p></div>
      <span v-if="item" class="status" :class="item.status.toLowerCase()">{{ statusText(item.status) }}</span>
    </div>
    <div v-if="loading" class="state">正在加载稿件…</div>
    <div v-else-if="error" class="state error"><p>{{ error }}</p><button class="button subtle" @click="load">重试</button></div>
    <div v-else-if="item" class="review-grid">
      <div class="review-main">
        <div class="section-head"><h2>投稿资料</h2><span>{{ item.chapterCount || 0 }} 章 · {{ item.wordCount || 0 }} 字</span></div>
        <div class="review-fields">
          <label>公开书名<input v-model.trim="form.bookName" maxlength="128" :disabled="!pending || busy" /></label>
          <label>作者署名<input v-model.trim="form.authorName" maxlength="64" :disabled="!pending || busy" /></label>
          <label>分类<select v-model="form.categoryId" :disabled="!pending || busy"><option value="">选择分类</option><option v-for="category in categoryList" :key="category.id" :value="category.id">{{ category.categoryName }}</option></select></label>
          <label class="field-wide">作品简介<textarea v-model.trim="form.bookDesc" rows="5" maxlength="2000" :disabled="!pending || busy" /></label>
        </div>
        <div class="submission-cover"><img v-if="item.picUrl && !coverFailed" :src="item.picUrl" :alt="item.bookName + '封面'" @error="coverFailed = true" /><div v-else class="submission-cover__fallback">{{ item.bookName }}</div><span>{{ item.picUrl ? '投稿封面' : '默认文字封面' }}</span></div>
        <dl class="facts"><div><dt>原始文件</dt><dd>{{ item.originalFileName || '私人原件已删除' }}</dd></div><div><dt>发布权声明</dt><dd>{{ item.rightsConfirmed ? '投稿人已确认' : '未确认' }}</dd></div><div v-if="item.publicBookId"><dt>公开作品 ID</dt><dd>{{ item.publicBookId }}</dd></div><div v-if="item.rejectionReason"><dt>退稿原因</dt><dd>{{ item.rejectionReason }}</dd></div></dl>
        <div v-if="pending" class="review-actions"><button class="button primary" :disabled="busy || !item.rightsConfirmed || !item.userNovelId" @click="accept"><Check :size="17" /> {{ busy ? '处理中…' : '审核通过' }}</button><button class="button danger" :disabled="busy" @click="decline"><X :size="17" /> 退稿</button></div>
      </div>
      <div class="preview-pane">
        <div class="section-head"><h2>章节预览</h2><span>{{ chapterList.length }} 章</span></div>
        <p v-if="!chapterList.length" class="preview-empty">没有可预览的章节。</p>
        <div v-else class="preview-layout">
          <div class="chapter-list" role="list"><button v-for="entry in chapterList" :key="entry.id" :class="{ selected: selectedId === entry.id }" @click="openChapter(entry.id)"><span>{{ entry.chapterNum }}.</span> {{ entry.chapterName }}</button></div>
          <div class="chapter-preview"><div v-if="chapterLoading" class="state">正在加载正文…</div><div v-else-if="chapterError" class="state error">{{ chapterError }}</div><template v-else-if="selectedChapter"><h3>{{ selectedChapter.chapterName }}</h3><p class="preview-meta">{{ selectedChapter.wordCount || 0 }} 字</p><div class="chapter-text">{{ selectedChapter.content }}</div></template><div v-else class="state">选择章节查看正文</div></div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Check, X } from 'lucide-vue-next'
import { approve, categories, chapter, chapters, reject, submission } from '../api/review.js'
import { notifyError } from '../api/client.js'

const route = useRoute(), router = useRouter()
const item = ref(null), chapterList = ref([]), categoryList = ref([])
const coverFailed = ref(false)
const selectedId = ref(null), selectedChapter = ref(null)
const loading = ref(false), chapterLoading = ref(false), busy = ref(false)
const error = ref(''), chapterError = ref('')
const form = reactive({ bookName: '', authorName: '', categoryId: '', bookDesc: '' })
const pending = computed(() => item.value?.status === 'PENDING')
const statusText = (value) => ({ PENDING: '待审核', REJECTED: '已退稿', APPROVED: '已通过', CANCELLED: '已取消' }[value] || value)
const formatDate = (value) => value ? String(value).replace('T', ' ').slice(0, 19) : '—'

async function openChapter(id) {
  selectedId.value = id; selectedChapter.value = null; chapterError.value = ''; chapterLoading.value = true
  try { selectedChapter.value = await chapter(route.params.id, id) }
  catch (reason) { chapterError.value = reason.message }
  finally { chapterLoading.value = false }
}
async function load() {
  loading.value = true; error.value = ''; item.value = null; selectedChapter.value = null; coverFailed.value = false
  try {
    const [detail, list, categoryData] = await Promise.all([submission(route.params.id), chapters(route.params.id), categories()])
    item.value = detail; chapterList.value = list || []; categoryList.value = categoryData || []
    Object.assign(form, { bookName: detail.bookName || '', authorName: detail.authorName || '', categoryId: detail.categoryId || '', bookDesc: detail.bookDesc || '' })
    if (chapterList.value.length) await openChapter(chapterList.value[0].id)
  } catch (reason) { error.value = reason.message }
  finally { loading.value = false }
}
async function accept() {
  if (!form.bookName || !form.authorName || !form.categoryId) { ElMessage.warning('请填写书名、作者和分类'); return }
  try { await ElMessageBox.confirm(`确认发布《${form.bookName}》的 ${item.value.chapterCount} 章？`, '审核通过', { confirmButtonText: '确认发布', cancelButtonText: '取消' }) }
  catch { return }
  busy.value = true
  try { await approve(item.value.id, { ...form }); ElMessage.success('作品已发布'); await router.push('/submissions') }
  catch (reason) { notifyError(reason); await load() }
  finally { busy.value = false }
}
async function decline() {
  let reason
  try {
    const answer = await ElMessageBox.prompt('退稿原因会显示给投稿人。', '退稿', { confirmButtonText: '确认退稿', cancelButtonText: '取消', inputType: 'textarea', inputValidator: (value) => Boolean(value?.trim()) && value.length <= 500 || '请填写不超过 500 字的原因' })
    reason = answer.value.trim()
  } catch { return }
  busy.value = true
  try { await reject(item.value.id, reason); ElMessage.success('已退稿'); await router.push('/submissions') }
  catch (requestError) { notifyError(requestError); await load() }
  finally { busy.value = false }
}
watch(() => route.params.id, load, { immediate: true })
</script>
<style scoped>
.submission-cover { display: flex; align-items: flex-end; gap: 16px; margin: 20px 0; color: #66736b; font-size: 13px; }
.submission-cover img, .submission-cover__fallback { width: 112px; height: 152px; object-fit: cover; }
.submission-cover__fallback { display: flex; align-items: center; justify-content: center; padding: 12px; background: #3b554d; color: white; text-align: center; font-family: serif; font-weight: 700; }
</style>
