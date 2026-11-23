package com.sdjz.novel.common.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口级限流标记。
 *
 * <p>注解只声明「这个接口属于哪一组、按什么维度计数」——这是代码语义；
 * 具体阈值放在配置里，改阈值不需要重新打包。
 *
 * <pre>{@code
 * @RateLimit(group = RateLimitGroup.AUTH, dimension = RateLimitDimension.IP)
 * @PostMapping("/login")
 * public RestResp<UserLoginVO> login(...) { ... }
 * }</pre>
 *
 * <p>超限抛出 {@code RateLimitException}，由全局异常处理转成 HTTP 429 +
 * {@code Retry-After}，不走 BusinessException 那条「HTTP 200 + code」的路径。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 分组，取 {@link RateLimitGroup} 中的常量，对应配置 rate-limit.groups.{group}。 */
    String group();

    /**
     * 计数维度。默认按用户；取不到用户时自动降级为 IP，
     * 所以游客接口不必单独写成 IP，除非要强制限制单个来源。
     */
    RateLimitDimension dimension() default RateLimitDimension.USER;
}
