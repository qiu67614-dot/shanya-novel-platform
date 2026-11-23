package com.sdjz.novel.common.interceptor;

import com.sdjz.novel.common.context.UserContext;
import com.sdjz.novel.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 可选鉴权：有合法 token 就解析出 uid，没有就当游客，<b>永远放行</b>。
 *
 * <p>存在的理由：公开接口（首页、榜单、目录、章节正文）原来完全不解析 token，
 * 登录用户在这些页面上也被当作游客，只能和整个出口 IP 共享一个限流桶。
 * 加上这一层后，登录用户能按 uid 独立计数，游客则自动退回 IP 桶。
 *
 * <p><b>不能与 {@link AuthInterceptor} 合并</b>：那个是强制鉴权，缺 token 必须 401。
 * 如果为了拿 uid 而放宽它，{@code /front/me/**} 就失去保护了。两者职责不同，
 * 同时注册、按顺序执行：本拦截器先解析，强制鉴权再判断 UserContext 是否为空。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OptionalAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isBlank()) {
            return true;
        }
        try {
            UserContext.setUid(jwtUtil.getUid(token));
        } catch (Exception e) {
            // token 过期或伪造：按游客处理，不报错。真正的拦截交给强制鉴权那条链
            UserContext.clear();
            log.debug("可选鉴权：token 无效，按游客处理，uri={}", request.getRequestURI());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        // 公开接口只经过本拦截器，没有别的地方清理 ThreadLocal，这里必须清
        UserContext.clear();
    }
}
