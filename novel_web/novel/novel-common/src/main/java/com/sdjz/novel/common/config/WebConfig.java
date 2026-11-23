package com.sdjz.novel.common.config;


import com.sdjz.novel.common.interceptor.AuthInterceptor;
import com.sdjz.novel.common.interceptor.AdminAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                        "/front/me/**"
                )
                // 免登录放行接口
                .excludePathPatterns(
                        "/front/auth/**",
                        "/front/resource/img_verify_code",
                        "/front/books/**",
                        "/front/categories",
                        "/front/ranks/**",
                        "/front/chapters/**"
                );
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/auth/login");
    }
}

