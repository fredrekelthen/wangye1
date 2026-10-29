package com.edu.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户读取工具：优先 JWT（SecurityContext），其次兼容旧 HttpSession。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 从 JWT 取当前用户 id，未登录返回 null */
    public static String getJwtUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String principal) {
            return principal;
        }
        return null;
    }

    /** JWT 是否携带 ADMIN 角色 */
    public static boolean isJwtAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    /** 当前用户 id：优先 JWT，其次兼容旧 session */
    public static String getCurrentUserId(HttpServletRequest request) {
        String jwtUserId = getJwtUserId();
        if (jwtUserId != null) {
            return jwtUserId;
        }
        HttpSession session = request.getSession(false);
        return session == null ? null : (String) session.getAttribute("userId");
    }
}
