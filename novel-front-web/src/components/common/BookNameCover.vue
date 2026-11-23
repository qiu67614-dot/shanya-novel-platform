<template>
  <div class="book-name-cover" :class="[coverTone, { 'book-name-cover--image': coverUrl && !imageFailed }]" :title="title" role="img" :aria-label="title ? title + '的封面' : '未命名作品的封面'">
    <img v-if="coverUrl && !imageFailed" class="cover-image" :src="coverUrl" :alt="title + '封面'" @error="imageFailed = true" />
    <template v-else>
      <span class="cover-line cover-line--top" aria-hidden="true"></span>
      <span class="cover-title">{{ title || '未命名作品' }}</span>
      <span class="cover-line cover-line--bottom" aria-hidden="true"></span>
    </template>
  </div>
</template>
<script>
export default {
  name: 'BookNameCover',
  props: { title: { type: String, default: '' }, src: { type: String, default: '' } },
  data() { return { imageFailed: false } },
  watch: { src() { this.imageFailed = false } },
  computed: {
    coverUrl() {
      if (!this.src) return ''
      if (/^(https?:\/\/|blob:|data:image\/)/i.test(this.src)) return this.src
      return `${process.env.VUE_APP_BASE_API_URL || '/api'}/${this.src.replace(/^\/+/, '')}`
    },
    coverTone() {
      const sum = [...(this.title || '')].reduce((value, char) => value + char.charCodeAt(0), 0)
      return 'cover-tone-' + (sum % 5)
    },
  },
}
</script>
<style scoped>
.cover-image { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.book-name-cover--image::before, .book-name-cover--image::after { display: none; }
</style>
