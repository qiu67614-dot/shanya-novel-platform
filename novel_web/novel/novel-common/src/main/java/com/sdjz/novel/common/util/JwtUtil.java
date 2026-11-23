package com.sdjz.novel.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expire-hour}")
    private Integer expireHour;

    private SecretKey getSignKey(){
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 生成token，存放userId */
    public String generateToken(Long userId){
        long expireMs = expireHour * 3600L * 1000;
        Date expireDate = new Date(System.currentTimeMillis() + expireMs);
        Map<String,Object> claims = new HashMap<>();
        claims.put("uid",userId);
        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(expireDate)
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** 解析token获取uid，失败抛异常 */
    public Long getUid(String token){
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token);
        return jws.getBody().get("uid",Long.class);
    }
}

