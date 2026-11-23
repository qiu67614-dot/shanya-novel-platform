<template>
  <ReaderLayout>
    <section class="page-heading private-heading">
      <div><p class="eyebrow">个人作品</p><h1>我的导入</h1></div>
      <span>{{ total }} 本</span>
    </section>

    <section class="private-upload" aria-labelledby="upload-title">
      <div>
        <h2 id="upload-title">导入 TXT 小说</h2>
        <p>导入 TXT 小说，最多 20 MB。</p>
      </div>
      <div class="private-publication-form">
        <fieldset class="private-mode" :disabled="uploading">
          <legend>阅读范围</legend>
          <label><input v-model="visibility" type="radio" value="PRIVATE" /> 仅自己阅读</label>
          <label><input v-model="visibility" type="radio" value="PUBLIC" /> 投稿公开阅读</label>
        </fieldset>
        <div v-if="visibility === 'PUBLIC'" class="publication-fields">
          <label>书名<input v-model.trim="metadata.bookName" maxlength="128" placeholder="公开展示的书名" /></label>
          <label>笔名<input v-model.trim="metadata.authorName" maxlength="64" placeholder="作者署名" /></label>
          <label>分类<select v-model="metadata.categoryId"><option value="">选择分类</option><option v-for="category in categories" :key="category.id" :value="category.id">{{ category.categoryName }}</option></select></label>
          <label class="publication-fields__wide">作品简介<textarea v-model.trim="metadata.bookDesc" maxlength="2000" rows="3" placeholder="可选" /></label>
          <label class="publication-fields__wide private-rights"><input v-model="metadata.rightsConfirmed" type="checkbox" /> 我确认拥有这本作品的公开发布权</label>
          <p class="publication-fields__wide">投稿后先由管理员审核，审核通过才会出现在公共书库。</p>
        </div>
      </div>
      <div class="private-upload__controls">
        <label for="private-file">选择 TXT 文件</label>
        <input id="private-file" type="file" accept=".txt,text/plain" :disabled="uploading" @change="chooseFile" />
        <label for="private-cover">封面图片（可选）</label>
        <input id="private-cover" type="file" accept="image/jpeg,image/png" :disabled="uploading" @change="chooseCover" />
        <BookNameCover class="cover-preview" :title="metadata.bookName || selectedFile?.name?.replace(/\.txt$/i, '') || '作品名称'" :src="coverPreview" />
        <button v-if="selectedCover" type="button" class="text-button" @click="clearCover">移除封面</button>
        <button class="primary-button" type="button" :disabled="!selectedFile || uploading" @click="upload">
          {{ uploading ? '正在导入…' : visibility === 'PUBLIC' ? '导入并投稿' : '导入作品' }}
        </button>
      </div>
      <p v-if="selectedFile" class="private-upload__file">{{ selectedFile.name }} · {{ formatSize(selectedFile.size) }}</p>
      <div v-if="uploading" class="private-upload__progress" role="status">
        <progress :value="progress" max="100"></progress>
        <span>{{ progress < 100 ? `上传中 ${progress}%` : '正在生成目录…' }}</span>
      </div>
      <p v-if="uploadError" class="private-error" role="alert">{{ uploadError }}</p>
    </section>

    <div v-if="loading" class="empty-state">正在加载私人书库…</div>
    <div v-else-if="loadError" class="empty-state"><p>{{ loadError }}</p><button class="secondary-button" @click="load">重新加载</button></div>
    <div v-else-if="!novels.length" class="empty-state">还没有导入的作品。</div>
    <section v-else class="private-list" aria-label="导入的作品">
      <article v-for="novel in novels" :key="novel.id" class="private-row">
        <BookNameCover :title="novel.bookName" :src="novel.picUrl" />
        <div class="private-row__body">
          <router-link class="private-row__title" :to="{ name: 'import-chapters', params: { novelId: novel.id } }">{{ novel.bookName }}</router-link>
          <p>{{ novel.chapterCount }} 章 · {{ formatSize(novel.fileSize) }} · {{ novel.encoding }}</p>
          <p>{{ novel.lastReadChapterName ? `读到：${novel.lastReadChapterName}` : '尚未开始阅读' }}</p>
          <p class="private-row__status">{{ statusText(novel) }}</p>
          <p v-if="novel.submissionStatus === 'REJECTED' && novel.rejectionReason" class="private-error">退稿原因：{{ novel.rejectionReason }}</p>
        </div>
        <div class="private-row__actions">
          <router-link v-if="novel.firstChapterId" class="primary-button" :to="readTarget(novel)">{{ novel.lastReadChapterId ? '继续阅读' : '开始阅读' }}</router-link>
          <router-link class="text-link" :to="{ name: 'import-chapters', params: { novelId: novel.id } }">目录</router-link>
          <button v-if="novel.submissionStatus === 'PENDING'" class="text-button" type="button" @click="cancel(novel)">取消投稿</button>
          <button v-if="['REJECTED', 'CANCELLED'].includes(novel.submissionStatus)" class="text-button" type="button" @click="startResubmit(novel)">修改后重投</button>
          <router-link v-if="novel.publicBookId" class="text-link" :to="{ name: 'book', params: { bookId: novel.publicBookId } }">公开页面</router-link>
          <button class="text-button" type="button" @click="remove(novel)">删除</button>
        </div>
        <form v-if="editingId === novel.id" class="publication-edit" @submit.prevent="resubmit(novel)">
          <h3>重新投稿《{{ novel.bookName }}》</h3>
          <div class="publication-fields">
            <label>书名<input v-model.trim="editMetadata.bookName" maxlength="128" required /></label>
            <label>笔名<input v-model.trim="editMetadata.authorName" maxlength="64" required /></label>
            <label>分类<select v-model="editMetadata.categoryId" required><option value="">选择分类</option><option v-for="category in categories" :key="category.id" :value="category.id">{{ category.categoryName }}</option></select></label>
            <label class="publication-fields__wide">作品简介<textarea v-model.trim="editMetadata.bookDesc" maxlength="2000" rows="3" /></label>
            <label class="publication-fields__wide">替换 TXT（可选，替换后阅读进度会重置）<input type="file" accept=".txt,text/plain" @change="chooseReplacement" /></label>
            <label class="publication-fields__wide">替换封面（可选）<input type="file" accept="image/jpeg,image/png" @change="chooseEditCover" /></label>
            <BookNameCover class="cover-preview" :title="editMetadata.bookName" :src="editCoverPreview || novel.picUrl" />
            <button v-if="editCover" type="button" class="text-button" @click="clearEditCover">保留原封面</button>
            <label class="publication-fields__wide private-rights"><input v-model="editMetadata.rightsConfirmed" type="checkbox" required /> 我确认拥有这本作品的公开发布权</label>
          </div>
          <p v-if="resubmitError" class="private-error">{{ resubmitError }}</p>
          <div class="publication-edit__actions"><button class="primary-button" :disabled="resubmitting">{{ resubmitting ? '提交中…' : '提交审核' }}</button><button class="text-button" type="button" @click="editingId = null">取消</button></div>
        </form>
      </article>
    </section>
    <nav v-if="pages > 1" class="pagination" aria-label="作品分页">
      <button :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
      <span>第 {{ page }} / {{ pages }} 页</span>
      <button :disabled="page >= pages" @click="changePage(page + 1)">下一页</button>
    </nav>
  </ReaderLayout>
</template>

<script>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ReaderLayout from '@/components/common/ReaderLayout.vue'
import BookNameCover from '@/components/common/BookNameCover.vue'
import { uploadPrivateNovel, getPrivateNovels, deletePrivateNovel, cancelSubmission, resubmitNovel } from '@/api/imports'
import { getCategories } from '@/api/books'

export default {
  name: 'Imports',
  components: { ReaderLayout, BookNameCover },
  setup() {
    const router = useRouter()
    const selectedFile = ref(null)
    const selectedCover = ref(null), coverPreview = ref('')
    const uploading = ref(false)
    const progress = ref(0)
    const visibility = ref('PRIVATE')
    const metadata = ref({ bookName: '', authorName: '', categoryId: '', bookDesc: '', rightsConfirmed: false })
    const categories = ref([])
    const editingId = ref(null)
    const editMetadata = ref({ bookName: '', authorName: '', categoryId: '', bookDesc: '', rightsConfirmed: false })
    const replacement = ref(null)
    const editCover = ref(null), editCoverPreview = ref('')
    const resubmitting = ref(false)
    const resubmitError = ref('')
    const uploadError = ref('')
    const loading = ref(false)
    const loadError = ref('')
    const novels = ref([])
    const page = ref(1)
    const total = ref(0)
    const size = 20
    const pages = computed(() => Math.max(1, Math.ceil(total.value / size)))
    const formatSize = (bytes) => `${(Number(bytes || 0) / 1024 / 1024).toFixed(2)} MB`
    const statusText = (novel) => ({ PENDING: '等待审核', REJECTED: '审核未通过',
      APPROVED: novel.publicStatus === 'ACTIVE' ? '已公开' : '已下架', CANCELLED: '投稿已取消' }[novel.submissionStatus] || '仅自己阅读')
    const validMetadata = (value) => Boolean(value.bookName && value.authorName && value.categoryId && value.rightsConfirmed)
    const readTarget = (novel) => ({ name: 'import-read', params: {
      novelId: novel.id, chapterId: novel.lastReadChapterId || novel.firstChapterId,
    } })

    const chooseFile = (event) => {
      uploadError.value = ''
      const file = event.target.files?.[0] || null
      selectedFile.value = null
      if (!file) return
      if (!file.name.toLowerCase().endsWith('.txt')) uploadError.value = '请选择 TXT 文件'
      else if (!file.size) uploadError.value = '文件为空'
      else if (file.size > 20 * 1024 * 1024) uploadError.value = '文件不能超过 20 MB'
      else selectedFile.value = file
    }
    const validateCover = (file) => {
      if (!file) return
      if (!['image/jpeg', 'image/png'].includes(file.type) || !file.size || file.size > 2 * 1024 * 1024) {
        throw new Error('封面须为非空且不超过 2 MB 的 JPG 或 PNG 图片')
      }
    }
    const clearCover = () => { if (coverPreview.value) URL.revokeObjectURL(coverPreview.value); coverPreview.value = ''; selectedCover.value = null }
    const clearEditCover = () => { if (editCoverPreview.value) URL.revokeObjectURL(editCoverPreview.value); editCoverPreview.value = ''; editCover.value = null }
    const chooseCover = (event) => {
      clearCover()
      try { validateCover(event.target.files?.[0]); selectedCover.value = event.target.files?.[0] || null
        if (selectedCover.value) coverPreview.value = URL.createObjectURL(selectedCover.value) }
      catch (error) { uploadError.value = error.message; event.target.value = '' }
    }
    const chooseEditCover = (event) => {
      clearEditCover()
      try { validateCover(event.target.files?.[0]); editCover.value = event.target.files?.[0] || null
        if (editCover.value) editCoverPreview.value = URL.createObjectURL(editCover.value) }
      catch (error) { resubmitError.value = error.message; event.target.value = '' }
    }

    const upload = async () => {
      if (!selectedFile.value || uploading.value) return
      uploading.value = true
      uploadError.value = ''
      progress.value = 0
      try {
        if (visibility.value === 'PUBLIC' && !validMetadata(metadata.value)) throw new Error('请填写书名、笔名、分类并确认发布权')
        const options = visibility.value === 'PUBLIC' ? { visibility: 'PUBLIC', ...metadata.value } : { visibility: 'PRIVATE' }
        const result = await uploadPrivateNovel(selectedFile.value, options, (value) => { progress.value = value }, selectedCover.value)
        clearCover()
        ElMessage.success(visibility.value === 'PUBLIC' ? '投稿已提交审核' : '作品已导入')
        await router.push({ name: 'import-chapters', params: { novelId: result.data.id } })
      } catch (error) {
        uploadError.value = error?.message || '导入失败，请检查文件后重试'
      } finally { uploading.value = false }
    }

    const load = async () => {
      loading.value = true
      loadError.value = ''
      try {
        const result = await getPrivateNovels({ page: page.value, size })
        novels.value = result.data?.list || []
        total.value = result.data?.total || 0
      } catch (error) {
        loadError.value = error?.message || '私人书库加载失败'
      } finally { loading.value = false }
    }
    const changePage = (target) => { page.value = target; load() }
    const startResubmit = (novel) => {
      editingId.value = novel.id
      editMetadata.value = { bookName: novel.bookName, authorName: novel.authorName || '',
        categoryId: novel.categoryId || '', bookDesc: novel.bookDesc || '', rightsConfirmed: Boolean(novel.rightsConfirmed) }
      replacement.value = null
      clearEditCover()
      resubmitError.value = ''
    }
    const chooseReplacement = (event) => { replacement.value = event.target.files?.[0] || null }
    const resubmit = async (novel) => {
      if (!validMetadata(editMetadata.value)) { resubmitError.value = '请填写书名、笔名、分类并确认发布权'; return }
      if (replacement.value && (!replacement.value.name.toLowerCase().endsWith('.txt') || replacement.value.size > 20 * 1024 * 1024 || !replacement.value.size)) {
        resubmitError.value = '替换文件须为非空且不超过 20 MB 的 TXT'; return
      }
      resubmitting.value = true
      resubmitError.value = ''
      try { await resubmitNovel(novel.id, editMetadata.value, replacement.value, editCover.value); clearEditCover(); editingId.value = null; ElMessage.success('已重新提交审核'); await load() }
      catch (error) { resubmitError.value = error?.message || '重新投稿失败' }
      finally { resubmitting.value = false }
    }
    const cancel = async (novel) => {
      try { await cancelSubmission(novel.id); ElMessage.success('投稿已取消'); await load() }
      catch (error) { ElMessage.error(error?.message || '取消投稿失败') }
    }
    const remove = async (novel) => {
      try {
        await ElMessageBox.confirm(`删除《${novel.bookName}》及其私人阅读进度？${novel.publicBookId ? '已发布的公开副本会继续保留，可在“我的公开”中申请撤回。' : ''}`, '删除作品', {
          confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning',
        })
      } catch { return }
      try {
        await deletePrivateNovel(novel.id)
        ElMessage.success('作品已删除')
        if (novels.value.length === 1 && page.value > 1) page.value--
        await load()
      } catch (error) {
        loadError.value = error?.message || '删除失败'
      }
    }
    onMounted(async () => { load(); try { categories.value = (await getCategories()).data || [] } catch { categories.value = [] } })
    onUnmounted(() => { clearCover(); clearEditCover() })
    return { selectedFile, selectedCover, coverPreview, chooseCover, clearCover, editCover, editCoverPreview, chooseEditCover, clearEditCover,
      uploading, progress, uploadError, loading, loadError, novels, visibility, metadata, categories,
      editingId, editMetadata, replacement, resubmitting, resubmitError, statusText, startResubmit, chooseReplacement, resubmit, cancel,
      page, total, pages, chooseFile, upload, load, changePage, remove, readTarget, formatSize }
  },
}
</script>
<style scoped>
.cover-preview { width: 96px; height: 132px; flex: none; }
</style>
