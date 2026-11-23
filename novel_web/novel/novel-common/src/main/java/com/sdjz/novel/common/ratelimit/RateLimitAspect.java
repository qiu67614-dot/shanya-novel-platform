package com.sdjz.novel.common.ratelimit;

import com.sdjz.novel.common.context.UserContext;
import com.sdjz.novel.common.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 接口级限流切面。
 *
 * <p>{@code @Order(HIGHEST_PRECEDENCE + 100)} 让它排在事务拦截器之前：被限流的请求
 * 不应该先开启事务、占用数据库连接再被拒。事务的默认顺序是 LOWEST_PRECEDENCE，
 * 所以这里在外层。
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimitProperties properties;
    private final CompositeRateLimiter limiter;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        if (!properties.isEnabled()) {
            return pjp.proceed();
        }

        RateLimitProperties.Rule rule = properties.ruleOf(rateLimit.group());

        Long uid = rateLimit.dimension() == RateLimitDimension.USER ? UserContext.getUid() : null;
        boolean byUser = uid != null;

        String key = buildKey(rateLimit.group(), rateLimit.dimension(), uid);
        int permits = resolvePermits(rateLimit.dimension(), rule, byUser);

        RateLimitResult result = limiter.tryAcquire(
                key, permits, rule.getWindow(), rule.getFallback());

        if (!result.allowed()) {
            // 记 key、group、byUser 是必须的：用户报「我被限了」时要能立刻判断
            // 是「这个人自己刷太多」还是「同出口的其他人在刷」
            log.info("限流命中 group={} key={} byUser={} permits={}/{}s retryAfter={}s",
                    rateLimit.group(), key, byUser, permits, rule.getWindow(),
                    result.retryAfterSeconds());
            throw new RateLimitException(result.retryAfterSeconds());
        }

        return pjp.proceed();
    }

    /**
     * 有身份用单人额度，没身份用「共用出口」额度。
     *
     * <p>这两个值不能相同：读接口按单人 60 次/分，游客会降级到 IP 桶，而一个
     * 校园网出口后面可能有几十人，共用 60 次/分等于把整栋楼卡死。
     */
    private int resolvePermits(RateLimitDimension dimension, RateLimitProperties.Rule rule,
                               boolean byUser) {
        if (dimension == RateLimitDimension.GLOBAL) {
            return rule.getPermits();
        }
        return byUser ? rule.getPermits() : rule.effectiveGuestPermits();
    }

    /**
     * 拼限流 key。
     *
     * <p>USER 维度取不到 uid 时自动落到 IP 桶 —— 这是「游客没有 ID」的解法：
     * 有身份享受独立额度，没身份退回共享 IP 桶，调用方不需要分别处理。
     * 但 IP 总量由 RateLimitFilter 的全局闸门兜底，换账号也绕不过去。
     */
    private String buildKey(String group, RateLimitDimension dimension, Long uid) {
        return switch (dimension) {
            case GLOBAL -> "rl:" + group + ":global";
            case IP -> "rl:" + group + ":ip:" + clientIp();
            case USER -> uid != null
                    ? "rl:" + group + ":u:" + uid
                    : "rl:" + group + ":ip:" + clientIp();
        };
    }

    private String clientIp() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return ClientIpResolver.resolve(sra.getRequest());
        }
        return "unknown";
    }
}
