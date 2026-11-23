<template>
  <view class="screen"><text class="eyebrow">账户</text><text class="page-title">个人资料</text>
    <GuestGate v-if="!authenticated" title="个人资料" redirect="/pages/me/profile" />
    <view v-else-if="loading" class="state">正在加载资料…</view><template v-else>
    <view class="field"><text class="field-label">用户名</text><text class="read-only">{{ user.username }}</text></view>
    <view class="field"><text class="field-label">昵称</text><input v-model.trim="user.nickName" class="field-input" maxlength="50" placeholder="怎么称呼你" /></view>
    <view class="field"><text class="field-label">头像地址</text><input v-model.trim="user.userPhoto" class="field-input" maxlength="255" placeholder="图片网址（可选）" /></view>
    <view class="field"><text class="field-label">性别</text><picker :range="sexes" :value="Number(user.userSex) || 0" @change="user.userSex = Number($event.detail.value)"><view class="field-input">{{ sexes[Number(user.userSex) || 0] }}　⌄</view></picker></view>
    <view class="field"><text class="field-label">生日</text><picker mode="date" :value="user.userBirthday || '2000-01-01'" @change="user.userBirthday = $event.detail.value"><view class="field-input">{{ user.userBirthday || '选择日期' }}　⌄</view></picker></view>
    <view class="field"><text class="field-label">手机号</text><input v-model.trim="user.userPhone" class="field-input" maxlength="11" type="number" placeholder="可选" /></view>
    <view class="field"><text class="field-label">邮箱</text><input v-model.trim="user.userEmail" class="field-input" type="text" placeholder="可选" /></view>
    <button class="primary-btn save" :disabled="saving" @click="submit">{{ saving ? '保存中…' : '保存资料' }}</button></template>
  </view>
</template>
<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { getToken, normalizeUser, saveUser, showError } from '../../utils/request.js'
import GuestGate from '../../components/GuestGate.vue'
const user = ref({}), loading = ref(false), saving = ref(false), sexes = ['未设置', '男', '女']
const authenticated = ref(false)
onLoad(async () => { authenticated.value = Boolean(getToken()); if (!authenticated.value) return; loading.value = true; try { user.value = normalizeUser(await api.me()) } catch (error) { authenticated.value = Boolean(getToken()); if (authenticated.value) showError(error) } finally { loading.value = false } })
async function submit() {
  saving.value = true
  try { await api.updateMe({ nickName: user.value.nickName, userPhoto: user.value.userPhoto, userSex: user.value.userSex, userBirthday: user.value.userBirthday || null, userPhone: user.value.userPhone, userEmail: user.value.userEmail }); saveUser(user.value); uni.showToast({ title: '已保存', icon: 'none' }) }
  catch (error) { showError(error) }
  finally { saving.value = false }
}
</script>
<style scoped>.read-only { display: block; min-height: 86rpx; padding: 20rpx 0; border-bottom: 1rpx solid #dce3dd; color: #67756b; }.save { width: 100%; margin-top: 22rpx; }</style>
