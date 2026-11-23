<template>
  <view class="screen auth-screen">
    <text class="eyebrow">山崖书院 · 创建账户</text><text class="page-title">加入书院</text>
    <view class="field"><text class="field-label">用户名</text><input v-model.trim="username" class="field-input" placeholder="设置用户名" /></view>
    <view class="field"><text class="field-label">密码</text><input v-model="password" class="field-input" type="password" placeholder="设置密码" /></view>
    <view class="field"><text class="field-label">确认密码</text><input v-model="confirm" class="field-input" type="password" placeholder="再次输入密码" /></view>
    <view class="field"><text class="field-label">验证码</text><view class="captcha-row"><input v-model.trim="captchaCode" class="field-input" placeholder="输入图中字符" /><image v-if="captcha.image" :src="captcha.image" mode="aspectFit" @click="loadCaptcha" /><text v-else @click="loadCaptcha">重新获取</text></view></view>
    <button class="primary-btn full" :disabled="busy || !username || !password || !captchaCode" @click="submit">{{ busy ? '注册中…' : '注册并登录' }}</button>
    <text class="back-link" @click="uni.navigateBack()">已有账户？去登录</text>
  </view>
</template>
<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { normalizeCaptcha, saveSession, showError } from '../../utils/request.js'
const username = ref(''), password = ref(''), confirm = ref(''), captchaCode = ref(''), captcha = ref({}), busy = ref(false), redirect = ref('')
onLoad((options) => { redirect.value = options.redirect ? decodeURIComponent(options.redirect) : ''; loadCaptcha() })
async function loadCaptcha() { try { captcha.value = normalizeCaptcha(await api.captcha()) } catch (error) { showError(error) } }
async function submit() {
  if (password.value !== confirm.value) return showError(new Error('两次输入的密码不一致'))
  if (busy.value) return
  busy.value = true
  try {
    const result = await api.register({ username: username.value, password: password.value, captchaKey: captcha.value.key, captchaCode: captchaCode.value })
    if (!result?.token) throw new Error('注册响应中没有 token')
    saveSession(result)
    uni.redirectTo({ url: redirect.value || '/pages/me/index' })
  } catch (error) { showError(error); captchaCode.value = ''; loadCaptcha() }
  finally { busy.value = false }
}
</script>
<style scoped>.auth-screen { max-width: 620px; padding-top: 65rpx; }.full { width: 100%; }.captcha-row { display: flex; align-items: center; gap: 12rpx; }.captcha-row input { flex: 1; min-width: 0; }.captcha-row image { width: 225rpx; height: 84rpx; background: #fff; }.captcha-row text { color: #ae4938; font-size: 23rpx; }.back-link { display: block; margin-top: 35rpx; color: #ae4938; text-align: center; font-size: 25rpx; }</style>
