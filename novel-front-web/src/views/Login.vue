<template>
  <ReaderLayout><section class="form-card">
    <h1>登录</h1>
    <form @submit.prevent="submit">
      <div class="form-row"><label>手机号</label><input v-model.trim="form.username" class="form-input" autocomplete="username" required /></div>
      <div class="form-row"><label>密码</label><input v-model="form.password" class="form-input" type="password" autocomplete="current-password" required /></div>
      <button class="primary-button" type="submit" :disabled="loading">{{ loading ? '登录中…' : '登录' }}</button>
    </form>
    <p class="muted">还没有账号？<router-link to="/register">立即注册</router-link></p>
  </section></ReaderLayout>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login } from '@/api/auth'
import { setToken, setUid, setUser } from '@/utils/auth'
import ReaderLayout from '@/components/common/ReaderLayout.vue'
const router = useRouter(); const route = useRoute(); const loading = ref(false)
const form = reactive({ username: '', password: '' })
const submit = async () => {
  loading.value = true
  try {
    const { data } = await login(form)
    setToken(data.token)
    setUid(data.uid || data.user?.id)
    setUser(data.user)
    router.push(route.query.redirect || '/books')
  } finally { loading.value = false }
}
</script>
