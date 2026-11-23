# 阅读接口 Redis 缓存

后端沿用 `application.yml` 中的 `spring.redis` 连接配置。`reader.cache.enabled` 默认是 `true`，设为 `false` 可让公开阅读请求直接查询 MySQL，不影响注册验证码原有的 Redis 用法。

| 接口数据 | Redis key（前缀 `novel:reader:v1:`） | TTL |
| --- | --- | --- |
| 首页三组书籍 | `home` | 30 秒 |
| 分类列表 | `categories:all`、`categories:0`、`categories:1` | 10 分钟 |
| 榜单 | `ranks:visit`、`ranks:update`、`ranks:new` | 30 秒 |
| 章节正文及上下章 ID | `chapter:{chapterId}` | 5 分钟 |

缓存只保存成功查询出的公开数据。Redis 读取或写入失败时，接口继续使用数据库结果。书籍详情、搜索结果、评论和用户数据不缓存。

## 高并发下的保护

| 机制 | 作用 | 关键实现 |
| --- | --- | --- |
| 单飞（single-flight） | 同一个 key 只放一个请求回源，其余等结果 | 进程内 `CompletableFuture` 合并 + Redis `SET NX PX` 跨实例锁，锁 key 为 `novel:lock:reader:v1:{key}` |
| 空值缓存 | 挡住对不存在数据的反复穿透 | 查不到时写入字符串 `null`，TTL 30 秒 |
| TTL 抖动 | 防止一批 key 在同一秒集体失效 | 过期时间上下浮动 10% |
| 延迟双删 | 事务提交后被并发读回的旧值清掉 | `CacheEvictor`：`afterCommit` 删除 + 500ms 后补删 |

单飞失败时的行为是「降级不阻塞」：抢不到锁最多等 500ms，等不到就自己查库；Redis 整体不可用则直接回源。锁的 TTL 为 3 秒，进程崩溃后自动释放。

## 阅读量

点击量不再每次请求都 `UPDATE` 数据库，而是：

1. 请求 → `INCR novel:visit:buf:{bookId}`，同时 `SADD novel:visit:dirty {bookId}`（一个 Lua 脚本内完成）；
2. 每 10 秒由 `BookVisitCounter` 取走缓存中的增量，合并成一条 `UPDATE book_info SET visit_count = visit_count + N`；
3. 写库失败会把增量放回缓冲区，下个周期重试。

因此榜单页的阅读数最多延迟约 10 秒（榜单本身还有 30 秒缓存）。进程重启最多丢失最近 10 秒的计数；Redis 不可用时退化为直接写库，功能不受影响。

如果直接在数据库修改分类、书籍状态或章节内容，相关缓存会在 TTL 到期后更新。需立即生效时，可删除对应的 `novel:reader:v1:` key；不要清空 Redis 中的验证码数据。`novel:lock:reader:v1:` 与 `novel:visit:` 前缀的 key 是运行期状态，不要手工改动。
