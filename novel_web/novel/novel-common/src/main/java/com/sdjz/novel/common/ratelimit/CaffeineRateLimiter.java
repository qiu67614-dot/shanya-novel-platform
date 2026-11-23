package com.sdjz.novel.common.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * JVM 内的本地令牌桶，只在 Redis 不可用且该分组为 {@link RateLimitFallback#OPEN} 时使用。
 *
 * <p>它<b>不是</b>主限流器：本地桶只作用于单个 JVM，加实例额度就翻倍，也无法跨实例
 * 统计。它的唯一职责是「Redis 挂掉时，不要因为限流组件故障而让全站不可用」——
 * 宁可退回一个更宽松、但仍有下限的保护。
 *
 * <p>依赖 caffeine 早已在 novel-common 的 pom 里但从未被使用，这里正好用上，
 * 零新增依赖。
 */
@Slf4j
@Component
public class CaffeineRateLimiter {

    /**
     * 桶状态。tokens 用 double 保存，避免整数扣减累积误差。
     * 过期时间取 10 分钟远大于常见窗口（60s），空闲即回收，防止 key 无限增长。
     */
    private static final class Bucket {
        double tokens;
        long lastRefillMs;

        Bucket(double tokens, long now) {
            this.tokens = tokens;
            this.lastRefillMs = now;
        }
    }

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    public RateLimitResult tryAcquire(String key, int permits, int windowSeconds) {
        if (permits <= 0 || windowSeconds <= 0) {
            return RateLimitResult.allow();
        }

        long now = System.currentTimeMillis();
        double ratePerMs = (double) permits / windowSeconds / 1000.0;

        Bucket bucket = buckets.get(key, k -> new Bucket(permits, now));
        if (bucket == null) {
            return RateLimitResult.allow();
        }

        long waitMs;
        synchronized (bucket) {
            long elapsed = Math.max(0, now - bucket.lastRefillMs);
            bucket.tokens = Math.min(permits, bucket.tokens + elapsed * ratePerMs);
            bucket.lastRefillMs = now;

            if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0;
                return RateLimitResult.allow();
            }
            waitMs = (long) Math.ceil((1.0 - bucket.tokens) / ratePerMs);
        }

        return RateLimitResult.deny(Math.max(1, (waitMs + 999) / 1000));
    }
}
