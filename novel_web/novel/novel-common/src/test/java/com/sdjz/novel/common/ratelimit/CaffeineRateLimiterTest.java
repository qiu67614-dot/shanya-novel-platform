package com.sdjz.novel.common.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 本地令牌桶算法的单元测试。
 *
 * <p>不启动 Spring、不依赖 Redis：Redis 版限流器的正确性主要靠 Lua 的原子性，
 * 而算法本身（容量、补充速率、拒绝时的等待时间）在本地桶上就能完整验证。
 * 这也是这套设计选择「两份实现」的好处之一——核心算法可被快速单测覆盖。
 */
class CaffeineRateLimiterTest {

    @Test
    @DisplayName("容量耗尽后必须拒绝，并给出至少 1 秒的等待建议")
    void shouldRejectWhenBucketIsEmpty() {
        CaffeineRateLimiter limiter = new CaffeineRateLimiter();
        String key = "rl:test:empty";

        // 3 次/分钟，补充速率极慢（0.05 枚/秒），保证下面的循环期间补不出新令牌
        for (int i = 1; i <= 3; i++) {
            assertTrue(limiter.tryAcquire(key, 3, 60).allowed(),
                    "第 " + i + " 次应放行");
        }

        RateLimitResult denied = limiter.tryAcquire(key, 3, 60);
        assertFalse(denied.allowed(), "第 4 次应被拒绝");
        assertTrue(denied.retryAfterSeconds() >= 1,
                "Retry-After 至少 1 秒，不能返回 0 让客户端立即重试");
    }

    @Test
    @DisplayName("等待一段时间后令牌必须按速率补充")
    void shouldRefillOverTime() throws InterruptedException {
        CaffeineRateLimiter limiter = new CaffeineRateLimiter();
        String key = "rl:test:refill";

        // 10 次/秒 => 每 100ms 补 1 枚
        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.tryAcquire(key, 10, 1).allowed());
        }
        assertFalse(limiter.tryAcquire(key, 10, 1).allowed(), "打满后应立即拒绝");

        Thread.sleep(250);
        assertTrue(limiter.tryAcquire(key, 10, 1).allowed(), "250ms 后应补出至少 1 枚");
    }

    @Test
    @DisplayName("不同限流键互不影响，避免一个用户拖累另一个")
    void shouldIsolateDifferentKeys() {
        CaffeineRateLimiter limiter = new CaffeineRateLimiter();

        for (int i = 0; i < 2; i++) {
            assertTrue(limiter.tryAcquire("rl:test:userA", 2, 60).allowed());
        }
        assertFalse(limiter.tryAcquire("rl:test:userA", 2, 60).allowed());

        assertTrue(limiter.tryAcquire("rl:test:userB", 2, 60).allowed(),
                "另一个用户不应受 userA 被限的影响");
    }

    @Test
    @DisplayName("非法阈值不应导致误拦，按放行处理")
    void shouldAllowWhenConfigIsInvalid() {
        CaffeineRateLimiter limiter = new CaffeineRateLimiter();
        assertTrue(limiter.tryAcquire("rl:test:bad", 0, 60).allowed());
        assertTrue(limiter.tryAcquire("rl:test:bad", 60, 0).allowed());
    }
}
