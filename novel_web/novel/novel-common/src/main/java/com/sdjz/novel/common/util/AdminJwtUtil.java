package com.sdjz.novel.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class AdminJwtUtil {
    @Value("${admin.jwt.secret}")
    private String secret;
    @Value("${admin.jwt.expire-hour:8}")
    private Integer expireHour;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long adminId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("aid", adminId);
        claims.put("kind", "admin");
        return Jwts.builder().setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + expireHour * 3600L * 1000L))
                .signWith(key(), SignatureAlgorithm.HS256).compact();
    }

    public Long getAdminId(String token) {
        Jws<Claims> jws = Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token);
        Claims claims = jws.getBody();
        if (!"admin".equals(claims.get("kind", String.class))) throw new IllegalArgumentException("not admin token");
        return claims.get("aid", Long.class);
    }
}
