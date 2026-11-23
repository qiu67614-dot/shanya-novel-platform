package com.sdjz.novel.common.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 全局 IP 粗筛闸门。
 *
 * <p>只做一件事：按 IP 限制「单位时间内的总请求数」，挡住单机无差别刷全站这类
 * 不需要业务知识就能识别的流量。不做精细判断，那属于 {@link RateLimitAspect}。
 *
 * <p><b>为什么用 Filter 而不是 Interceptor</b>：Filter 在 DispatcherServlet 之前执行，
 * 拒绝时不消耗路由匹配、参数绑定和 MVC 拦截链；而且能覆盖全部路径，不需要在
 * 每个 Controller 上重复声明。
 *
 * <p><b>必须自己写响应</b>：Filter 抛出的异常不会被 {@code @RestControllerAdvice}
 * 捕获（那个 Advice 在 MVC 层内部），抛异常只会变成 500 加一堆无意义堆栈。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String GATE_KEY_PREFIX = "rl:gate:ip:";

    private final RateLimitProperties properties;
    private final CompositeRateLimiter limiter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.contains("img_verify_code")
                || isStaticResource(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        RateLimitProperties.Gate gate = properties.getGate();
        if (!properties.isEnabled() || !gate.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        String ip = ClientIpResolver.resolve(request);
        String key = GATE_KEY_PREFIX + ip;

        // 闸门本身用 OPEN：它的定位是「兜底」，不能因为 Redis 抖动把全站挡死
        RateLimitResult result = limiter.tryAcquire(
                key, gate.getPermits(), gate.getWindow(), RateLimitFallback.OPEN);

        if (!result.allowed()) {
            log.info("全局闸门拦截 ip={} retryAfter={}s path={}",
                    ip, result.retryAfterSeconds(), request.getRequestURI());
            writeTooManyRequests(response, result.retryAfterSeconds());
            return;
        }

        chain.doFilter(request, response);
    }

    private void writeTooManyRequests(HttpServletResponse response, long retryAfterSeconds)
            throws IOException {
        // 显式 429：Nginx 侧已把 limit_req_status 设为 429，两层状态码必须一致，
        // 否则前端无法用同一段逻辑处理。
        // 用 Spring 的 HttpStatus 而不是 HttpServletResponse.SC_TOO_MANY_REQUESTS：
        // 后者是 Servlet 6.0 才加入的常量，低版本容器编译不过
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"code\":\"A0429\",\"data\":null,\"message\":\"操作过于频繁，请 "
                        + retryAfterSeconds + " 秒后重试\"}");
    }

    private static boolean isStaticResource(String path) {
        return path.endsWith(".js") || path.endsWith(".css") || path.endsWith(".map")
                || path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg")
                || path.endsWith(".gif") || path.endsWith(".svg") || path.endsWith(".ico")
                || path.endsWith(".woff") || path.endsWith(".woff2") || path.endsWith(".ttf");
    }
}
