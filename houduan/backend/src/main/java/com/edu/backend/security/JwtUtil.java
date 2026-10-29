package com.edu.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：签发与解析。
 *
 * <p>密钥从 {@code jwt.secret} 读取（生产必须通过环境变量 {@code JWT_SECRET} 注入），
 * 本地未配置时使用仅限开发环境的默认值。</p>
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret:${JWT_SECRET:}}") String secret,
                   @Value("${jwt.expiration-ms:604800000}") long expirationMs) {
        String s = (secret == null || secret.isBlank())
                ? "javahouduan-dev-default-secret-please-override-in-prod-0123456789"
                : secret;
        this.key = Keys.hmacShaKeyFor(s.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /** 签发 token，携带 userId（subject）与 role（claim），默认有效期 7 天 */
    public String generateToken(String userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** 解析并校验 token，失败抛异常 */
    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
