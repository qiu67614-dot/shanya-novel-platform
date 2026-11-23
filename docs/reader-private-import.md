# 私人 TXT 导入

先在现有 `novel` 数据库执行 `reader-private-import-migration.mysql.sql`，再启动后端。
该迁移只新增 `user_novel`、`user_novel_chapter`、`user_novel_progress`，不修改公开书籍表。
原始上传文件在解析后不保留，章节正文存入 MySQL。需要备份私人作品时，备份这三张表。

登录后打开前端 `/me/imports`。仅支持不超过 20 MB 的 TXT，依次尝试 UTF-8 BOM、UTF-8、GB18030；最多生成 5000 章。章节标题须独占一行，格式如 `第一章 开始` 或 `第1章：开始`。标题前正文作为序章；没有章节标题则作为一章。

所有接口位于 `/front/me/imports`，沿用现有 `Authorization: <JWT>` 和 `{code,data,message}` 格式。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/front/me/imports` | multipart 字段 `file`，返回作品摘要 |
| GET | `/front/me/imports?page=1&size=20` | 本人作品分页 |
| GET | `/front/me/imports/{id}` | 本人作品详情 |
| GET | `/front/me/imports/{id}/chapters?page=1&size=100` | 章节目录，最多每页 200 条 |
| GET | `/front/me/imports/{id}/chapters/{chapterId}` | 正文及前后章节 ID |
| PUT | `/front/me/imports/{id}/progress` | JSON `{ "chapterId": 123 }` |
| DELETE | `/front/me/imports/{id}` | 删除作品、章节和阅读进度 |

私人作品的详情、目录、章节、进度和删除均根据 JWT 用户 ID 校验归属。不存在或不属于当前用户时返回 `B0003`；文件不合法返回 `A0400`。私人数据不会出现在公开书库、搜索、榜单、评论或公开阅读历史接口中。
