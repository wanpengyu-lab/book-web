package org.example.bookweb.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.bookweb.dto.Result;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.SecretKey;

/**
 * JWT 拦截器（v0.3）。拦截范围由 WebConfig 限定为 /api/**，规则严格按 PRD v0.3 放行表：
 * 放行：GET /api/stats、GET /api/books（含详情）、/api/auth/**；
 * POST/PUT/DELETE /api/books/** 需 admin（40301）；其余 /api/** 需登录（40101）。
 * 旧无前缀端点不在 /api/** 内，保持现状至 v0.10 统一删除。不得临时加白名单。
 */
public class JwtInterceptor implements HandlerInterceptor {

    private final SecretKey secretKey;
    private final ObjectMapper objectMapper;

    public JwtInterceptor(SecretKey secretKey, ObjectMapper objectMapper) {
        this.secretKey = secretKey;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws java.io.IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 放行：开放只读接口（GET）与认证接口。
        // v0.6：GET 放行收窄为「/api/stats、/api/books 列表、/api/books/{数字id} 详情」——
        // 不得放行 /api/books/{id}/related 等子路径（v0.6 明确其按「其余 /api/** 需登录」处理）。
        if ("GET".equals(method)) {
            if (path.equals("/api/stats") || path.equals("/api/books")) {
                return true;
            }
            if (path.startsWith("/api/books/")
                    && path.substring("/api/books/".length()).matches("\\d+")) {
                return true;
            }
        }
        if (path.startsWith("/api/auth/")) {
            return true;
        }

        // 其余 /api/** 一律需要登录；不匹配 → 40101
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return reject(response, HttpStatus.UNAUTHORIZED, 40101, "未登录或缺少 Bearer token");
        }
        Claims claims;
        int userId;
        try {
            claims = Jwts.parser().verifyWith(secretKey).build()
                    .parseSignedClaims(auth.substring(7)).getPayload();
            userId = Integer.parseInt(claims.getSubject());
        } catch (ExpiredJwtException e) {
            return reject(response, HttpStatus.UNAUTHORIZED, 40101, "token 已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException e) {
            return reject(response, HttpStatus.UNAUTHORIZED, 40101, "token 无效");
        }

        // userId / role 存入 request attribute，供借阅、记录查询等接口使用
        request.setAttribute("userId", userId);
        request.setAttribute("role", claims.get("role", String.class));

        // v0.4：借/还（PUT /api/books/{id}/borrow|return）为登录用户操作，不走 admin 检查（PRD v0.4）
        // v0.6：预约（POST /api/books/{id}/reserve）同为登录用户操作（PRD v0.6），同类豁免 admin 检查
        boolean borrowReturnReserve = path.startsWith("/api/books/")
                && (path.endsWith("/borrow") || path.endsWith("/return") || path.endsWith("/reserve"));

        // POST/PUT/DELETE /api/books/** 需 admin（v0.3 拦截规则）
        if ((path.equals("/api/books") || path.startsWith("/api/books/"))
                && ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method))
                && !borrowReturnReserve
                && !"admin".equals(claims.get("role", String.class))) {
            return reject(response, HttpStatus.FORBIDDEN, 40301, "无权限：该操作需要 admin");
        }

        // v0.7：GET /api/isbn/** 需 admin（PRD v0.7；路径不在放行表，登录后在此强制角色）
        if (path.startsWith("/api/isbn") && !"admin".equals(claims.get("role", String.class))) {
            return reject(response, HttpStatus.FORBIDDEN, 40301, "无权限：该操作需要 admin");
        }
        return true;
    }

    /** 拦截器内直接以 Result{code, message, data} 结构写回（3.1 统一响应） */
    private boolean reject(HttpServletResponse response, HttpStatus status, int code, String message)
            throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, message)));
        return false;
    }
}
