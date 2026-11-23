<template>
  <ReaderLayout>
    <section class="page-heading">
      <div><p class="eyebrow">更新提醒</p><h1>关注的书</h1></div>
      <router-link class="text-link" to="/books">继续找书</router-link>
    </section>
    <p class="notice">关注记录保存在账号中。打开此页面或作品详情时，会检查最新章节并提示更新。</p>
    <div v-if="loading" class="empty-state">正在检查关注作品...</div>
    <div v-else-if="!entries.length" class="empty-state">还没有关注作品，去书库选择一本吧。</div>
    <section v-else class="follow-list">
      <article v-for="entry in entries" :key="entry.book.id" class="follow-row">
        <BookNameCover :title="entry.book.bookName" :src="entry.book.picUrl" />
        <div class="follow-row__body">
          <div class="follow-title-line"><router-link :to="`/book/${entry.book.id}`">{{ entry.book.bookName }}</router-link><span v-if="entry.hasUpdate" class="update-badge">有更新</span></div>
          <p>{{ entry.book.authorName || '未知作者' }}</p>
          <small>最新章节：{{ entry.book.lastChapterName || '暂无章节' }}</small>
        </div>
        <div class="follow-actions">
          <router-link class="primary-button" :to="readTarget(entry)" @click="markRead(entry)">查看最新</router-link>
          <button v-if="entry.hasUpdate" class="text-button" @click="markRead(entry)">标为已读</button>
          <button class="text-button" @click="unfollow(entry.book.id)">取消关注</button>
        </div>
      </article>
    </section>
  </ReaderLayout>
</template>

<script>
import { onMounted, ref } from 'vue'
import ReaderLayout from '@/components/common/ReaderLayout.vue'
import BookNameCover from '@/components/common/BookNameCover.vue'
import { getFollowing, markFollowingRead, unfollowBook } from '@/api/me'

export default {
  name: 'Following',
  components: { ReaderLayout, BookNameCover },
  setup() {
    const entries = ref([])
    const loading = ref(false)
    const load = async () => {
      loading.value = true
      try {
        const result = await getFollowing()
        entries.value = result.data || []
      } catch (error) {
        entries.value = []
      }
      loading.value = false
    }
    const readTarget = (entry) => ({ name: 'read', params: { bookId: entry.book.id, chapterId: entry.book.lastChapterId || entry.book.firstChapterId } })
    const markRead = async (entry) => { const result = await markFollowingRead(entry.book.id); Object.assign(entry, result.data || {}, { hasUpdate: false }) }
    const unfollow = async (bookId) => { await unfollowBook(bookId); entries.value = entries.value.filter((entry) => entry.book.id !== bookId) }
    onMounted(load)
    return { entries, loading, readTarget, markRead, unfollow }
  },
}
</script>



