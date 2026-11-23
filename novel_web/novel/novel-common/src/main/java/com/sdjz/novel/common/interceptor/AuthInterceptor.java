package com.sdjz.novel.common.interceptor;

import com.sdjz.novel.common.context.UserContext;
import com.sdjz.novel.common.exception.ErrorCode;
import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        // 获取请求头 Authorization，无 Bearer
        String token = request.getHeader("Authorization");
        log.info("【鉴权拦截】请求地址：{}", request.getRequestURI());

        if (token == null || token.isBlank()) {
            log.info("【鉴权拦截】token为空，拒绝访问");
            throw new BusinessException(ErrorCode.TOKEN_EXPIRE, "登录已过期，请重新登录");
        }

        Long uid;
        try {
            uid = jwtUtil.getUid(token);
            log.info("【鉴权成功】用户uid={}", uid);
            // 存入用户上下文 UserContext
            UserContext.setUid(uid);
        } catch (Exception e) {
            log.info("【鉴权失败】token解析异常，msg={}", e.getMessage());
            throw new BusinessException(ErrorCode.TOKEN_EXPIRE, "解析失败");
        }
        return true;
    }


    /** 请求完成后清理ThreadLocal，必须！ */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        UserContext.clear();
    }
}

