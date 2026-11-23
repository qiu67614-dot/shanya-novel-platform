<template>
  <div class="login-screen">
    <div class="login-panel">
      <div class="login-brand"><span class="brand-mark">山</span><span>山崖书院 <small>内容管理</small></span></div>
      <p class="eyebrow">ADMINISTRATION</p>
      <h1>管理员登录</h1>
      <p class="login-note">审核投稿与管理公开作品</p>
      <form @submit.prevent="submit">
        <label>账号<input v-model.trim="form.username" autocomplete="username" required /></label>
        <label>密码<input v-model="form.password" type="password" autocomplete="current-password" required /></label>
        <p v-if="error" class="form-error" role="alert">{{ error }}</p>
        <button class="button primary full" :disabled="busy">{{ busy ? '正在登录…' : '登录工作台' }}</button>
      </form>
    </div>
    <div class="login-aside"><p>稿件审核 / 公开阅读</p><strong>让每一部作品<br />都经过认真审阅。</strong></div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login } from '../api/review.js'
import { setAdminSession } from '../api/client.js'

const route = useRoute(), router = useRouter()
const form = reactive({ username: '', password: '' })
const error = ref(''), busy = ref(false)
const submit = async () => {
  error.value = ''; busy.value = true
  try { setAdminSession(await login(form)); await router.replace(route.query.redirect || '/submissions') }
  catch (reason) { error.value = reason.message }
  finally { busy.value = false }
}
</script>
