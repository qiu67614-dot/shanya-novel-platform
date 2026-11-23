package com.sdjz.novel.common.exception;

/**
 * 限流拒绝异常。
 *
 * <p>刻意不复用 {@link BusinessException}：后者会被全局异常处理统一转成 HTTP 200 +
 * body 里的 code，而限流必须返回真正的 <b>HTTP 429</b>，前端 axios 才能走 429 分支
 * 读取 Retry-After 做退避，否则只会落到「网络异常，请稍后再试」。
 *
 * <p>携带 {@code retryAfterSeconds}，由全局异常处理写入 Retry-After 响应头。
 */
public class RateLimitException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitException(long retryAfterSeconds) {
        super("操作过于频繁，请 " + Math.max(1, retryAfterSeconds) + " 秒后重试");
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public RateLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
