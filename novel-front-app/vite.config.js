import { defineConfig } from 'vite'
import uniPlugin from '@dcloudio/vite-plugin-uni'

const uni = uniPlugin.default || uniPlugin

export default defineConfig({
  plugins: [uni()],
  server: {
    host: '127.0.0.1',
    port: 1026,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
