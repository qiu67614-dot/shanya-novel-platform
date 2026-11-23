<template>
  <view class="screen detail-screen">
    <view v-if="loading" class="state">正在加载作品…</view><view v-else-if="error" class="state state-error" @click="load">{{ error }} · 点击重试</view>
    <template v-else-if="book.id">
      <view class="detail-head"><BookCover class="detail-cover" :title="book.bookName" :pic-url="book.picUrl" /><view class="detail-head__info"><text class="eyebrow">{{ book.categoryName || '小说' }}</text><text class="page-title">{{ book.bookName }}</text><text class="muted">{{ book.authorName || '佚名' }}</text><text class="muted">{{ book.bookStatus === 1 ? '已完结' : '连载中' }} · {{ book.chapterCount || 0 }} 章</text></view></view>
      <view class="actions main-actions"><button class="primary-btn" @click="start">开始阅读</button><button class="secondary-btn" @click="catalog">章节目录</button></view>
      <view class="actions secondary-actions"><button class="quiet-btn" @click="toggleShelf">{{ inShelf ? '移出书架' : '加入书架' }}</button><button class="quiet-btn" @click="toggleFollow">{{ isFollowing ? '取消关注' : '关注作品' }}</button></view>
      <view class="divider" /><text class="section-title">作品简介</text><text class="description">{{ book.bookDesc || '暂无简介' }}</text>
      <view class="facts"><text>字数 {{ book.wordCount || 0 }}</text><text>阅读 {{ book.visitCount || 0 }}</text></view>
      <view class="section-head"><text class="section-title">最近更新</text><text class="link-text" @click="catalog">全部章节</text></view><view class="latest-chapter" @click="latest"><text>{{ book.lastChapterName || '暂无章节' }}</text><text>查看 ›</text></view>
      <view class="section-head"><text class="section-title">读者评论</text><text class="muted">{{ comments.length ? `第 ${commentPage} 页` : '' }}</text></view>
      <view v-if="getToken()" class="comment-form"><textarea v-model.trim="commentText" maxlength="500" placeholder="聊聊这本书" /><button class="secondary-btn" :disabled="!commentText || posting" @click="postComment">发表评论</button></view>
      <view v-else class="login-hint" @click="login">登录后参与讨论 ›</view>
      <view v-if="commentLoading" class="state">正在加载评论…</view><view v-else-if="!comments.length" class="state">还没有评论。</view>
      <view v-for="item in comments" :key="item.id" class="comment"><view class="comment-head"><text class="comment-name">{{ item.nickName || item.nickname || '书友' }}</text><text class="muted">{{ formatTime(item.createTime) }}</text></view><text class="comment-body">{{ item.deleted ? '该评论已删除' : item.content }}</text><view class="comment-actions"><text v-if="!item.deleted" @click="reply(item)">回复</text><text v-if="item.replyCount" @click="loadReplies(item)">{{ item.repliesOpen ? '收起回复' : `查看 ${item.replyCount} 条回复` }}</text></view><view v-if="item.repliesOpen" class="replies"><view v-for="replyItem in item.replies || []" :key="replyItem.id" class="reply"><text class="comment-name">{{ replyItem.nickName || replyItem.nickname || '书友' }}：</text>{{ replyItem.deleted ? '该回复已删除' : replyItem.content }}</view></view></view>
      <view v-if="commentTotal > 10" class="pager"><button :disabled="commentPage <= 1" @click="changeCommentPage(-1)">上一页</button><text>{{ commentPage }} / {{ Math.ceil(commentTotal / 10) }}</text><button :disabled="commentPage * 10 >= commentTotal" @click="changeCommentPage(1)">下一页</button></view>
    </template>
  </view>
</template>

<script setup>
import BookCover from '../../components/BookCover.vue'
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { getToken, requireLogin, showError } from '../../utils/request.js'
const id = ref(''), book = ref({}), loading = ref(false), error = ref('')
const inShelf = ref(false), isFollowing = ref(false)
const comments = ref([]), commentTotal = ref(0), commentPage = ref(1), commentLoading = ref(false)
const commentText = ref(''), posting = ref(false)
const formatTime = (value) => String(value || '').replace('T', ' ').slice(0, 16)
const catalog = () => uni.navigateTo({ url: `/pages/book/chapters?id=${id.value}` })
const login = () => requireLogin(`/pages/book/detail?id=${id.value}`)
const openRead = (chapterId) => uni.navigateTo({ url: `/pages/read/index?bookId=${id.value}&chapterId=${chapterId}` })
async function start() {
  let chapterId = book.value.firstChapterId
  if (!chapterId) {
    try { chapterId = (await api.chapters(id.value, { page: 1, size: 1 })).list?.[0]?.id } catch (reason) { showError(reason); return }
  }
  if (chapterId) openRead(chapterId)
  else catalog()
}
const latest = () => book.value.lastChapterId ? openRead(book.value.lastChapterId) : catalog()
async function loadMembership() {
  if (!getToken()) { inShelf.value = false; isFollowing.value = false; return }
  try {
    const [shelf, followed] = await Promise.all([api.bookshelf(), api.following()])
    inShelf.value = (shelf || []).some((item) => String(item.book?.id) === id.value)
    isFollowing.value = (followed || []).some((item) => String(item.book?.id) === id.value)
  } catch { inShelf.value = false; isFollowing.value = false }
}
async function load() {
  loading.value = true; error.value = ''
  try { book.value = await api.book(id.value); api.visit(id.value).catch(() => {}); loadMembership(); loadComments() }
  catch (reason) { error.value = reason.message || '作品加载失败' }
  finally { loading.value = false }
}
async function toggleShelf() {
  if (!requireLogin(`/pages/book/detail?id=${id.value}`)) return
  try {
    if (inShelf.value) await api.removeShelf(id.value)
    else await api.addShelf(id.value)
    inShelf.value = !inShelf.value
    uni.showToast({ title: inShelf.value ? '已加入书架' : '已移出书架', icon: 'none' })
  } catch (reason) { showError(reason) }
}
async function toggleFollow() {
  if (!requireLogin(`/pages/book/detail?id=${id.value}`)) return
  try {
    if (isFollowing.value) await api.unfollow(id.value)
    else await api.follow(id.value)
    isFollowing.value = !isFollowing.value
    uni.showToast({ title: isFollowing.value ? '已关注' : '已取消关注', icon: 'none' })
  } catch (reason) { showError(reason) }
}
async function loadComments() {
  commentLoading.value = true
  try { const data = await api.comments(id.value, { page: commentPage.value, size: 10 }); comments.value = data.list || []; commentTotal.value = data.total || 0 }
  catch { comments.value = [] }
  finally { commentLoading.value = false }
}
const changeCommentPage = (offset) => { commentPage.value += offset; loadComments() }
async function postComment() {
  if (!commentText.value || posting.value) return
  posting.value = true
  try { await api.comment(id.value, { content: commentText.value }); commentText.value = ''; commentPage.value = 1; await loadComments() }
  catch (reason) { showError(reason) }
  finally { posting.value = false }
}
async function reply(item) {
  if (!requireLogin(`/pages/book/detail?id=${id.value}`)) return
  uni.showModal({ title: `回复 ${item.nickName || item.nickname || '书友'}`, editable: true, placeholderText: '写下回复', success: async ({ confirm, content }) => {
    if (!confirm || !content?.trim()) return
    try { await api.comment(id.value, { content: content.trim(), parentCommentId: item.id }); item.repliesOpen = false; loadReplies(item); loadComments() }
    catch (reason) { showError(reason) }
  } })
}
async function loadReplies(item) {
  if (item.repliesOpen) { item.repliesOpen = false; return }
  try { item.replies = (await api.replies(id.value, item.id, { page: 1, size: 100 })).list || []; item.repliesOpen = true }
  catch (reason) { showError(reason) }
}
onLoad((options) => { id.value = String(options.id || ''); if (id.value) load() })
onShow(() => { if (id.value) loadMembership() })
</script>

<style scoped>
.detail-head { display: flex; align-items: center; gap: 36rpx; margin: 16rpx 0 38rpx; padding-bottom: 35rpx; border-bottom: 1rpx solid #dce3dd; }
.detail-cover { flex-basis: 208rpx; width: 208rpx; height: 278rpx; }
.detail-cover .book-cover__title { max-height: 225rpx; font-size: 34rpx; }
.detail-head__info { flex: 1; min-width: 0; }
.detail-head__info .page-title { margin: 9rpx 0 20rpx; font-size: 42rpx; word-break: break-word; }
.detail-head__info .muted { display: block; margin-top: 9rpx; }
.main-actions button { flex: 1; }
.secondary-actions { margin-top: 13rpx; }
.secondary-actions button { flex: 1; font-size: 24rpx; }
.description { display: block; max-width: 44em; margin-top: 18rpx; color: #53645a; font-size: 27rpx; line-height: 1.75; white-space: pre-wrap; }
.facts { display: flex; flex-wrap: wrap; gap: 28rpx; margin-top: 25rpx; color: #67776c; font-size: 23rpx; }
.latest-chapter { display: flex; justify-content: space-between; gap: 20rpx; min-height: 85rpx; padding: 21rpx 22rpx; border-top: 1rpx solid #dce3dd; border-bottom: 1rpx solid #dce3dd; color: #37614e; font-size: 25rpx; }
.latest-chapter text:first-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.latest-chapter text:last-child { flex: none; }
.comment-form textarea { width: 100%; height: 145rpx; padding: 18rpx 20rpx; border: 1rpx solid #dce3dd; background: #fff; font-size: 25rpx; }
.comment-form button { margin: 13rpx 0; }
.login-hint { padding: 22rpx 0; color: #ae4938; border-top: 1rpx solid #dce3dd; font-size: 25rpx; }
.comment { padding: 27rpx 0; border-bottom: 1rpx solid #dce3dd; }
.comment-head { display: flex; justify-content: space-between; align-items: center; gap: 15rpx; }
.comment-name { color: #344e3d; font-weight: 700; }
.comment-body { display: block; margin: 15rpx 0; line-height: 1.65; white-space: pre-wrap; }
.comment-actions { display: flex; gap: 32rpx; color: #ae4938; font-size: 24rpx; }
.replies { margin-top: 18rpx; padding: 8rpx 19rpx; border-left: 3rpx solid #8fa99a; background: #edf2ee; }
.reply { padding: 13rpx 0; font-size: 24rpx; line-height: 1.6; }
@media (max-width: 480px) { .detail-head { gap: 24rpx; } .detail-cover { flex-basis: 170rpx; width: 170rpx; height: 235rpx; } .detail-head__info .page-title { font-size: 37rpx; } }
</style>
