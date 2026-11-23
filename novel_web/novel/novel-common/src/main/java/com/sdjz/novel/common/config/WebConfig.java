package com.sdjz.novel.common.config;


import com.sdjz.novel.common.interceptor.AuthInterceptor;
import com.sdjz.novel.common.interceptor.AdminAuthInterceptor;
import com.sdjz.novel.common.interceptor.OptionalAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;
    private final OptionalAuthInterceptor optionalAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1) 可选鉴权：公开接口也解析 token，让登录用户能按 uid 限流。
        //    必须注册在强制鉴权之前，否则强制鉴权拿不到已解析的 uid。
        registry.addInterceptor(optionalAuthInterceptor)
                .addPathPatterns("/front/**")
                .excludePathPatterns(
                        "/front/auth/**",
                        "/front/resource/img_verify_code"
                );

        // 2) 强制鉴权：仅个人中心，缺 token 直接拒绝（语义与改造前一致）
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/front/me/**");

        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/auth/login");
    }
}

