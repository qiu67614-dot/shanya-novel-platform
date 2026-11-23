package com.sdjz.novel.common.ratelimit;

/**
 * Redis 不可用时的降级策略。
 *
 * <p>必须按业务分流，不能一刀切：
 * <ul>
 *   <li>读接口、登录 —— {@link #OPEN}：宁可限流稍宽松，也不能让全站不可用；</li>
 *   <li>导入、投稿、审核 —— {@link #CLOSE}：宁可拒绝，也不能无限流地压垮内存或写出脏数据。</li>
 * </ul>
 */
public enum RateLimitFallback {

    /** 放行，退回到 JVM 内的 Caffeine 本地令牌桶。 */
    OPEN,

    /** 拒绝，抛出 429。适用于不可承受突发流量的重型写接口。 */
    CLOSE
}
