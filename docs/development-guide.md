# 小说阅读项目开发文档

更新日期：2026-11-16。本文以当前 `novel-front-web`、`novel_web/novel` 源码和 `docs` 下的 SQL 为准。旧版前端 `README.md` 描述了上游完整小说平台，其中作家后台、新闻、支付和 VIP 并非当前阅读端的功能。

## 1. 项目概览

项目分为 Vue 3 阅读端、Vue 3 管理端和 Spring Boot 后端。游客可浏览公开作品、目录、正文和评论；登录用户还可使用资料、书架、历史、关注、评论及 TXT 导入。导入时可只供本人阅读，或填写书名、笔名、分类与发布权声明后提交管理员审核。审核通过的公开副本与私人原件独立保存。

```text
novel-front-web/                 Vue 阅读端，默认端口 1024
  src/api/                       Axios 接口封装
  src/router/                     页面路由与登录守卫
  src/views/                      页面
novel-front-manger/              Vue 管理端，默认端口 1025
  src/views/                     投稿审核、撤回申请、已发布作品、操作记录
docs/                            SQL、接口与开发文档
novel_web/novel/                 Maven 多模块后端，默认端口 8080
  novel-pojo/                    Entity、DTO、VO
  novel-common/                  响应、异常、JWT、鉴权、Redis 工具
  novel-service/                 Controller、Service、Mapper XML
```

浏览器请求 `/api/front/books/home`，Vue 开发代理去掉 `/api` 后转发至 `http://localhost:8080/front/books/home`。数据库 `novel` 默认位于 MySQL `3306`，Redis 配置为本机 `6379`。

## 2. 技术选型

| 层 | 当前技术 | 用途 |
| --- | --- | --- |
| 前端 | Vue 3.2、Vue Router 4、Vue CLI 5 | 单页应用、路由与构建 |
| 管理端 | Vue 3.5、Vue Router 4、Vite 6、Element Plus、Lucide | 独立审核工作台 |
| UI/请求 | Element Plus 2.2、Axios 0.27 | 组件、请求拦截与响应处理 |
| 后端 | Java 17、Spring Boot 3.3、Spring Web、Validation | HTTP 服务与参数校验 |
| 数据访问 | MyBatis XML、MyBatis-Plus 3.5.5、MySQL Connector/J | 显式 SQL、实体映射 |
| 持久化 | MySQL 8、InnoDB、utf8mb4 | 公开书籍、用户和私人作品 |
| 认证 | JJWT 0.11.5、Spring Security、BCrypt | JWT、路径鉴权、密码哈希 |
| 缓存 | Redis、Spring Data Redis | 验证码；公开首页、分类、榜单及章节短时缓存 |
| 其他 | Hutool Captcha、Lombok | 图片验证码、减少 Java 样板代码 |

Redis 不可用时，公开阅读缓存会回退到数据库；注册验证码依赖 Redis，因而注册流程需要 Redis 正常运行。MyBatis 配置启用 `map-underscore-to-camel-case`；关联字段通过 SQL 别名映射。数据库字段用 `snake_case`，HTTP JSON 用 Java POJO 的 `camelCase`。

## 3. 数据库设计

数据库为 `novel`。新库初始化脚本是 [`reader-database.mysql.sql`](reader-database.mysql.sql)，私人导入增量脚本是 [`reader-private-import-migration.mysql.sql`](reader-private-import-migration.mysql.sql)，投稿审核增量脚本是 [`reader-publication-migration.mysql.sql`](reader-publication-migration.mysql.sql)。**主脚本会删除并重建原有 9 张表，只能在可丢弃数据的新库执行；已有数据库只执行所需增量脚本。** 评论和关注的独立迁移脚本也保存在本目录，但主脚本已经包含对应表，不能将它们误认为额外的新表。

| 表 | 关键字段 | 作用与约束 |
| --- | --- | --- |
| `sys_user` | `id`, `username`, `password`, `nick_name`, `status` | 正式用户；`username` 唯一，密码存 BCrypt 哈希；游客不入库 |
| `book_category` | `id`, `category_name`, `direction`, `sort`, `status` | 分类；`direction` 为 0 男生、1 女生；`status=0` 启用 |
| `book_info` | `id`, `category_id`, `book_name`, `author_name`, `book_status`, `status`, `first_chapter_id`, `last_chapter_id`, `visit_count` | 公开作品；`book_status` 为 0 连载、1 完结；`status` 为 0 上架、1 下架 |
| `book_chapter` | `id`, `book_id`, `chapter_num`, `chapter_name`, `word_count` | 公开目录；`(book_id, chapter_num)` 唯一 |
| `book_content` | `id`, `chapter_id`, `content` | 公开正文，`chapter_id` 唯一；与目录分表以避免目录查询读取 `LONGTEXT` |
| `user_bookshelf` | `user_id`, `book_id`, `last_chapter_id`, `last_read_at` | 每人每本书一条书架记录；`(user_id, book_id)` 唯一 |
| `user_read_history` | `user_id`, `book_id`, `chapter_id`, `read_at` | 每人每本书一条最新阅读记录；`(user_id, book_id)` 唯一 |
| `book_comment` | `book_id`, `user_id`, `parent_comment_id`, `content`, `status` | 公开作品评论；空 `parent_comment_id` 为主评论，回复挂主评论；删除为软删除 |
| `user_book_follow` | `user_id`, `book_id`, `last_notified_chapter_id` | 关注与站内更新提示；`(user_id, book_id)` 唯一 |
| `user_novel` | `id`, `user_id`, `book_name`, `encoding`, `file_size`, `chapter_count`, `first_chapter_id` | 用户私有 TXT 作品；不写入 `book_info` |
| `user_novel_chapter` | `id`, `novel_id`, `chapter_num`, `chapter_name`, `content` | 私有章节及正文；`(novel_id, chapter_num)` 唯一 |
| `user_novel_progress` | `novel_id`, `user_id`, `chapter_id`, `read_at` | 每部私有作品一条续读记录；`novel_id` 为主键 |
| `admin_user` | `id`, `username`, `password_hash`, `status` | 独立管理员账号，密码为 BCrypt 哈希 |
| `novel_submission` | `user_novel_id`, `user_id`, `visibility`, `status`, `public_book_id`, `public_status` | 公开投稿及审核状态；私人原件删除时外键置空 |
| `novel_withdrawal_request` | `public_book_id`, `user_id`, `status`, `reason` | 已发布作品的撤回申请，管理员审批 |
| `admin_audit_log` | `admin_id`, `target_type`, `target_id`, `action` | 审核、退稿、撤回和下架日志 |

`book_info` 引用分类；公开章节引用公开作品，正文引用章节。书架、历史、评论和关注引用正式用户与公开作品。私人作品引用正式用户，私人章节和进度引用私人作品；删除私人作品会通过外键级联删除章节与进度。私人作品的原始 TXT 在解析后不保留，数据库保存章节正文。公开投稿引用私人原件，但删除原件时投稿外键置空；已审核通过的章节在公开表中有独立副本，不受原件删除影响。

字段约定：`book_info.category_name` 不存在，接口中的 `categoryName` 来自 `book_category.category_name` 关联查询；`user_birthday` 在 SQL 中为 `VARCHAR(20)`，Java DTO/VO 中为 `LocalDate`。生产部署前应核对这类跨层字段转换。

## 4. 接口约定

以下路径均为后端真实路径，开发环境浏览器请求需在前面加 `/api`。除标注为“需登录”的接口外均可由游客调用。需登录接口使用原样 JWT：

```http
Authorization: <JWT>
```

不是 `Bearer <JWT>`。退出登录是前端清除本地 token 和用户信息，后端没有 `/front/auth/logout`。

成功与失败均采用统一 JSON 包装：

```json
{ "code": "00000", "data": {}, "message": "成功" }
```

分页响应的 `data` 为 `{ "list": [], "page": 1, "size": 20, "total": 0 }`。业务错误通常也以 HTTP 200 返回，调用方应检查 `code`。当前常见错误码：`A0001` 登录失败、`A0002` 验证码错误、`A0003` 账号已存在、`A0230` 未登录或 token 过期、`A0400` 参数错误、`B0003` 资源不存在、`B0004` 榜单类型错误。`B0004` 是当前后端实际值，不是旧草案中的 `A0400`。

### 4.1 公开阅读

| 方法 | 路径 | 参数 | `data` / 行为 |
| --- | --- | --- | --- |
| GET | `/front/books/home` | 无 | `{latest, hot, updated}`，各为书籍摘要数组 |
| GET | `/front/categories` | `direction` 可选，0/1 | 启用分类数组 |
| GET | `/front/books` | `q`, `categoryId`, `status`, `sort`, `page`, `size` | 书籍分页；`status` 指连载/完结，`sort` 为 `visit/update/new` |
| GET | `/front/ranks/{type}` | `type=visit/update/new` | 榜单书籍摘要数组，最多 20 条 |
| GET | `/front/books/{bookId}` | 路径 ID | 上架书籍摘要 |
| GET | `/front/books/{bookId}/chapters` | `page=1`, `size=20` | 章节分页，不含正文 |
| GET | `/front/chapters/{chapterId}` | 路径 ID | `{book, chapter, content, previousChapterId, nextChapterId}` |
| POST | `/front/books/{bookId}/visit` | 无 | 点击量原子加一，`data=null` |
| GET | `/front/books/{bookId}/comments` | `page=1`, `size=20` | 主评论分页 |
| GET | `/front/books/{bookId}/comments/{commentId}/replies` | `page=1`, `size=20` | 指定主评论的回复分页 |

`GET /front/books` 的 `q` 同时匹配书名和作者；公开查询过滤下架作品。书籍分页上限 100，目录分页上限 10000，评论分页上限 100。章节前后 ID 根据同一作品中的章节序号查找，不根据章节 ID 加减推断。

书籍摘要主要字段：`id`, `bookName`, `authorName`, `categoryId`, `categoryName`, `picUrl`, `bookDesc`, `bookStatus`, `wordCount`, `visitCount`, `chapterCount`, `firstChapterId`, `lastChapterId`, `lastChapterName`, `lastChapterUpdateTime`, `status`, `createTime`, `updateTime`。章节摘要为 `id`, `bookId`, `chapterNum`, `chapterName`, `wordCount`, `createTime`, `updateTime`。

### 4.2 认证与个人资料

| 方法 | 路径 | 请求 | `data` / 行为 |
| --- | --- | --- | --- |
| GET | `/front/auth/captcha` | 无 | `{img, sessionId, captchaKey, image}`；`image` 为 data URI |
| POST | `/front/auth/register` | `{username, password, captchaKey, captchaCode}` | 注册并登录，返回登录结果 |
| POST | `/front/auth/login` | `{username, password}` | `{token, uid, nickName, user}` |
| GET | `/front/me/me` | 需登录 | 当前用户资料 |
| PUT | `/front/me/me` | 需登录；资料字段见下文 | 更新资料，当前返回 `data=null` |

注册 DTO 还兼容旧字段 `sessionId` / `velCode`，新请求建议使用 `captchaKey` / `captchaCode`。登录结果中的 `user` 为 `{id, username, nickname, avatar}`；资料响应使用 `nickName`, `userPhoto`, `userSex`, `userBirthday`, `userPhone`, `userEmail`。前端字段适配层将登录摘要的 `nickname/avatar` 转为标准资料字段。资料更新使用这些标准字段，其中 `userBirthday` 格式为 `yyyy-MM-dd`。

### 4.3 书架、历史、关注与评论

本节接口均需登录。

| 方法 | 路径 | 请求 | `data` / 行为 |
| --- | --- | --- | --- |
| GET | `/front/me/bookshelf` | 无 | 书架数组，条目含 `book`, `lastChapterId`, `lastChapterName`, `lastReadAt` |
| PUT | `/front/me/bookshelf/{bookId}` | 可不带 body，或 `{chapterId}` | 加入书架或更新该书架项进度，返回条目 |
| DELETE | `/front/me/bookshelf/{bookId}` | 无 | 移出书架 |
| GET | `/front/me/history` | `page=1`, `size=20` | 历史分页，条目含 `book`, `chapterId`, `chapterName`, `readAt` |
| PUT | `/front/me/progress` | `{bookId, chapterId}` | 写入/更新历史；仅同步已存在的书架项，不自动加入书架 |
| GET | `/front/me/following` | 无 | 关注数组，含 `book`, `hasUpdate`, `lastNotifiedChapterId` |
| PUT | `/front/me/following/{bookId}` | 无 | 关注作品 |
| DELETE | `/front/me/following/{bookId}` | 无 | 取消关注 |
| PUT | `/front/me/following/{bookId}/read` | 无 | 确认已看过当前更新 |
| POST | `/front/me/books/{bookId}/comments` | `{content, parentCommentId?}` | 发表主评论或回复，`content` 最多 500 字 |
| DELETE | `/front/me/comments/{commentId}` | 无 | 仅作者可软删除自己的评论 |

回复时 `parentCommentId` 必须是同一公开作品下仍有效的主评论 ID。关注提醒目前通过列表中的 `hasUpdate` 提供站内状态，没有 WebSocket、短信或推送服务。私人作品不使用上述公开历史、关注和评论接口。

### 4.4 私人 TXT 导入

本节接口均需登录，且每次详情、目录、正文、进度和删除都核对当前 JWT 用户的作品归属；访问他人作品与访问不存在作品统一返回 `B0003`。

| 方法 | 路径 | 请求 | `data` / 行为 |
| --- | --- | --- | --- |
| POST | `/front/me/imports` | `multipart/form-data`，字段 `file` | 导入并返回私人作品摘要 |
| GET | `/front/me/imports` | `page=1`, `size=20` | 本人作品分页，`size` 上限 100 |
| GET | `/front/me/imports/{novelId}` | 路径 ID | 本人作品详情与续读章节 |
| GET | `/front/me/imports/{novelId}/chapters` | `page=1`, `size=100` | 私人目录分页，`size` 上限 200 |
| GET | `/front/me/imports/{novelId}/chapters/{chapterId}` | 路径 ID | 正文及 `previousChapterId` / `nextChapterId` |
| PUT | `/front/me/imports/{novelId}/progress` | `{chapterId}` | 更新私人续读进度 |
| DELETE | `/front/me/imports/{novelId}` | 无 | 删除作品、章节和进度 |

作品摘要字段：`id`, `bookName`, `originalFileName`, `encoding`, `fileSize`, `wordCount`, `chapterCount`, `firstChapterId`, `lastReadChapterId`, `lastReadChapterName`, `lastReadAt`, `createTime`, `updateTime`。正文沿用 `{book, chapter, content, previousChapterId, nextChapterId}` 结构；这里的 `chapter.bookId` 表示**私人作品 ID**，不能拿它请求公开书籍接口。

仅支持最大 20 MB 的 `.txt`；依次尝试 UTF-8 BOM、UTF-8、GB18030，拒绝空文件、明显二进制和无法解码内容。独占一行的常见“第一章 …”标题触发拆章；标题前正文作为序章，没有标题则整本一章，最多 5000 章。导入成功后只保留解析结果，不保存原始 TXT。无进度时“开始阅读”进入 `firstChapterId`，有进度时“继续阅读”进入 `lastReadChapterId`。

### 4.5 请求示例

```http
GET /api/front/books?q=剑&sort=update&page=1&size=20
GET /api/front/me/imports?page=1&size=20
Authorization: <JWT>
```

```http
PUT /api/front/me/imports/12/progress
Authorization: <JWT>
Content-Type: application/json

{"chapterId": 345}
```

### 4.6 投稿与管理员审核

读者可将 `POST /front/me/imports` 的 `visibility` 设为 `PRIVATE` 或 `PUBLIC`。`PUBLIC` 需填写 `bookName`、`authorName`、`categoryId` 和 `rightsConfirmed=true`。审核通过前只有上传者能阅读私人原件。退稿后可通过 `/front/me/imports/{novelId}/resubmit` 修改资料并可选替换 TXT；替换后阅读进度清空。撤回公开作品需要管理员批准，不能直接删除公开副本。

管理端使用独立 `admin_user` 与管理员 JWT。接口包含 `/admin/auth/login`、投稿列表/详情/章节预览/通过/退稿、撤回列表/批准/驳回、主动下架和审核日志。完整字段、请求体与响应结构见 [`reader-api.md`](reader-api.md) 第 9 节；[`reader-publication.openapi.json`](reader-publication.openapi.json) 可直接导入 Apifox。

投稿状态为 `PENDING`、`REJECTED`、`APPROVED`、`CANCELLED`。公开副本状态为 `ACTIVE`、`WITHDRAWN`、`TAKEN_DOWN`。撤回申请状态为 `PENDING`、`APPROVED`、`REJECTED`、`CLOSED`；管理员直接下架时仍待处理的申请转为 `CLOSED`。审核通过时，私人章节和正文在一个数据库事务内复制到公开表；失败则回滚。公开章节接口每次校验书籍仍上架，防止 Redis 中已缓存的正文绕过下架。

## 5. 本地开发与验证

1. 安装 JDK 17、Maven、Node.js、MySQL 8；若需要注册验证码，启动 Redis。检查 `novel-service/src/main/resources/application.yml` 的数据库、Redis 和 JWT 配置。配置文件当前包含本地连接参数，部署前需改为部署环境的独立凭据。
2. 新数据库执行 `docs/reader-database.mysql.sql`，再执行 `docs/reader-private-import-migration.mysql.sql` 和 `docs/reader-publication-migration.mysql.sql`。已有数据库先备份并核对表结构，只执行缺少的增量脚本；不要重新执行带 `DROP TABLE` 的主脚本。本地库的投稿审核迁移已于 2026-11-16 执行，执行前备份在 `D:\Frist_project\novel-before-publication-20261116.sql`。
3. 首次启动前设置 `NOVEL_ADMIN_BOOTSTRAP_USERNAME`、`NOVEL_ADMIN_BOOTSTRAP_PASSWORD` 和长度至少 32 字节的 `NOVEL_ADMIN_JWT_SECRET`。首次启动会在 `admin_user` 插入管理员；后续启动不会覆盖现有密码。不要将这些变量写入仓库。先在 `novel_web/novel` 运行 `mvn -DskipTests install`，再进入 `novel-service` 运行 `mvn spring-boot:run`。在 `novel-front-web` 运行 `npm install`、`npm run serve`，打开 `http://localhost:1024/`；导入入口为 `/me/imports`。
4. 在 `novel-front-manger` 运行 `npm install`、`npm run dev`，打开 `http://127.0.0.1:1025/`。管理端 `/api` 代理到后端 8080；管理员登录后可处理投稿和撤回申请。
5. 两个前端分别运行 `npm run lint` 和 `npm run build`。后端可运行 `mvn test`；截至本文日期，完整测试仍会被原有 `NovelPojoApplicationTests` 找不到 `@SpringBootConfiguration` 阻断，投稿服务和 Mapper 的定向测试已通过。

扩展接口时，先更新 POJO/DTO/VO 与 Mapper，再实现 Service 和 Controller，最后同步 `src/api`、页面及本文。新增私人作品查询始终在 SQL 或服务层校验 `user_id`；私人数据不要并入公开书籍索引、缓存键或评论流程。
