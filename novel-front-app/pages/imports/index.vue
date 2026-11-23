<template>
  <view class="screen"><text class="eyebrow">个人书库</text><text class="page-title">我的导入</text>
    <GuestGate v-if="!authenticated" title="私人书库" redirect="/pages/imports/index" />
    <template v-else>
    <view class="upload-section"><text class="section-title">导入 TXT 小说</text>
      <view class="mode-row"><view :class="{ active: visibility === 'PRIVATE' }" @click="visibility = 'PRIVATE'">仅自己阅读</view><view :class="{ active: visibility === 'PUBLIC' }" @click="visibility = 'PUBLIC'">投稿公开阅读</view></view>
      <button class="quiet-btn choose" @click="chooseFile">{{ selectedName || '选择 TXT 文件' }}</button>
      <view class="cover-picker"><text class="field-label">封面图片（可选）</text><button class="quiet-btn" @click="chooseCover('new')">{{ selectedCoverPath ? '更换封面' : '选择封面' }}</button><text v-if="selectedCoverPath" class="link-text" @click="selectedCoverPath = ''">移除</text></view>
      <image v-if="selectedCoverPath" class="cover-preview" :src="selectedCoverPath" mode="aspectFill" />
      <template v-if="visibility === 'PUBLIC'">
        <view class="field"><text class="field-label">作品名称</text><input v-model.trim="form.bookName" class="field-input" maxlength="128" placeholder="公开展示的书名" /></view>
        <view class="field"><text class="field-label">作者笔名</text><input v-model.trim="form.authorName" class="field-input" maxlength="64" placeholder="公开展示的笔名" /></view>
        <view class="field"><text class="field-label">分类</text><picker :range="categories" range-key="categoryName" @change="form.categoryId = categories[$event.detail.value]?.id"><view class="field-input">{{ categories.find((item) => item.id === form.categoryId)?.categoryName || '选择分类' }}　⌄</view></picker></view>
        <view class="field"><text class="field-label">作品简介</text><textarea v-model.trim="form.bookDesc" class="field-textarea" maxlength="2000" placeholder="介绍作品（可选）" /></view>
        <view class="rights" @click="form.rightsConfirmed = !form.rightsConfirmed"><text class="check">{{ form.rightsConfirmed ? '☑' : '□' }}</text><text>我确认拥有这本作品的公开发布权</text></view>
      </template>
      <button class="primary-btn upload" :disabled="uploading || !selectedPath" @click="submit">{{ uploading ? `处理中 ${progress}%` : visibility === 'PUBLIC' ? '提交审核' : '导入私人书库' }}</button>
      <text v-if="uploadError" class="error-text">{{ uploadError }}</text>
    </view>
    <view class="section-head"><text class="section-title">已导入作品</text><text class="muted">{{ total }} 本</text></view>
    <view v-if="loading && !items.length" class="state">正在加载作品…</view><view v-else-if="error" class="state state-error" @click="load">{{ error }} · 点击重试</view><view v-else-if="!items.length" class="state">尚未导入作品。</view>
    <view v-for="item in items" :key="item.id" class="import-row"><BookCover :title="item.bookName" :pic-url="item.picUrl" /><view class="import-body"><text class="import-name" @click="catalog(item)">{{ item.bookName }}</text><text class="muted">{{ item.chapterCount }} 章 · {{ status(item) }}</text><text v-if="item.lastReadChapterName" class="muted">读到：{{ item.lastReadChapterName }}</text><text v-if="item.rejectionReason" class="error-text">{{ item.rejectionReason }}</text><view class="import-actions"><text @click="read(item)">{{ item.lastReadChapterId ? '继续阅读' : '开始阅读' }}</text><text @click="catalog(item)">目录</text><text v-if="item.submissionStatus === 'PENDING'" @click="cancel(item)">取消投稿</text><text v-if="['REJECTED', 'CANCELLED'].includes(item.submissionStatus)" @click="startEdit(item)">修改后重投</text><text @click="remove(item)">删除</text></view></view>
      <view v-if="editingId === item.id" class="resubmit"><text class="section-title">重新投稿</text><view class="field"><text class="field-label">作品名称</text><input v-model.trim="edit.bookName" class="field-input" maxlength="128" /></view><view class="field"><text class="field-label">作者笔名</text><input v-model.trim="edit.authorName" class="field-input" maxlength="64" /></view><view class="field"><text class="field-label">分类</text><picker :range="categories" range-key="categoryName" @change="edit.categoryId = categories[$event.detail.value]?.id"><view class="field-input">{{ categories.find((category) => category.id === edit.categoryId)?.categoryName || '选择分类' }}　⌄</view></picker></view><view class="field"><text class="field-label">作品简介</text><textarea v-model.trim="edit.bookDesc" class="field-textarea" maxlength="2000" /></view><button class="quiet-btn choose" @click="chooseReplacement">{{ replacementName || '选择新的 TXT 文件' }}</button><view class="cover-picker"><text class="field-label">替换封面（可选）</text><button class="quiet-btn" @click="chooseCover('edit')">选择封面</button><text v-if="editCoverPath" class="link-text" @click="editCoverPath = ''">保留原封面</text></view><image v-if="editCoverPath" class="cover-preview" :src="editCoverPath" mode="aspectFill" /><view class="rights" @click="edit.rightsConfirmed = !edit.rightsConfirmed"><text class="check">{{ edit.rightsConfirmed ? '☑' : '□' }}</text><text>我确认拥有公开发布权</text></view><view class="actions"><button class="primary-btn" :disabled="resubmitting" @click="resubmit(item)">{{ resubmitting ? '提交中…' : '重新提交' }}</button><button class="quiet-btn" @click="editingId = null">取消</button></view>
      </view>
    </view>
    <button v-if="items.length < total" class="quiet-btn more" :disabled="loading" @click="more">加载更多</button>
    </template>
  </view>
</template>
<script setup>
import { ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { getToken, showError, uploadTxt } from '../../utils/request.js'
import { pickTxt } from '../../utils/pick-txt.js'
import GuestGate from '../../components/GuestGate.vue'
import BookCover from '../../components/BookCover.vue'
const visibility = ref('PRIVATE'), selectedPath = ref(''), selectedName = ref(''), selectedCoverPath = ref(''), uploading = ref(false), progress = ref(0), uploadError = ref('')
const form = ref({ bookName: '', authorName: '', categoryId: null, bookDesc: '', rightsConfirmed: false })
const categories = ref([]), items = ref([]), page = ref(1), total = ref(0), loading = ref(false), error = ref('')
const authenticated = ref(false)
const editingId = ref(null), edit = ref({}), replacementPath = ref(''), replacementName = ref(''), resubmitting = ref(false)
const editCoverPath = ref('')
function chooseCover(target) {
  uni.chooseImage({ count: 1, sourceType: ['album'], success: ({ tempFilePaths, tempFiles }) => {
    const file = tempFiles?.[0]
    if (!file || !file.size || file.size > 2 * 1024 * 1024) return showError(new Error('封面须为非空且不超过 2 MB 的图片'))
    const path = tempFilePaths?.[0] || file.path
    if (target === 'edit') editCoverPath.value = path
    else selectedCoverPath.value = path
  }, fail: (error) => { if (!/cancel|取消/i.test(error.errMsg || '')) showError(new Error(error.errMsg || '选择封面失败')) } })
}
const status = (item) => ({ PENDING: '等待审核', REJECTED: '审核未通过', CANCELLED: '投稿已取消', APPROVED: item.publicStatus === 'ACTIVE' ? '已公开' : '已下架' }[item.submissionStatus] || '仅自己阅读')
async function chooseFile() {
  selectedPath.value = ''; uploadError.value = ''
  try {
    const file = await pickTxt()
    if (!file.name?.toLowerCase().endsWith('.txt')) throw new Error('请选择 TXT 文件')
    if (!file.size || file.size > 20 * 1024 * 1024) throw new Error('文件须非空且不超过 20 MB')
    selectedPath.value = file.path; selectedName.value = file.name
  } catch (reason) { if (!/cancel|取消/.test(reason.message || '')) uploadError.value = reason.message }
}
async function submit() {
  if (!selectedPath.value || uploading.value) return
  if (visibility.value === 'PUBLIC' && (!form.value.bookName || !form.value.authorName || !form.value.categoryId || !form.value.rightsConfirmed)) return showError(new Error('请填写书名、笔名、分类并确认发布权'))
  uploading.value = true; uploadError.value = ''; progress.value = 0
  try {
    const fields = visibility.value === 'PUBLIC' ? { visibility: 'PUBLIC', ...form.value } : { visibility: 'PRIVATE' }
    const result = await uploadTxt(selectedPath.value, fields, (value) => { progress.value = value }, '/front/me/imports', selectedCoverPath.value)
    selectedPath.value = ''; selectedName.value = ''; selectedCoverPath.value = ''; page.value = 1; await load()
    uni.showToast({ title: visibility.value === 'PUBLIC' ? '已提交审核' : '导入成功', icon: 'none' })
    if (result?.id) catalog(result)
  } catch (reason) { uploadError.value = reason.message || '导入失败' }
  finally { uploading.value = false }
}
async function load() { authenticated.value = Boolean(getToken()); if (!authenticated.value) { uni.stopPullDownRefresh(); return } loading.value = true; error.value = ''; try { const data = await api.imports({ page: page.value, size: 20 }); items.value = page.value === 1 ? data.list || [] : [...items.value, ...(data.list || [])]; total.value = data.total || 0 } catch (reason) { error.value = reason.message; authenticated.value = Boolean(getToken()) } finally { loading.value = false; uni.stopPullDownRefresh() } }
const more = () => { page.value++; load() }
const catalog = (item) => uni.navigateTo({ url: `/pages/imports/chapters?id=${item.id}` })
const read = (item) => uni.navigateTo({ url: item.lastReadChapterId || item.firstChapterId ? `/pages/read/index?bookId=${item.id}&chapterId=${item.lastReadChapterId || item.firstChapterId}&private=1` : `/pages/imports/chapters?id=${item.id}` })
function cancel(item) { uni.showModal({ title: '取消投稿', content: `取消《${item.bookName}》的待审投稿？`, success: async ({ confirm }) => { if (!confirm) return; try { await api.cancelSubmission(item.id); page.value = 1; load() } catch (reason) { showError(reason) } } }) }
function remove(item) { uni.showModal({ title: '删除私人作品', content: `删除《${item.bookName}》和私人阅读进度？已发布的公开副本仍会保留。`, success: async ({ confirm }) => { if (!confirm) return; try { await api.deleteImport(item.id); page.value = 1; load() } catch (reason) { showError(reason) } } }) }
function startEdit(item) { editingId.value = item.id; edit.value = { bookName: item.bookName, authorName: item.authorName || '', categoryId: item.categoryId || null, bookDesc: item.bookDesc || '', rightsConfirmed: Boolean(item.rightsConfirmed) }; replacementPath.value = ''; replacementName.value = ''; editCoverPath.value = '' }
async function chooseReplacement() { try { const file = await pickTxt(); if (!file.name?.toLowerCase().endsWith('.txt') || !file.size || file.size > 20 * 1024 * 1024) throw new Error('请选择非空且不超过 20 MB 的 TXT'); replacementPath.value = file.path; replacementName.value = file.name } catch (reason) { if (!/cancel|取消/.test(reason.message || '')) showError(reason) } }
async function resubmit(item) {
  if (!edit.value.bookName || !edit.value.authorName || !edit.value.categoryId || !edit.value.rightsConfirmed) return showError(new Error('请补全作品信息并确认发布权'))
  if (!replacementPath.value && !editCoverPath.value) return showError(new Error('请选择新的 TXT 文件或封面'))
  resubmitting.value = true
  try { await uploadTxt(replacementPath.value, edit.value, null, `/front/me/imports/${item.id}/resubmit`, editCoverPath.value); editingId.value = null; replacementPath.value = ''; editCoverPath.value = ''; page.value = 1; await load(); uni.showToast({ title: '已重新提交审核', icon: 'none' }) }
  catch (reason) { showError(reason) }
  finally { resubmitting.value = false }
}
onShow(() => { authenticated.value = Boolean(getToken()); if (!authenticated.value) return; page.value = 1; load(); api.categories().then((data) => { categories.value = data || [] }).catch(() => {}) })
onPullDownRefresh(() => { page.value = 1; load() })
</script>
<style scoped>
.upload-section, .resubmit { padding: 28rpx; border: 1rpx solid #dce3dd; background: #fff; }
.upload-section .section-title, .resubmit .section-title { display: block; margin-bottom: 22rpx; }
.mode-row { display: flex; margin-bottom: 20rpx; border: 1rpx solid #dce3dd; }
.mode-row view { flex: 1; padding: 19rpx 10rpx; color: #5e7064; text-align: center; font-size: 24rpx; }
.mode-row .active { background: #37614e; color: #fff; }
.choose, .upload { width: 100%; margin: 10rpx 0 22rpx; }
.cover-picker { display: flex; align-items: center; gap: 16rpx; margin: 8rpx 0 18rpx; }
.cover-picker .quiet-btn { margin: 0; font-size: 23rpx; }
.cover-preview { display: block; width: 128rpx; height: 172rpx; margin-bottom: 20rpx; }
.rights { display: flex; align-items: flex-start; gap: 12rpx; margin-bottom: 24rpx; font-size: 24rpx; line-height: 1.6; }
.check { color: #ae4938; font-size: 35rpx; }
.import-row { display: flex; flex-wrap: wrap; gap: 20rpx; padding: 29rpx 0; border-bottom: 1rpx solid #dce3dd; }
.import-body { flex: 1; min-width: 0; }
.import-name { display: block; margin-bottom: 8rpx; font-family: "Songti SC", serif; font-size: 30rpx; font-weight: 700; }
.import-body .muted { display: block; margin: 8rpx 0; }
.import-actions { display: flex; flex-wrap: wrap; gap: 22rpx; margin-top: 17rpx; color: #ae4938; font-size: 23rpx; }
.import-actions text:last-child { color: #69776c; }
.resubmit { width: 100%; }
.resubmit .actions button { flex: 1; }
.more { width: 100%; margin-top: 25rpx; }
</style>
