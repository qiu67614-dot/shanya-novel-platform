package com.sdjz.novel.common.interceptor;

import com.sdjz.novel.common.context.AdminContext;
import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.exception.ErrorCode;
import com.sdjz.novel.common.util.AdminJwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {
    private final AdminJwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");
        try {
            if (token == null || token.isBlank()) throw new IllegalArgumentException("missing token");
            AdminContext.setAdminId(jwtUtil.getAdminId(token));
            return true;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "管理员登录已失效或无权限");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AdminContext.clear();
    }
}
