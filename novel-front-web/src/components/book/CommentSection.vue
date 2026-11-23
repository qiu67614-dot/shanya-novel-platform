<template>
  <section class="comments" aria-labelledby="comments-title">
    <div class="comments__heading">
      <div>
        <p class="eyebrow">读者互动</p>
        <h2 id="comments-title">评论区</h2>
      </div>
      <span class="comments__count">{{ total }} 条评论</span>
    </div>

    <form class="comment-editor" @submit.prevent="submit">
      <div v-if="replyingTo" class="replying-bar">
        回复 {{ replyingTo.nickName }}
        <button type="button" class="plain-button" @click="cancelReply">取消</button>
      </div>
      <textarea
        v-model="content"
        class="comment-editor__input"
        maxlength="500"
        rows="4"
        :placeholder="editorPlaceholder"
        @focus="requireLoginOnFocus"
      />
      <div class="comment-editor__footer">
        <span>{{ content.length }}/500</span>
        <button class="primary-button" type="submit" :disabled="submitting">
          {{ submitting ? '提交中…' : (replyingTo ? '发表回复' : '发表评论') }}
        </button>
      </div>
    </form>

    <div v-if="loading" class="comment-state">正在加载评论...</div>
    <div v-else-if="error" class="comment-state comment-state--error">
      {{ error }}
      <button class="plain-button" type="button" @click="loadComments">重试</button>
    </div>
    <div v-else-if="!comments.length" class="comment-state">还没有评论，来写下第一条吧。</div>
    <div v-else class="comment-list">
      <article v-for="comment in comments" :key="comment.id" class="comment-item">
        <div class="comment-item__avatar">{{ avatarText(comment.nickName) }}</div>
        <div class="comment-item__body">
          <div class="comment-item__meta">
            <strong>{{ comment.nickName }}</strong>
            <time>{{ comment.createTime || '刚刚' }}</time>
          </div>
          <p class="comment-item__content" :class="{ 'is-deleted': comment.deleted }">{{ comment.content }}</p>
          <div class="comment-item__actions">
            <button v-if="!comment.deleted" class="plain-button" type="button" @click="startReply(comment)">回复</button>
            <button v-if="canDelete(comment)" class="plain-button danger" type="button" @click="remove(comment)">删除</button>
            <button v-if="comment.replyCount" class="plain-button" type="button" @click="toggleReplies(comment)">
              {{ replyState(comment).open ? '收起回复' : `查看 ${comment.replyCount} 条回复` }}
            </button>
          </div>

          <div v-if="replyState(comment).open" class="reply-list">
            <div v-if="replyState(comment).loading" class="reply-state">正在加载回复...</div>
            <div v-else-if="!replyState(comment).list.length" class="reply-state">暂无回复</div>
            <div v-else v-for="reply in replyState(comment).list" :key="reply.id" class="reply-item">
              <div class="reply-item__meta">
                <strong>{{ reply.nickName }}</strong>
                <span v-if="reply.replyToNickName">回复 {{ reply.replyToNickName }}</span>
                <time>{{ reply.createTime || '刚刚' }}</time>
              </div>
              <p>{{ reply.content }}</p>
              <button v-if="canDelete(reply)" class="plain-button danger" type="button" @click="remove(reply)">删除</button>
            </div>
            <button
              v-if="replyState(comment).page * replyState(comment).size < replyState(comment).total"
              class="load-more"
              type="button"
              :disabled="replyState(comment).loading"
              @click="loadReplies(comment, replyState(comment).page + 1)"
            >
              加载更多回复
            </button>
          </div>
        </div>
      </article>
    </div>

    <div v-if="total > size" class="comment-pagination">
      <button class="plain-button" type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
      <span>{{ page }} / {{ pageCount }}</span>
      <button class="plain-button" type="button" :disabled="page >= pageCount || loading" @click="changePage(page + 1)">下一页</button>
    </div>
  </section>
</template>

<script>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getComments, getCommentReplies } from '@/api/books'
import { createComment, deleteComment } from '@/api/me'
import { getToken, getUser } from '@/utils/auth'

export default {
  name: 'CommentSection',
  props: {
    bookId: { type: [String, Number], required: true },
  },
  setup(props) {
    const route = useRoute()
    const router = useRouter()
    const comments = ref([])
    const content = ref('')
    const replyingTo = ref(null)
    const loading = ref(false)
    const submitting = ref(false)
    const error = ref('')
    const page = ref(1)
    const size = 10
    const total = ref(0)
    const replies = reactive({})
    const loggedIn = computed(() => Boolean(getToken()))
    const editorPlaceholder = computed(() => {
      if (!loggedIn.value) return '登录后参与评论'
      return replyingTo.value ? `回复 ${replyingTo.value.nickName}` : '写下你的阅读感受'
    })
    const pageCount = computed(() => Math.max(1, Math.ceil(total.value / size)))

    const emptyReplyState = () => ({ open: false, loading: false, page: 0, size: 10, total: 0, list: [] })
    const replyState = (comment) => {
      if (!replies[comment.id]) replies[comment.id] = emptyReplyState()
      return replies[comment.id]
    }
    const avatarText = (name) => (name || '书').slice(0, 1)
    const canDelete = (comment) => !comment.deleted && String(comment.userId) === String(getUser()?.id)

    const loadComments = async (targetPage = page.value) => {
      loading.value = true
      error.value = ''
      try {
        const result = await getComments(props.bookId, { page: targetPage, size })
        comments.value = result.data?.list || []
        page.value = result.data?.page || targetPage
        total.value = result.data?.total || 0
      } catch (requestError) {
        error.value = requestError?.message || '评论加载失败'
      } finally {
        loading.value = false
      }
    }

    const loadReplies = async (comment, targetPage = 1) => {
      const state = replyState(comment)
      state.loading = true
      try {
        const result = await getCommentReplies(props.bookId, comment.id, { page: targetPage, size: state.size })
        const data = result.data || {}
        state.page = data.page || targetPage
        state.total = data.total || 0
        state.list = targetPage === 1 ? (data.list || []) : state.list.concat(data.list || [])
        state.open = true
      } finally {
        state.loading = false
      }
    }

    const toggleReplies = (comment) => {
      const state = replyState(comment)
      if (state.open) state.open = false
      else loadReplies(comment).catch(() => ElMessage.error('回复加载失败'))
    }

    const startReply = (comment) => {
      if (!getToken()) {
        router.push({ name: 'login', query: { redirect: route.fullPath } })
        return
      }
      replyingTo.value = comment
      content.value = ''
    }

    const cancelReply = () => { replyingTo.value = null }
    const requireLoginOnFocus = () => {
      if (!getToken()) router.push({ name: 'login', query: { redirect: route.fullPath } })
    }

    const submit = async () => {
      if (!getToken()) {
        requireLoginOnFocus()
        return
      }
      const text = content.value.trim()
      if (!text) {
        ElMessage.warning('请输入评论内容')
        return
      }
      submitting.value = true
      try {
        await createComment(props.bookId, { content: text, parentCommentId: replyingTo.value?.id })
        const target = replyingTo.value
        content.value = ''
        replyingTo.value = null
        ElMessage.success(target ? '回复已发表' : '评论已发表')
        if (target) {
          await loadReplies(target, 1)
        } else {
          await loadComments(1)
        }
      } finally {
        submitting.value = false
      }
    }

    const remove = async (comment) => {
      try {
        await deleteComment(comment.id)
        ElMessage.success('评论已删除')
        await loadComments(page.value)
        Object.keys(replies).forEach((key) => {
          const state = replies[key]
          state.list = state.list.filter((item) => item.id !== comment.id)
        })
      } catch (requestError) {
        ElMessage.error(requestError?.message || '删除失败')
      }
    }

    const changePage = (targetPage) => loadComments(targetPage)

    onMounted(() => loadComments())
    watch(() => props.bookId, () => {
      page.value = 1
      total.value = 0
      comments.value = []
      Object.keys(replies).forEach((key) => delete replies[key])
      loadComments()
    })

    return {
      comments, content, replyingTo, loading, submitting, error, page, size, total, pageCount,
      editorPlaceholder, replyState, avatarText, canDelete, loadComments, loadReplies, toggleReplies,
      startReply, cancelReply, requireLoginOnFocus, submit, remove, changePage,
    }
  },
}
</script>



