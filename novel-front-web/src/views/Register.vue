<template>
  <ReaderLayout><section class="form-card">
    <h1>注册</h1>
    <form @submit.prevent="submit">
      <div class="form-row"><label>手机号</label><input v-model.trim="form.username" class="form-input" required /></div>
      <div class="form-row"><label>密码</label><input v-model="form.password" class="form-input" type="password" required /></div>
      <div class="form-row"><label>验证码</label><div class="captcha-row"><input v-model.trim="form.captchaCode" class="form-input" required /><img class="captcha-image" :src="captcha.image" alt="验证码" @click="loadCaptcha" /></div></div>
      <button class="primary-button" type="submit" :disabled="loading">{{ loading ? '注册中…' : '注册并登录' }}</button>
    </form>
    <p class="muted">已有账号？<router-link to="/login">去登录</router-link></p>
  </section></ReaderLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCaptcha, register } from '@/api/auth'
import { setToken, setUid, setUser } from '@/utils/auth'
import ReaderLayout from '@/components/common/ReaderLayout.vue'
const router = useRouter(); const loading = ref(false); const captcha = reactive({ key: '', image: '' })
const form = reactive({ username: '', password: '', captchaCode: '' })
const loadCaptcha = async () => { const { data } = await getCaptcha(); captcha.key = data.captchaKey; captcha.image = data.image }
const submit = async () => {
  loading.value = true
  try {
    const { data } = await register({
      username: form.username,
      password: form.password,
      captchaKey: captcha.key,
      captchaCode: form.captchaCode,
    })
    setToken(data.token)
    setUid(data.uid || data.user?.id)
    setUser(data.user)
    router.push('/books')
  } finally { loading.value = false }
}
onMounted(loadCaptcha)
</script>
