# 小说封面接入阿里云 OSS

## 行为

- PC 和 App 导入 TXT 时可选择一张封面；不选、图片加载失败时显示原有书名文字封面。
- 封面仅支持 JPG、PNG，文件不超过 2 MB，宽高均为 100-4000 像素。后端会实际解码图片，不信任文件扩展名和客户端 MIME。
- 私人作品封面保存在 `covers/private/`，私人书库和管理审核接口只返回 10 分钟有效的签名读取地址。作品正文仍由原有 JWT 权限控制。
- 管理员审核通过公开投稿时，后端将封面复制到 `covers/public/`，并将公开地址写入 `book_info.pic_url`。公开书库只读取审核通过的公开封面。
- 数据库保存原始对象地址；重新投稿时选新封面会替换投稿快照，不选择则沿用旧封面。

## 数据库

首次部署先执行 [reader-cover-migration.mysql.sql](reader-cover-migration.mysql.sql)，给 `user_novel` 和 `novel_submission` 增加 `cover_url`。现有 `book_info.pic_url` 继续用于公开作品，无需重建表。

## OSS 配置

创建一个 OSS Bucket，保持默认私有读写。使用 Bucket Policy **只**允许匿名读取 `covers/public/*`；`covers/private/*` 不应开放匿名读取。给后端 RAM 用户授予该 Bucket 的 `PutObject`、`GetObject`、`DeleteObject` 权限，并将权限范围限制到 `covers/*`。审核发布时还需要能读取私有源对象并复制到公开前缀。

在启动 Spring Boot 的环境中配置：

```powershell
$env:NOVEL_OSS_ENDPOINT = 'https://oss-cn-hangzhou.aliyuncs.com'
$env:NOVEL_OSS_BUCKET = 'your-bucket-name'
$env:NOVEL_OSS_ACCESS_KEY_ID = '<RAM AccessKey ID>'
$env:NOVEL_OSS_ACCESS_KEY_SECRET = '<RAM AccessKey Secret>'
$env:NOVEL_OSS_PUBLIC_BASE_URL = 'https://your-bucket-name.oss-cn-hangzhou.aliyuncs.com'
```

`ENDPOINT` 和 Bucket 必须在同一区域。`PUBLIC_BASE_URL` 可以是该 Bucket 的 HTTPS 访问域名或已配置的 CDN 域名，末尾不要带路径。AccessKey 只放在后端运行环境，不写入前端、数据库脚本或版本库。没有配置 OSS 时，不带封面的导入照常运行；带封面的请求返回明确的配置错误。

上传经过后端，不需要浏览器直传 OSS 的 CORS 配置。App 若使用域名白名单，需要把签名地址使用的 OSS 域名和公开封面域名加入图片下载白名单。

## 接口字段

`POST /front/me/imports` 与 `POST /front/me/imports/{novelId}/resubmit` 均为 `multipart/form-data`：

| 字段 | 用途 |
| --- | --- |
| `file` | TXT 内容；首次导入必填，重新投稿沿用现有规则 |
| `cover` | 可选 JPG/PNG 封面 |
| 其他字段 | 沿用原有可见性、书名、笔名、分类、简介和发布权声明 |

私人作品、投稿审核和公开书籍响应都使用 `picUrl` 作为展示地址。封面上传失败会使本次导入或重新投稿失败；数据库事务回滚时会清理本次新上传的 OSS 对象。

## 验收

1. 无封面导入后，PC 与 App 均显示书名文字封面。
2. 有封面的私人导入在本人书库可见，退出登录后无法调用私人书库接口，匿名访问私有 OSS 对象被拒绝。
3. 带封面投稿在管理端审核详情可预览；通过后，公开书库、详情、榜单、书架和历史展示图片。
4. 故意让图片 URL 加载失败，前端回退书名文字封面。

尚未配置真实 Bucket 与 RAM 凭证时，只能验证代码构建和本地接口逻辑，不能验证 OSS 网络读写。旧封面对象在作品删除或重投时仍保留，后续应按对象创建时间和数据库引用定期清理。
