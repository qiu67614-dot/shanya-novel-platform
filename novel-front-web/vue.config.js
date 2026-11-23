const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  devServer:{
    // 端口号的配置
    port:1024,
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 后端springboot地址
        changeOrigin: true,
        pathRewrite: {
          '^/api': ''  // 把请求路径开头的 /api 删掉
        }
      }
    }
  },
  css: {
    sourceMap: true // 开启 CSS source map
  },
  transpileDependencies: true,
  lintOnSave: false
})
