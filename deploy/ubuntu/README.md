# 山崖书院 Ubuntu 单机部署

本目录是部署模板，不会自动创建阿里云资源。示例域名 `reader.example.com` 和 `admin.reader.example.com` 必须替换为真实域名。服务器建议阿里云香港 Ubuntu 24.04，2 vCPU、8 GB 内存、80 GB 云盘。本方案只部署 PC 读者站、管理站和 Java API，不包含 UniApp。

## 1. 构建和准备

在 `D:\Frist_project` 本机执行：

```powershell
cd novel_web\novel
mvn -pl novel-service -am clean test
mvn -pl novel-service -am clean package -DskipTests
cd ..\..\novel-front-web
yarn install --frozen-lockfile
yarn lint
yarn build
cd ..\novel-front-manger
npm ci
npm run lint
npm run build
```

确认读者站 `.env.production` 的 API 地址为 `/api`。交付以下文件，不要交付 `node_modules`、本地日志或任何真实密钥：

- `novel_web/novel/novel-service/target/novel-service-1.0.0.jar`（实际文件名以构建结果为准）
- `novel-front-web/dist/`、`novel-front-manger/dist/`
- `docs/reader-production-init.mysql.sql`、`reader-private-import-migration.mysql.sql`、`reader-publication-migration.mysql.sql`、`reader-cover-migration.mysql.sql`
- 本目录的配置模板，含 `nginx-ratelimit-zones.conf`、`snippets/` 下的 5 个配置片段和 `verify-ratelimit.sh`

先为本次源码和构建产物保存一个可追溯版本；当前工作区包含未提交改动，上传时务必使用这次构建的产物。

## 2. ECS 和数据库

安全组只向公网开放 TCP 80、443；TCP 22 仅允许你的固定 IP。不要开放 3306、6379、8080。服务器上安装 `openjdk-17-jre-headless`、`mysql-server`、`redis-server`、`nginx`、`apache2-utils`、`certbot`、`python3-certbot-nginx`。MySQL、Redis 均只监听 `127.0.0.1`；Redis 保持 `protected-mode yes` 并设置强密码。

创建系统用户 `novel`，工作目录 `/opt/novel`，前端目录 `/var/www/novel-reader` 和 `/var/www/novel-manager`，备份目录 `/var/backups/novel`。让 `novel` 拥有工作和备份目录；Nginx 用户应能读取两个前端目录。将 JAR 放到 `/opt/novel/novel-service.jar`，两个 `dist` 的内容分别放到对应前端目录。

只在全新空库执行下列顺序；**不要**把含 `DROP TABLE` 的 `reader-database.mysql.sql` 用于线上数据库：

```bash
sudo mysql < reader-production-init.mysql.sql
sudo mysql < reader-private-import-migration.mysql.sql
sudo mysql < reader-publication-migration.mysql.sql
sudo mysql < reader-cover-migration.mysql.sql
```

生产脚本只初始化分类，没有任何测试小说。评论和关注表已在主脚本中，不需要再跑对应的增量迁移。以后升级必须先备份，再执行单独的增量 SQL，不能重跑初始化脚本。

为应用创建仅有 `novel` 库业务权限的 MySQL 用户，不要使用 `root`：

```sql
CREATE USER 'novel_app'@'127.0.0.1' IDENTIFIED BY '替换为随机强密码';
GRANT SELECT, INSERT, UPDATE, DELETE ON novel.* TO 'novel_app'@'127.0.0.1';
CREATE USER 'novel_backup'@'127.0.0.1' IDENTIFIED BY '替换为另一随机强密码';
GRANT SELECT, SHOW VIEW, TRIGGER, EVENT ON novel.* TO 'novel_backup'@'127.0.0.1';
```

## 3. Java 服务和 OSS

把 `novel.env.example` 复制为 `/etc/novel/novel.env`，替换全部 `REPLACE_...` 值。`NOVEL_DB_URL` 使用本机 MySQL；用户 JWT、管理员 JWT 使用两份不同且至少 32 字节的随机密钥。`NOVEL_ADMIN_BOOTSTRAP_USERNAME/PASSWORD` 仅用于首次创建管理员；账号创建后可清空这两项，管理员 JWT 密钥仍须保持不变。不要把环境文件放进 Git。

OSS 使用私有 Bucket、香港 Endpoint 和 HTTPS Bucket 域名。为应用单独创建最小权限 RAM 凭据，允许封面对象的上传、读取、复制、设置 ACL 和删除；Bucket 保持私有，只有审核通过后复制到 `covers/public/` 的对象设为 `public-read`。若 OSS 的“阻止公共访问”阻止对象级 ACL，需按官方设置允许对象级公开读取，再验证 `covers/private/` 无签名请求为拒绝访问。`NOVEL_OSS_PUBLIC_BASE_URL` 与实际 Bucket HTTPS 地址必须一致。封面不上传时仍可使用默认封面。

将 `novel.service` 安装到 `/etc/systemd/system/`，环境文件和 MySQL 备份凭据放在 `/etc/novel/`，权限设置为 `root:novel`、目录 `0750`、文件 `0640`。执行 `sudo systemctl daemon-reload`、`sudo systemctl enable --now novel`，用 `sudo journalctl -u novel -n 100 --no-pager` 看启动日志。以下请求应返回 `code: 00000`：

```bash
curl -fsS http://127.0.0.1:8080/front/books/home
curl -fsS http://127.0.0.1:8080/front/categories
```

## 4. Nginx、域名和证书

未购买域名时，仅用 ECS IP 做本人联调，不在 HTTP 下输入正式用户或管理员密码。先把安全组的 80 端口限制为自己的 IP，禁用 Ubuntu 默认 Nginx 站点，启用 `nginx-ip-test.conf`（替换 `YOUR_PUBLIC_IP`），再检查读者站静态资源和后端 API。管理站等 HTTPS 就绪后再登录。购买域名后添加两条 A 记录指向 ECS：`reader.example.com`、`admin.reader.example.com`。启用正式站点时移除 `nginx-ip-test.conf`。

替换两个 HTTP 模板中的域名，放入 `/etc/nginx/sites-available/` 并在 `sites-enabled/` 建立软链接。创建 `/var/www/letsencrypt` 供证书验证使用。管理站用 `htpasswd` 创建 `/etc/nginx/novel-manager.htpasswd`，它与应用管理员密码应不同。Basic Auth 保护管理页面；管理 API 的 `Authorization` 头必须留给现有 JWT，因此 `/api/` 由后端管理员鉴权保护。先执行 `sudo nginx -t`，再重载 Nginx。

DNS 生效后申请同时覆盖两个域名的证书：

```bash
sudo certbot certonly --webroot -w /var/www/letsencrypt -d reader.example.com -d admin.reader.example.com
```

把 HTTP 模板替换为 `nginx-reader-https.conf` 和 `nginx-manager-https.conf`，证书路径按签发结果核对。再次执行 `sudo nginx -t` 后重载 Nginx，并执行 `sudo certbot renew --dry-run`。两站的 `/api/` 均会去掉前缀后代理到本机 Java；读者站会拒绝 `/api/admin/`。前端深层路由通过 `try_files` 回退到 `index.html`。

### 4.1 接入层限流（L0）

限流配置与站点配置分开安装，**注意上下文不同**：`limit_req_zone` 只能定义在 `http {}`，而站点模板是 `server {}`。

| 本地模板 | 服务器目标路径 | 作用 |
| --- | --- | --- |
| `nginx-ratelimit-zones.conf` | `/etc/nginx/conf.d/nginx-ratelimit-zones.conf` | 定义 4 个共享 zone，**只加载一次** |
| `snippets/*.conf` | `/etc/nginx/snippets/` | 反代头、三档限流、429 JSON，供各站点 `include` |

```bash
sudo install -m 0644 nginx-ratelimit-zones.conf /etc/nginx/conf.d/nginx-ratelimit-zones.conf
sudo install -d /etc/nginx/snippets
sudo install -m 0644 snippets/*.conf /etc/nginx/snippets/
sudo install -m 0755 verify-ratelimit.sh /opt/novel/verify-ratelimit.sh
sudo nginx -t && sudo systemctl reload nginx
sudo bash /opt/novel/verify-ratelimit.sh https://reader.example.com
```

阈值（起始值，**压测前的初值，不是结论**）：

| 接口组 | 匹配 location | 阈值 | 读超时 |
| --- | --- | --- | --- |
| 普通 `/api/` | `^~ /api/` | 10 r/s，突发 20；单 IP 20 连接 | 15s |
| 登录/注册/验证码 | `^~ /api/front/auth/`、`^~ /api/admin/auth/` | 20 r/m，突发 10；单 IP 5 连接 | 15s |
| TXT 导入 | `^~ /api/front/me/imports` | 6 r/m，突发 1；单 IP 2 连接 | 120s |

几个必须知道的设计点：

- `limit_req_status` / `limit_conn_status` 显式设为 **429**（Nginx 默认 503，前端会误判为服务故障）；`nodelay` 让超额请求立即被拒而不是排队，避免用户感觉「网站变卡」。
- `snippets/novel-ratelimit-429.conf` 在 **server 上下文** include，把 429 渲染成 `{code:"A0429", ...}` JSON 并带 `Retry-After: 3`。Nginx 默认返回 HTML，axios 解析不到 `message`。
- `X-Forwarded-For` 由 `$proxy_add_x_forwarded_for` 改为 `$remote_addr` **覆盖写入**，另加 `X-Client-IP`；后端 L2 限流只信任后者，防止伪造 IP 绕过。当前后端不读取 XFF，此改动无兼容影响。
- 导入接口单独放宽到 120s：在把 5000 次逐条 insert 改成批量之前，15s 会掐断正常导入；批量化改造完成后应收回。
- `nginx-ip-test.conf` 是临时 IP 联调模板，已有 `allow/deny` 白名单，本轮不加限流；正式启用站点时会移除它。
- **本层只做按 IP 的粗限流**。登录失败锁定、按 UID 的接口级限流、导入全局并发 `Semaphore(2)`、缓存击穿防护属于 L2~L5，不在这里实现，也不在这一层假装实现。

验证脚本会检查文件就位、跑 `nginx -t`、并发打 60 次统计 200/429 分布、连打认证接口看 429，并打印响应头。**只看配置文件不算做完，要看到 429 且正常请求零误伤才算生效。**

回滚：删除 `/etc/nginx/conf.d/nginx-ratelimit-zones.conf`，把 4 份站点模板换回上一版，`sudo nginx -t && sudo systemctl reload nginx`；不涉及数据库和 Java 服务。

前端三端的 `request.js` 目前还不识别 429（会落到「网络异常」提示），这是 P0 的剩余工作；在补上之前，被限流的用户只会看到通用报错，不会自动退避。

## 5. 异地备份和验收

准备与封面 Bucket 分开的私有备份 Bucket，给 `novel` 用户配置 `ossutil` 的独立、受限 RAM 凭据。将 `mysql-backup.cnf.example`、`backup.env.example` 分别安装到 `/etc/novel/`，把 `novel-backup.sh` 安装到 `/opt/novel/` 并设为可执行；`novel-backup.service` 和 `.timer` 放入 `/etc/systemd/system/`。先运行一次 `sudo systemctl start novel-backup.service`，确认 OSS 存在非空压缩备份并在测试空库恢复成功，然后启用定时器：`sudo systemctl enable --now novel-backup.timer`。OSS Bucket 开启服务端加密和生命周期保留策略。

用浏览器验收游客浏览、搜索、目录、正文；注册登录、书架、历史、评论、私人 TXT 导入；管理员审核、发布与封面访问。刷新 `/read/...`、`/submissions/...` 等深层路由应正常。浏览器网络请求必须是同域 HTTPS `/api/...`，不能出现 `localhost` 或旧测试域名。重启 ECS 后检查 Java、Nginx、MySQL、Redis 自动恢复，并确认数据库与 Redis 无法从公网访问。

正式开放前只上传拥有发布权的作品，不导入用于本地联调的热门小说测试数据。
