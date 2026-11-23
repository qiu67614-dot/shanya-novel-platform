# 读者站 · novel-front-web

山崖书院的 PC 读者端。Vue 3 + Vue CLI 5 + Element Plus，构建产物由 Nginx 托管，接口通过 `/api` 同域反向代理到后端。

## 与上游的关系

本目录的页面骨架基于开源项目 [novel-front-web](https://gitee.com/novel_dev_team/novel-front-web)（Apache License 2.0）二次开发：

- 首页、阅读页、书架、个人中心、TXT 导入等页面已重写或大幅修改；
- 作家后台、新闻资讯、充值等未实现模块已移除；
- 原始 README 保留在 [`UPSTREAM-README.md`](./UPSTREAM-README.md)，许可证保留在 [`LICENSE`](./LICENSE)，二者均未被修改。

对上游文件的修改遵循 Apache-2.0 第 4 条：保留原版权与许可声明，并对修改过的文件作说明。

## 本地开发

```bash
npm install
npm run serve     # 开发服务器
npm run lint
npm run build     # 产物输出到 dist/
```

接口地址由 `.env.development` / `.env.production` 控制，生产环境应为 `/api`（同域代理），不要写死 `localhost`。

## 目录

```
src/
├── api/          # 接口封装
├── assets/       # 样式与图片
├── components/   # 通用组件
├── router/       # 路由
├── utils/        # request 封装、工具函数
└── views/        # 页面
```

## 部署

见仓库根目录 [`deploy/ubuntu/README.md`](../deploy/ubuntu/README.md)。
