# 山崖书院用户端 App

uni-app Vue 3 阅读端，对接同一工作区的 Spring Boot 后端。包含公开书库、搜索与榜单、章节阅读、评论、登录注册、个人资料、书架、阅读历史、关注更新，以及私人 TXT 导入和公开投稿。

首页的山间书房插画由 `tools/make-home-scene.py` 生成并保存在 `static/home/`，运行 App 不需要 Python。画面可手动暂停，离开首页自动停止；H5 系统设置为减少动态效果时不播放动画。全局页面样式在 `App.vue`，正文页另有独立日夜主题。

首次打开 App 且尚未登录时，只弹出一次“登录 / 游客阅读”选择；游客选择会保存在本机。公开书库、榜单、作品详情与正文无需登录。书架、历史、关注和私人作品页面在游客访问时显示登录入口，不会自动反复跳转。

## 本地运行

要求 Node.js 22.12+、后端运行于 `http://127.0.0.1:8080`。在 Windows PowerShell 中进入本目录后执行：

```powershell
npm install
npm run dev:h5
```

H5 开发请求使用 `/api`，由 Vite 转发到本机后端 `8080`。终端会显示实际预览端口；若 `1026` 被占用，Vite 会使用下一个可用端口。

```powershell
npm run build:h5
npm run build:app
```

App 资源构建产物位于 `dist/build/app`，需要在 HBuilderX 中运行或打包。首次发行前须在 `manifest.json` 配置自己的 DCloud AppID，并在真机上检查安装包与网络权限。

## 连接后端

- Android 模拟器默认使用 `http://10.0.2.2:8080`。
- Android 真机在“我的 → 服务器地址”中填写电脑的局域网地址，例如 `http://192.168.1.10:8080`。手机和电脑须处于同一网络，后端须监听可被手机访问的地址。
- H5 固定走本地 `/api` 代理，App 中的服务器地址设置不影响 H5。
- 生产环境建议改为 HTTPS，并配置正式域名。当前 HTTP 明文设置只供本地开发联调。

## 私人作品与投稿

“我的导入”支持非空且不超过 20 MB 的 TXT。私人作品只对当前账户可见；选择“投稿公开阅读”时还需填写书名、笔名、分类和发布权确认，提交后等待管理端审核。审核退回或取消后可选择新 TXT 修改重投；已公开作品可在“我的公开”申请撤回。

H5 使用 `uni.chooseFile`。Android App 使用系统文档选择器读取 TXT 并复制到应用缓存，再通过 `uni.uploadFile` 上传；这部分需要在 Android 模拟器或真机进一步验收。iOS 原生文件选择尚未实现，iOS 用户可先通过 H5 导入，同一账户可在 App 续读。

## 接口约定

请求封装在 `utils/request.js`，接口路径集中在 `api/index.js`。所有接口期望 `{ code, data, message }`，成功码为 `00000`。登录后 JWT 原样放入 `Authorization` 请求头。后端接口的详细字段和业务规则以工作区的 `docs/development-guide.md` 为准。
