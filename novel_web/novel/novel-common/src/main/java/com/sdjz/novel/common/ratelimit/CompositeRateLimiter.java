package com.sdjz.novel.common.ratelimit;

import com.sdjz.novel.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 限流器门面：正常走 Redis，Redis 故障时按分组决定放行还是拒绝。
 *
 * <p>为什么降级策略必须按分组配置：读接口和登录如果一律拒绝，Redis 一抖动全站就
 * 不可用，这是比限流失效更严重的事故；而导入、投稿如果放行，20 MB 解析会把堆和
 * 连接池一起打穿。两类接口的风险方向相反，不可能用同一个策略。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompositeRateLimiter {

    private final RedisRateLimiter redisRateLimiter;
    private final CaffeineRateLimiter caffeineRateLimiter;

    /** Redis 故障期间的关键日志节流，避免故障时日志刷爆磁盘。 */
    private final AtomicLong lastWarnAt = new AtomicLong(0);

    public RateLimitResult tryAcquire(String key, int permits, int windowSeconds,
                                      RateLimitFallback fallback) {
        try {
            return redisRateLimiter.tryAcquire(key, permits, windowSeconds);
        } catch (Exception e) {
            long now = System.currentTimeMillis();
            if (now - lastWarnAt.getAndSet(now) > 10_000) {
                log.warn("Redis 限流不可用，按 {} 降级（同类日志 10 秒内只记一次），key={}",
                        fallback, key, e);
            }
            if (fallback == RateLimitFallback.CLOSE) {
                // 宁可直接拒绝，也不能无限流地把重型请求放进 JVM
                return RateLimitResult.deny(3);
            }
            return caffeineRateLimiter.tryAcquire(key, permits, windowSeconds);
        }
    }
}
