package com.sdjz.novel.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 解析真实客户端 IP。
 *
 * <p><b>只信任 {@code X-Client-IP}</b>，由 Nginx 用 {@code $remote_addr} 覆盖写入。
 * 绝对不能读 {@code X-Forwarded-For} 的第一段：那是客户端可以随便填的，读它等于把
 * 限流的 key 交给攻击者控制，换个头就能无限绕过。若将来前置 CDN，必须在 Nginx 侧
 * 用 {@code set_real_ip_from} + {@code real_ip_header} 还原，而不是在这里改。
 */
@Slf4j
public final class ClientIpResolver {

    public static final String HEADER = "X-Client-IP";

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader(HEADER);
        if (StringUtils.hasText(forwarded)) {
            return forwarded.trim();
        }
        return request.getRemoteAddr();
    }
}
