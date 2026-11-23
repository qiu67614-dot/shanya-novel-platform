<template>
  <ReaderLayout>
    <section class="page-heading private-heading">
      <div><p class="eyebrow">私人作品 · 章节目录</p><h1>{{ novel.bookName || '章节目录' }}</h1></div>
      <router-link class="text-link" to="/me/imports">返回我的导入</router-link>
    </section>
    <div v-if="loading" class="empty-state">正在加载目录…</div>
    <div v-else-if="error" class="empty-state"><p>{{ error }}</p><button class="secondary-button" @click="load">重新加载</button></div>
    <template v-else>
      <div class="private-chapters__summary">
        <span>{{ novel.chapterCount }} 章 · {{ novel.wordCount }} 字</span>
        <router-link v-if="novel.firstChapterId" class="primary-button" :to="readTarget">
          {{ novel.lastReadChapterId ? '继续阅读' : '开始阅读' }}
        </router-link>
      </div>
      <div class="chapter-list">
        <router-link v-for="chapter in chapters" :key="chapter.id"
          :to="{ name: 'import-read', params: { novelId: novel.id, chapterId: chapter.id } }">
          <span>第{{ chapter.chapterNum }}章</span>{{ chapter.chapterName }}
          <small>{{ chapter.wordCount }} 字</small>
        </router-link>
        <div v-if="!chapters.length" class="empty-state">暂无章节</div>
      </div>
      <nav v-if="pages > 1" class="pagination" aria-label="章节分页">
        <button :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
        <span>第 {{ page }} / {{ pages }} 页</span>
        <button :disabled="page >= pages" @click="changePage(page + 1)">下一页</button>
      </nav>
    </template>
  </ReaderLayout>
</template>

<script>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import ReaderLayout from '@/components/common/ReaderLayout.vue'
import { getPrivateNovel, getPrivateChapters } from '@/api/imports'

export default {
  name: 'ImportChapters',
  components: { ReaderLayout },
  setup() {
    const route = useRoute()
    const novel = ref({})
    const chapters = ref([])
    const page = ref(1)
    const size = 100
    const total = ref(0)
    const loading = ref(false)
    const error = ref('')
    const pages = computed(() => Math.max(1, Math.ceil(total.value / size)))
    const readTarget = computed(() => ({ name: 'import-read', params: {
      novelId: novel.value.id, chapterId: novel.value.lastReadChapterId || novel.value.firstChapterId,
    } }))
    const load = async () => {
      if (route.name !== 'import-chapters' || !route.params.novelId) return
      loading.value = true
      error.value = ''
      try {
        const [detail, directory] = await Promise.all([
          getPrivateNovel(route.params.novelId),
          getPrivateChapters(route.params.novelId, { page: page.value, size }),
        ])
        novel.value = detail.data || {}
        chapters.value = directory.data?.list || []
        total.value = directory.data?.total || 0
      } catch (requestError) {
        error.value = requestError?.message || '目录加载失败'
      } finally { loading.value = false }
    }
    const changePage = (target) => { page.value = target; load() }
    watch(() => [route.name, route.params.novelId], () => { page.value = 1; load() }, { immediate: true })
    return { novel, chapters, page, pages, loading, error, readTarget, load, changePage }
  },
}
</script>
