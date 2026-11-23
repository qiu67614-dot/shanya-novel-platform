<template>
  <ReaderLayout>
    <section class="page-heading private-heading">
      <div><p class="eyebrow">公开作品</p><h1>我的公开</h1></div>
      <router-link class="text-link" to="/me/imports">导入新作品</router-link>
    </section>
    <div v-if="loading" class="empty-state">正在加载作品…</div>
    <div v-else-if="error" class="empty-state"><p>{{ error }}</p><button class="secondary-button" @click="load">重新加载</button></div>
    <div v-else-if="!publications.length" class="empty-state">还没有通过审核的公开作品。</div>
    <section v-else class="private-list" aria-label="我的公开作品">
      <article v-for="item in publications" :key="item.submissionId" class="private-row">
        <BookNameCover :title="item.bookName" :src="item.picUrl" />
        <div class="private-row__body">
          <router-link class="private-row__title" :to="{ name: 'book', params: { bookId: item.publicBookId } }">{{ item.bookName }}</router-link>
          <p>{{ item.publicStatus === 'ACTIVE' ? '公开中' : '已下架' }} · {{ item.chapterCount || 0 }} 章</p>
          <p v-if="item.id">私人原件仍在“我的导入”中</p>
          <p v-else>私人原件已删除，公开副本独立保留</p>
        </div>
        <div class="private-row__actions">
          <router-link v-if="item.publicStatus === 'ACTIVE'" class="text-link" :to="{ name: 'book', params: { bookId: item.publicBookId } }">查看公开页</router-link>
          <button v-if="item.publicStatus === 'ACTIVE'" class="text-button" @click="withdraw(item)">申请撤回</button>
        </div>
      </article>
    </section>
  </ReaderLayout>
</template>

<script>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ReaderLayout from '@/components/common/ReaderLayout.vue'
import BookNameCover from '@/components/common/BookNameCover.vue'
import { getPublications, requestWithdrawal } from '@/api/imports'

export default {
  name: 'Publications', components: { ReaderLayout, BookNameCover },
  setup() {
    const publications = ref([])
    const loading = ref(false)
    const error = ref('')
    const load = async () => {
      loading.value = true
      error.value = ''
      try { publications.value = (await getPublications()).data || [] }
      catch (reason) { error.value = reason?.message || '公开作品加载失败' }
      finally { loading.value = false }
    }
    const withdraw = async (item) => {
      let reason
      try {
        const result = await ElMessageBox.prompt('填写撤回原因。申请期间作品仍然公开，管理员批准后才会下架。', `申请撤回《${item.bookName}》`, {
          confirmButtonText: '提交申请', cancelButtonText: '取消', inputType: 'textarea',
          inputValidator: (value) => value.length <= 500 || '原因不能超过 500 字',
        })
        reason = result.value
      } catch { return }
      try { await requestWithdrawal(item.publicBookId, reason); ElMessage.success('撤回申请已提交') }
      catch (requestError) { ElMessage.error(requestError?.message || '提交失败') }
    }
    onMounted(load)
    return { publications, loading, error, load, withdraw }
  },
}
</script>
