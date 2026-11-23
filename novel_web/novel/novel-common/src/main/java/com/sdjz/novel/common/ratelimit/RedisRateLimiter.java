package com.sdjz.novel.common.ratelimit;

import com.sdjz.novel.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于 Redis + Lua 的令牌桶限流器。
 *
 * <p><b>为什么必须用 Lua</b>：{@code HMGET → 计算 → HMSET} 如果拆成三次 Java 调用，
 * 并发下会出现「两个线程都读到还剩 1 枚、都放行」的超发，限流形同虚设。Redis 单线程
 * 执行整段脚本，天然原子。
 *
 * <p><b>为什么用令牌桶而不是固定窗口</b>：固定窗口在边界会突刺（限 100 次/分钟时，
 * 第 59 秒打 100 次、第 61 秒再打 100 次，两秒内实际吃了 200 次）；令牌桶既能限制
 * 平均速率，又允许合理突发，还能直接算出「还要等几秒」，正好是 Retry-After 需要的值。
 *
 * <p><b>已知限制</b>：脚本用 JVM 时间做时间基准。单机部署没有问题；将来多实例时
 * 机器间时钟漂移会让补充量算偏，届时应改为读取 Redis 的 TIME 命令。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final RedisUtil redisUtil;

    /**
     * KEYS[1] 桶 key
     * ARGV[1] 容量（窗口内允许的次数）
     * ARGV[2] 本次消耗令牌数
     * ARGV[3] 窗口长度（秒）
     * ARGV[4] 当前毫秒时间戳
     *
     * 返回 { 是否放行(1/0), 建议等待毫秒(0 表示无需等待) }
     */
    private static final String LUA = """
            local key      = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local cost     = tonumber(ARGV[2])
            local window   = tonumber(ARGV[3])
            local now      = tonumber(ARGV[4])

            if capacity == nil or capacity <= 0 or window == nil or window <= 0 then
              return {1, 0}
            end

            -- 补充速率：窗口内补满一桶
            local rate = capacity / window

            local data   = redis.call('HMGET', key, 'tokens', 'ts')
            local tokens = tonumber(data[1])
            local ts     = tonumber(data[2])

            if tokens == nil or ts == nil then
              tokens = capacity
              ts = now
            end

            local elapsed = now - ts
            if elapsed < 0 then elapsed = 0 end
            tokens = math.min(capacity, tokens + elapsed * rate / 1000)

            local allowed = 0
            local waitMs  = 0

            if tokens >= cost then
              tokens  = tokens - cost
              allowed = 1
            else
              waitMs = math.ceil((cost - tokens) / rate * 1000)
              if waitMs < 1 then waitMs = 1 end
            end

            redis.call('HMSET', key, 'tokens', string.format('%.4f', tokens), 'ts', now)
            -- 至少保留一个完整窗口，避免桶在长时间无请求后被回收导致额度被重置
            redis.call('PEXPIRE', key, math.ceil(window * 1000) + 1000)

            return {allowed, waitMs}
            """;

    private static final RedisScript<List> TOKEN_BUCKET =
            new DefaultRedisScript<>(LUA, List.class);

    /**
     * 尝试获取一枚令牌。
     *
     * @param key           限流键，调用方拼好（如 rl:read:u:123）
     * @param permits       窗口内允许次数
     * @param windowSeconds 窗口长度（秒）
     * @throws RuntimeException Redis 不可用时抛出，由 {@link CompositeRateLimiter} 决定降级策略
     */
    @SuppressWarnings("rawtypes")
    public RateLimitResult tryAcquire(String key, int permits, int windowSeconds) {
        List result = redisUtil.execute(TOKEN_BUCKET, List.of(key),
                String.valueOf(permits),
                "1",
                String.valueOf(windowSeconds),
                String.valueOf(System.currentTimeMillis()));

        if (result == null || result.size() < 2) {
            // 脚本返回异常结构时按放行处理：限流组件自身故障不应阻断业务
            log.warn("限流脚本返回结构异常，放行本次请求，key={}", key);
            return RateLimitResult.allow();
        }

        long allowed = toLong(result.get(0));
        long waitMs = toLong(result.get(1));

        return allowed == 1 ? RateLimitResult.allow() : RateLimitResult.deny(toSeconds(waitMs));
    }

    private static long toLong(Object v) {
        return v instanceof Number n ? n.longValue() : 0L;
    }

    /** 秒数向上取整，且至少 1 秒，避免 Retry-After: 0 让客户端立即重试。 */
    private static long toSeconds(long millis) {
        return Math.max(1, (millis + 999) / 1000);
    }
}
