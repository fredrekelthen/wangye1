package com.edu.backend.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试：签发与解析往返、非法 token 解析失败。
 */
public class JwtUtilTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    public void testGenerateAndParseToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60000);
        String token = jwtUtil.generateToken("userId123", "ADMIN");
        assertNotNull(token);

        Claims claims = jwtUtil.parseToken(token);
        assertEquals("userId123", claims.getSubject());
        assertEquals("ADMIN", claims.get("role", String.class));
    }

    @Test
    public void testParseInvalidTokenThrows() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60000);
        assertThrows(Exception.class, () -> jwtUtil.parseToken("not.a.valid.token"));
    }
}
