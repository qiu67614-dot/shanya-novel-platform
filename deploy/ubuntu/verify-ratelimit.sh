#!/usr/bin/env bash
# 在服务器上执行：sudo bash verify-ratelimit.sh https://reader.example.com [并发请求数]
# 不带参数时默认打本机 80 端口（需站点已启用）；第二个参数控制第 4 步的请求数，默认 300。
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

echo "== 4. 并发打普通 API，统计状态码分布 =="
# 当前 novel_api 为 40r/s + burst=80，一秒内约可放行 120 次，
# 所以并发数必须明显大于 120 才会出现 429。默认打 300 次 / 50 并发。
# 若结果里 429 为 0：说明阈值对当前流量足够宽松（正常现象），
# 想验证拦截能力就临时把 N 调大，例如 N=2000。
N="${2:-300}"
echo "并发数: $N"
seq 1 "$N" | xargs -P 50 -I{} curl -sS -o /dev/null -w "%{http_code}\n" \
    "$BASE/api/front/books/home" | sort | uniq -c

echo "== 5. 认证接口连续 60 次（novel_auth 为 80r/m + burst=30，应出现 429）=="
codes=""
for i in $(seq 1 60); do
    codes="$codes $(curl -sS -o /dev/null -w "%{http_code}" "$BASE/api/front/auth/captcha")"
done
echo "$codes" | tr ' ' '\n' | grep -v '^$' | sort | uniq -c

echo "== 6. 429 响应头与响应体（第 5 步已把认证 zone 打满）=="
curl -sS -i "$BASE/api/front/auth/captcha" | head -12

echo "== 完成：确认 429 带 Retry-After，且响应体是 JSON =="
