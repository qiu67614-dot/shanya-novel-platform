<template>
  <ReaderLayout>
    <article class="reader">
      <div class="reader-top">
        <router-link :to="{ name: 'import-chapters', params: { novelId: route.params.novelId } }">
          {{ data.book?.bookName || '返回目录' }}
        </router-link>
        <router-link to="/me/imports">我的导入</router-link>
      </div>
      <header>
        <h1>{{ data.chapter?.chapterName || (loading ? '正在加载…' : '章节不存在') }}</h1>
        <p v-if="data.chapter?.id">私人作品 · {{ data.chapter.wordCount || 0 }} 字</p>
      </header>

      <div v-if="loading" class="empty-state">正在加载正文…</div>
      <div v-else-if="loadError" class="empty-state error-state"><p>{{ loadError }}</p><button class="secondary-button" @click="load">重新加载</button></div>
      <div v-else-if="data.content" class="content">{{ data.content }}</div>
      <div v-else class="empty-state">暂无正文内容</div>
      <div v-if="progressError && data.chapter?.id" class="private-progress-error" role="alert">
        {{ progressError }} <button class="text-button" @click="saveProgress()">重试保存</button>
      </div>

      <nav class="chapter-nav" aria-label="章节导航">
        <button :disabled="!data.previousChapterId || loading || navigating" @click="go(data.previousChapterId)">上一章</button>
        <router-link :to="{ name: 'import-chapters', params: { novelId: route.params.novelId } }">章节目录</router-link>
        <button :disabled="!data.nextChapterId || loading || navigating" @click="go(data.nextChapterId)">下一章</button>
      </nav>
    </article>
  </ReaderLayout>
</template>

<script>
import { reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ReaderLayout from '@/components/common/ReaderLayout.vue'
import { getPrivateChapter, savePrivateProgress } from '@/api/imports'

const emptyChapter = () => ({ book: {}, chapter: {}, content: '',
  previousChapterId: null, nextChapterId: null })

export default {
  name: 'ImportRead',
  components: { ReaderLayout },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const data = reactive(emptyChapter())
    const loading = ref(false)
    const navigating = ref(false)
    const loadError = ref('')
    const progressError = ref('')
    let generation = 0

    const saveProgress = async (chapterId = data.chapter?.id, novelId = route.params.novelId,
      current = generation) => {
      if (!chapterId) return
      progressError.value = ''
      try {
        await savePrivateProgress(novelId, chapterId)
      } catch (error) {
        if (current === generation) progressError.value = error?.message || '阅读进度保存失败'
      }
    }

    const load = async () => {
      if (route.name !== 'import-read' || !route.params.novelId || !route.params.chapterId) return
      const current = ++generation
      loading.value = true
      loadError.value = ''
      progressError.value = ''
      Object.assign(data, emptyChapter())
      try {
        const result = await getPrivateChapter(route.params.novelId, route.params.chapterId)
        if (current !== generation) return
        Object.assign(data, result.data || {})
        window.scrollTo({ top: 0, behavior: 'smooth' })
        saveProgress(data.chapter.id, route.params.novelId, current)
      } catch (error) {
        if (current === generation) loadError.value = error?.message || '章节加载失败'
      } finally {
        if (current === generation) loading.value = false
      }
    }

    const go = async (chapterId) => {
      const id = Number(chapterId)
      if (!Number.isSafeInteger(id) || id <= 0 || navigating.value) return
      navigating.value = true
      try {
        await router.push({ name: 'import-read', params: {
          novelId: route.params.novelId, chapterId: String(id),
        } })
      } catch (error) {
        loadError.value = error?.message || '章节跳转失败'
      } finally { navigating.value = false }
    }

    watch(() => [route.name, route.params.novelId, route.params.chapterId], load, { immediate: true })
    return { route, data, loading, navigating, loadError, progressError, load, go, saveProgress }
  },
}
</script>
