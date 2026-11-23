# 阅读接口 Redis 缓存

后端沿用 `application.yml` 中的 `spring.redis` 连接配置。`reader.cache.enabled` 默认是 `true`，设为 `false` 可让公开阅读请求直接查询 MySQL，不影响注册验证码原有的 Redis 用法。

| 接口数据 | Redis key（前缀 `novel:reader:v1:`） | TTL |
| --- | --- | --- |
| 首页三组书籍 | `home` | 30 秒 |
| 分类列表 | `categories:all`、`categories:0`、`categories:1` | 10 分钟 |
| 榜单 | `ranks:visit`、`ranks:update`、`ranks:new` | 30 秒 |
| 章节正文及上下章 ID | `chapter:{chapterId}` | 5 分钟 |

缓存只保存成功查询出的公开数据；不存在的章节不会缓存。Redis 读取或写入失败时，接口继续使用数据库结果。书籍详情、搜索结果、评论和用户数据不缓存。点击量会立即写入 MySQL，但首页和榜单上的计数最多可能延迟 30 秒更新。

如果直接在数据库修改分类、书籍状态或章节内容，相关缓存会在 TTL 到期后更新。需立即生效时，可删除对应的 `novel:reader:v1:` key；不要清空 Redis 中的验证码数据。
