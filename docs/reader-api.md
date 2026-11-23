# 小说阅读端接口设计（字段对齐版）

版本：`1.3`  
适用前端：`novel-front-web@3.4.0`  
数据库：MySQL `novel`  
接口前缀：`/front`

本版本以 `docs/reader-database.mysql.sql`、`docs/reader-private-import-migration.mysql.sql`、`docs/reader-publication-migration.mysql.sql` 和现有后端 Java POJO 为字段来源，并记录当前后端实际已实现的接口。

## 1. 地址、鉴权与实现状态

开发环境浏览器请求使用 `/api` 前缀：

```text
/api/front/books/home
```

Vue 代理转发到：

```text
http://localhost:8080/front/books/home
```

正式用户接口携带：

```http
Authorization: <JWT>
```

当前后端已实现并可对接的接口：

- `POST /front/auth/register`
- `POST /front/auth/login`
- `GET /front/auth/captcha`
- `GET /front/books/home`、`GET /front/categories`、`GET /front/books`、`GET /front/ranks/{type}`
- `GET /front/books/{bookId}`、`GET /front/books/{bookId}/chapters`、`GET /front/chapters/{chapterId}`、`POST /front/books/{bookId}/visit`
- 评论查询、发表、回复和本人删除接口（`/front/books/**/comments`、`/front/me/**/comments`）
- `GET/PUT /front/me/me`
- 书架、阅读历史、阅读进度和关注接口（`/front/me/**`）

退出登录没有后端接口，前端只清除 `localStorage.Authorization`、`localStorage.reader_uid` 和 `localStorage.reader_user`。
关注更新通过读取 `GET /front/me/following` 或作品详情时返回的 `hasUpdate` 判断；当前为站内提醒，不包含 WebSocket、短信或推送通知。

## 2. 公共响应

```json
{
  "code": "00000",
  "data": {},
  "message": "成功"
}
```

错误码：

| code | 含义 |
| --- | --- |
| `A0001` | 用户名或密码错误 |
| `A0002` | 验证码错误 |
| `A0003` | 用户名已存在 |
| `A0230` | 登录过期或未登录 |
| `A0400` | 参数校验失败 |
| `B0003` | 资源不存在 |

分页请求统一使用 `page`、`size`，默认 `page=1,size=20`。分页 `data`：

```json
{
  "list": [],
  "page": 1,
  "size": 20,
  "total": 0
}
```

## 3. 字段映射规则

数据库使用下划线字段，MyBatis 开启下划线转驼峰，因此接口 JSON 使用后端 POJO 的 camelCase 字段。

### 3.1 用户字段

| 数据库字段 | 标准 JSON/前端字段 | 现有后端兼容字段 |
| --- | --- | --- |
| `id` | `id` | `uid` 仅出现在登录响应 |
| `username` | `username` | — |
| `nick_name` | `nickName` | 登录摘要还可能返回 `nickname` |
| `user_photo` | `userPhoto` | 资料 VO 当前拼写为 `userPhotp`；登录摘要还可能返回 `avatar` |
| `user_sex` | `userSex` | — |
| `user_birthday` | `userBirthday` | 当前资料 VO 可能不返回 |
| `user_phone` | `userPhone` | 当前资料 VO 可能不返回 |
| `user_email` | `userEmail` | 当前资料 VO 当前字段为 `email` |
| `status` | `status` | — |
| `create_time` | `createTime` | — |
| `update_time` | `updateTime` | — |

前端 `src/utils/reader-fields.js` 负责把 `nickname/avatar/userPhotp/email` 兼容转换为标准字段；页面只使用标准字段。

标准用户对象：

```json
{
  "id": 1,
  "username": "13800138000",
  "nickName": "书友",
  "userPhoto": null,
  "userSex": 0,
  "userBirthday": null,
  "userPhone": null,
  "userEmail": null,
  "status": 0,
  "createTime": "2026-09-21 10:00:00",
  "updateTime": "2026-09-21 10:00:00"
}
```

### 3.2 书籍、章节与用户记录

书籍字段：

```json
{
  "id": 1,
  "bookName": "示例小说",
  "authorName": "作者",
  "categoryId": 1,
  "categoryName": "玄幻",
  "picUrl": "/cover/1.jpg",
  "bookDesc": "作品简介",
  "bookStatus": 0,
  "wordCount": 120000,
  "visitCount": 3200,
  "chapterCount": 100,
  "firstChapterId": 1,
  "lastChapterId": 100,
  "lastChapterName": "第一百章",
  "lastChapterUpdateTime": "2026-09-19 20:00:00",
  "status": 0,
  "createTime": "2026-09-01 12:00:00",
  "updateTime": "2026-09-19 20:00:00"
}
```

章节字段：

```json
{
  "id": 100,
  "bookId": 1,
  "chapterNum": 100,
  "chapterName": "第一百章",
  "wordCount": 3200,
  "updateTime": "2026-09-19 20:00:00",
  "createTime": "2026-09-19 20:00:00"
}
```

书架条目严格使用 `user_bookshelf` 语义：

```json
{
  "book": {},
  "lastChapterId": 100,
  "lastChapterName": "第一百章",
  "lastReadAt": "2026-09-19 20:00:00",
  "createTime": "2026-09-01 12:00:00",
  "updateTime": "2026-09-19 20:00:00"
}
```

历史条目严格使用 `user_read_history` 语义：

```json
{
  "book": {},
  "chapterId": 100,
  "chapterName": "第一百章",
  "readAt": "2026-09-19 20:00:00",
  "createTime": "2026-09-01 12:00:00",
  "updateTime": "2026-09-19 20:00:00"
}
```

### 3.3 评论字段与数据库表

评论数据来自 `book_comment`，用户昵称和头像通过 `sys_user` 关联查询：

| 数据库字段 | JSON/前端字段 | 说明 |
| --- | --- | --- |
| `id` | `id` | 评论 ID |
| `book_id` | 由作品路径确定 | 不重复放入评论对象 |
| `user_id` | `userId` | 仅用于展示和本人删除判断 |
| `parent_comment_id` | `parentCommentId` | 空值为主评论，非空值为一级回复 |
| `content` | `content` | 最多 500 字 |
| `status` | `deleted` | `status=1` 转为 `true` |
| `create_time` | `createTime` | 发表时间 |
| `update_time` | `updateTime` | 修改时间 |
| `sys_user.nick_name` | `nickName` | 评论者昵称 |
| `sys_user.user_photo` | `userPhoto` | 评论者头像 |

主评论额外返回 `replyCount`；回复额外返回 `replyToNickName`。删除采用软删除，主评论始终返回 `[评论已删除]` 占位，正常回复继续保留。评论表由 `reader-database.mysql.sql` 创建，已有数据库使用 `reader-comment-migration.mysql.sql` 增量迁移。

## 4. 公开阅读接口

### `GET /front/books/home`

返回 `latest`、`hot`、`updated` 三组书籍。旧实现可能使用 `recentlyUpdated`，前端适配层会转换为 `updated`。

响应 `data`：

```json
{
  "latest": [],
  "hot": [],
  "updated": []
}
```

状态：后端已实现。

### `GET /front/categories`

查询参数：`direction`，`0` 男生分类，`1` 女生分类。

响应 `data`：

```json
[
  { "id": 1, "categoryName": "玄幻", "direction": 0, "sort": 10, "status": 0 }
]
```

状态：后端已实现。

### `GET /front/books`

查询参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `q` | string | 书名或作者关键词 |
| `categoryId` | number | 分类 ID |
| `status` | number | `0` 连载中，`1` 已完结；对应 `book_info.book_status` |
| `sort` | string | `visit`、`update`、`new` |
| `page` | number | 默认 1 |
| `size` | number | 默认 20 |

响应 `data` 为分页对象，`list` 元素使用书籍标准字段。

状态：后端已实现。

### `GET /front/ranks/{type}`

`type`：`visit` 阅读榜、`update` 更新榜、`new` 新书榜。

响应 `data` 为书籍数组。

状态：后端已实现。

### `GET /front/books/{bookId}`

响应 `data` 为书籍标准对象。

状态：后端已实现。

### `GET /front/books/{bookId}/chapters`

查询参数：`page`、`size`。响应 `data` 为章节分页对象。

状态：后端已实现。

### `GET /front/chapters/{chapterId}`

响应 `data`：

```json
{
  "book": {
    "id": 1,
    "bookName": "示例小说",
    "authorName": "作者"
  },
  "chapter": {},
  "content": "章节正文",
  "previousChapterId": 99,
  "nextChapterId": 101
}
```

所有章节免费，不返回 VIP、订阅或支付字段。

状态：后端已实现。

### `POST /front/books/{bookId}/visit`

游客可调用，成功时 `data=null`。

状态：后端已实现。

### `GET /front/books/{bookId}/comments?page=1&size=20`

游客可访问。返回作品的主评论，按发表时间倒序分页。软删除后的主评论会保留为 `[评论已删除]` 占位。

响应 `data`：

```json
{
  "list": [
    {
      "id": 12,
      "userId": 1,
      "parentCommentId": null,
      "nickName": "书友",
      "userPhoto": null,
      "replyToNickName": null,
      "content": "这一章很精彩",
      "replyCount": 2,
      "deleted": false,
      "createTime": "2026-10-15 20:00:00",
      "updateTime": "2026-10-15 20:00:00"
    }
  ],
  "page": 1,
  "size": 20,
  "total": 1
}
```

状态：后端已实现。

### `GET /front/books/{bookId}/comments/{commentId}/replies?page=1&size=20`

游客可访问。返回指定主评论下的一级回复，按发表时间正序分页。回复不能再继续嵌套回复。

状态：后端已实现。

## 5. 认证接口

### `GET /front/auth/captcha`

现有后端返回字段：

```json
{
  "code": "00000",
  "data": {
    "img": "base64原文",
    "sessionId": "captcha-session-id",
    "captchaKey": "captcha-session-id",
    "image": "data:image/png;base64,..."
  },
  "message": "成功"
}
```

前端统一使用 `captchaKey` 和 `image`，同时兼容 `img/sessionId`。

状态：后端已实现。

### `POST /front/auth/register`

请求体严格对应 `UserRegisterDTO`：

```json
{
  "username": "13800138000",
  "password": "******",
  "captchaKey": "captcha-session-id",
  "captchaCode": "1234"
}
```

成功响应 `data` 对应 `UserLoginVO`：

```json
{
  "token": "jwt-token",
  "uid": 1,
  "nickName": "书友",
  "user": {
    "id": 1,
    "username": "13800138000",
    "nickname": "书友",
    "avatar": null
  }
}
```

前端将 `user.nickname/avatar` 适配为 `nickName/userPhoto`。

状态：后端已实现。

### `POST /front/auth/login`

请求体对应 `UserLoginDTO`：

```json
{
  "username": "13800138000",
  "password": "******"
}
```

响应结构同注册。

状态：后端已实现。

## 6. 正式用户接口

所有接口需要 `Authorization`。

### `GET /front/me/me`

当前后端实际资料查询路径。响应 `data` 为 `UserInfoVO`，前端统一转换为标准用户对象。

当前后端可能返回：

```json
{
  "id": 1,
  "username": "13800138000",
  "nickName": "书友",
  "userPhotp": null,
  "userSex": 0,
  "email": null,
  "createTime": "2026-09-21 10:00:00"
}
```

前端转换后使用 `userPhoto`、`userEmail`。

状态：后端已实现。

### `PUT /front/me/me`

请求体使用数据库语义对应的 POJO camelCase 字段：

```json
{
  "nickName": "新昵称",
  "userPhoto": "/avatar/1.jpg",
  "userSex": 0,
  "userBirthday": "2000-01-01",
  "userPhone": "13800138000",
  "userEmail": "reader@example.com"
}
```

状态：后端已实现。

### `GET /front/me/bookshelf`

响应 `data` 为书架条目数组，每本书只保留一条记录。

状态：后端已实现。

### `PUT /front/me/bookshelf/{bookId}`

请求体：

```json
{ "chapterId": 100 }
```

重复加入同一本书时更新原记录。

状态：后端已实现。

### `DELETE /front/me/bookshelf/{bookId}`

移出书架。

状态：后端已实现。

### `GET /front/me/history?page=1&size=20`

响应 `data` 为分页对象，列表元素严格使用 `chapterId`、`chapterName`、`readAt`，不再把书架字段作为历史字段。

状态：后端已实现。

### `PUT /front/me/progress`

请求体：

```json
{
  "bookId": 1,
  "chapterId": 100
}
```

服务端更新 `user_read_history`；如果书籍已在书架中，同时更新 `user_bookshelf`。游客不会产生服务端历史。

状态：后端已实现。

### `GET /front/me/following`

获取当前用户关注的上架作品。结果按“有更新优先、最新章节更新时间、关注时间”排序。

响应 `data` 为数组，元素字段：

```json
{
  "book": {
    "id": 1,
    "bookName": "示例小说",
    "authorName": "作者",
    "lastChapterId": 101,
    "lastChapterName": "第二章"
  },
  "followedAt": "2026-10-12 20:00:00",
  "lastNotifiedChapterId": 100,
  "lastNotifiedAt": "2026-10-12 20:00:00",
  "hasUpdate": true
}
```

`hasUpdate=true` 表示作品当前最新章节 ID 与用户上次确认提醒的章节 ID 不同。

状态：后端已实现。

### `PUT /front/me/following/{bookId}`

关注作品。首次关注时将当前最新章节记为已确认，避免刚关注就提示已有更新；重复关注不会重置已有提醒状态。

状态：后端已实现。

### `DELETE /front/me/following/{bookId}`

取消关注作品。

状态：后端已实现。

### `PUT /front/me/following/{bookId}/read`

将作品的更新提醒标记为已读，并把当前最新章节写入 `lastNotifiedChapterId`。

状态：后端已实现。

### `POST /front/me/books/{bookId}/comments`

需要 `Authorization`。不传 `parentCommentId` 发表主评论；传入同一作品下的主评论 ID 时发表一级回复。评论内容会去除首尾空白，长度限制为 1 至 500 字。

请求体：

```json
{
  "content": "我也很喜欢这一章",
  "parentCommentId": 12
}
```

成功时返回 `data=null`。后端从 JWT 获取用户 ID，不接受客户端传入的用户 ID。

状态：后端已实现。

### `DELETE /front/me/comments/{commentId}`

需要 `Authorization`。只能删除当前登录用户自己的正常评论，删除采用软删除；主评论会保留占位，回复继续保留。

成功时返回 `data=null`。

状态：后端已实现。

## 7. 前端封装对应关系

| 文件 | 接口 |
| --- | --- |
| `src/api/auth.js` | 验证码、注册、登录 |
| `src/api/books.js` | 首页、分类、书籍、章节和阅读接口 |
| `src/api/me.js` | 资料、书架、历史、进度、评论发表和删除 |
| `src/api/imports.js` | 私人导入、公开投稿、撤回申请 |
| `novel-front-manger/src/api/review.js` | 管理员登录、审核、下架、日志 |
| `src/components/book/CommentSection.vue` | 评论列表、一级回复、分页和评论编辑 |
| `src/utils/reader-fields.js` | POJO、历史和评论兼容字段统一转换 |
| `src/utils/auth.js` | token、uid、标准用户本地存储 |

关注相关接口也由 `src/api/me.js` 封装，前端关注列表和作品详情通过 `hasUpdate` 显示更新提醒。新章节发布后，后端读取 `book_info.last_chapter_id` 与用户已确认章节进行比较；书籍数据写入流程需要同步维护最新章节字段。

## 8. 验收路径

游客：

```text
/ → /books → /book/:bookId → /book/:bookId/chapters → /read/:bookId/:chapterId
```

正式用户：

```text
注册/登录 → /me/me → 修改资料 → 加入书架 → 阅读 → 同步进度 → 查看书架/历史
```

前端页面不得直接依赖以下兼容字段：

```text
nickname、avatar、gender、email、userPhotp、lastChapterName（历史）、lastReadAt（历史）
```

## 9. 私人导入与公开投稿

投稿和管理接口的 Apifox 可导入定义见 [`reader-publication.openapi.json`](reader-publication.openapi.json)。所有响应仍为 `{code,data,message}`；除管理员登录外，鉴权请求头均为 `Authorization: <JWT>`，不加 `Bearer`。普通用户 JWT 与管理员 JWT 分离，不能互用。

`POST /front/me/imports` 仍接受旧的仅含 `file` 的私人导入；新增 `multipart/form-data` 字段 `visibility=PRIVATE|PUBLIC`。选择 `PUBLIC` 时还要提交 `bookName`、`authorName`、`categoryId`、`bookDesc`（可选）、`rightsConfirmed=true`。文件仅支持最大 20 MB 的 TXT。上传后先在私人书库生成原件，公开投稿状态为 `PENDING`；未审核通过前不会进入公开书库。

私人作品摘要增加 `visibility`、`authorName`、`categoryId`、`bookDesc`、`rightsConfirmed`、`submissionId`、`submissionStatus`、`rejectionReason`、`publicBookId`、`publicStatus`。没有投稿时 `visibility=PRIVATE`，投稿字段为 `null`。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/front/me/imports/{novelId}/resubmit` | 被退稿或已取消的作品重新投稿；表单资料必填，可选 `file` 替换 TXT。替换后私人阅读进度清空 |
| POST | `/front/me/imports/{novelId}/cancel-submission` | 取消待审核投稿 |
| GET | `/front/me/publications` | 本人审核通过的公开副本；私人原件删除后仍可列出 |
| POST | `/front/me/publications/{bookId}/withdrawals` | 申请撤回公开副本，JSON `{ "reason": "可选原因" }`；等待审核期间继续公开 |

管理员站点默认运行在 `http://127.0.0.1:1025`，请求经 `/api` 代理到 8080。管理员账号首次启动时由 `NOVEL_ADMIN_BOOTSTRAP_USERNAME` 和 `NOVEL_ADMIN_BOOTSTRAP_PASSWORD` 创建，密码存 BCrypt 哈希；管理员 JWT 密钥可用 `NOVEL_ADMIN_JWT_SECRET` 设置。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/admin/auth/login` | 管理员登录，JSON `{username,password}`，返回 `{token,id,username}` |
| GET | `/admin/submissions?status=PENDING&page=1&size=20` | 投稿分页；状态可为 `PENDING/REJECTED/APPROVED/CANCELLED` |
| GET | `/admin/submissions/{id}` | 投稿资料与审核状态 |
| GET | `/admin/submissions/{id}/chapters` | 原件章节目录，最多 5000 章 |
| GET | `/admin/submissions/{id}/chapters/{chapterId}` | 原件章节正文预览 |
| POST | `/admin/submissions/{id}/approve` | 审核通过；JSON 可选修订字段 `bookName/authorName/categoryId/bookDesc`；返回公开书籍 ID |
| POST | `/admin/submissions/{id}/reject` | 退稿，JSON `{reason}`，原因必填 |
| GET | `/admin/withdrawals?status=PENDING&page=1&size=20` | 撤回申请分页；另有 `APPROVED/REJECTED/CLOSED` 状态 |
| POST | `/admin/withdrawals/{id}/approve` | 批准撤回并下架公开副本 |
| POST | `/admin/withdrawals/{id}/reject` | 驳回撤回，JSON `{reason}`，原因必填 |
| POST | `/admin/publications/{bookId}/take-down` | 管理员主动下架，JSON `{reason}`，原因必填；未处理的撤回申请转为 `CLOSED` |
| GET | `/admin/audit-logs?page=1&size=20` | 审核操作日志分页 |

审核通过时服务端在同一事务中复制私人章节到 `book_chapter` / `book_content`，并创建 `book_info`；原件和公开副本随后独立。删除私人原件不会删除已发布副本。撤回申请只有管理员批准后才下架；已下架作品的详情、目录和正文公开接口返回 `B0003`。审核状态变化后重新请求列表，避免使用旧状态重复提交。
