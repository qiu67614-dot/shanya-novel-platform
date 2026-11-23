package com.sdjz.novel.common.ratelimit;

/**
 * 限流判定结果。
 *
 * @param allowed           是否放行
 * @param retryAfterSeconds 建议等待秒数，直接写入 {@code Retry-After} 响应头；
 *                          放行时为 0
 */
public record RateLimitResult(boolean allowed, long retryAfterSeconds) {

    public static RateLimitResult allow() {
        return new RateLimitResult(true, 0);
    }

    public static RateLimitResult deny(long retryAfterSeconds) {
        return new RateLimitResult(false, Math.max(1, retryAfterSeconds));
    }
}
