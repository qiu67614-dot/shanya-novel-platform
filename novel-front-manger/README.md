# 山崖书院内容管理

独立的 Vue 3 管理站点，用于审核读者投稿、处理撤回申请、主动下架公开作品及查看审核日志。

## 启动

先在 MySQL `novel` 库执行 [`../docs/reader-publication-migration.mysql.sql`](../docs/reader-publication-migration.mysql.sql)。已有数据库应先备份；不要重跑会删除数据的主建库脚本。

首次启动后端前设置环境变量：

```powershell
$env:NOVEL_ADMIN_BOOTSTRAP_USERNAME = '你的管理员账号'
$env:NOVEL_ADMIN_BOOTSTRAP_PASSWORD = '你的强密码'
$env:NOVEL_ADMIN_JWT_SECRET = '至少 32 字节的随机密钥'
```

后端首次启动时创建独立管理员账号，密码以 BCrypt 保存。账号已存在时不会重置密码。以后每次启动后端仍须提供同一个 `NOVEL_ADMIN_JWT_SECRET`，否则之前签发的管理员 token 失效；不要提交密钥到仓库。

后端默认监听 `8080`。本目录运行：

```powershell
npm install
npm run dev
```

打开 `http://127.0.0.1:1025/`。管理端通过 `/api` 代理到本地后端。执行 `npm run lint` 和 `npm run build` 可检查代码。接口定义见 [`../docs/reader-publication.openapi.json`](../docs/reader-publication.openapi.json)，可直接导入 Apifox。
