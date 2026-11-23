#!/usr/bin/env bash
# 在服务器上执行：sudo bash verify-ratelimit.sh https://reader.example.com
# 不带参数时默认打本机 80 端口（需站点已启用）。
# 目的：证明限流真的生效，而不是「配置写完了就算做完了」。
set -euo pipefail
BASE="${1:-http://127.0.0.1}"

echo "== 1. 检查限流文件是否全部就位 =="
for f in /etc/nginx/conf.d/nginx-ratelimit-zones.conf \
         /etc/nginx/snippets/novel-proxy.conf \
         /etc/nginx/snippets/novel-ratelimit-api.conf \
         /etc/nginx/snippets/novel-ratelimit-auth.conf \
         /etc/nginx/snippets/novel-ratelimit-import.conf \
         /etc/nginx/snippets/novel-ratelimit-429.conf; do
    if [ ! -f "$f" ]; then echo "缺失: $f"; exit 1; fi
done
echo "OK"

echo "== 2. 配置语法校验 =="
nginx -t

echo "== 3. 单个请求必须正常 =="
curl -sS -o /dev/null -w "HTTP %{http_code}\n" "$BASE/api/front/books/home"

echo "== 4. 60 次并发，统计状态码分布（应同时出现 200 与 429）=="
seq 1 60 | xargs -P 20 -I{} curl -sS -o /dev/null -w "%{http_code}\n" \
    "$BASE/api/front/books/home" | sort | uniq -c

echo "== 5. 认证接口连续 12 次（应很快出现 429）=="
for i in $(seq 1 12); do
    curl -sS -o /dev/null -w "%{http_code} " "$BASE/api/front/auth/captcha"
done
echo

echo "== 6. 429 响应头与响应体（第 5 步已把认证 zone 打满）=="
curl -sS -i "$BASE/api/front/auth/captcha" | head -12

echo "== 完成：确认 429 带 Retry-After，且响应体是 JSON =="
