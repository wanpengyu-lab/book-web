package org.example.bookweb;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.bookweb.controller.StatsController;
import org.example.bookweb.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API 错误映射补充测试（用户拍板项，均不新增错误码）：
 * - v0.3：请求方法不匹配（405 场景）→ 400 + 40001；以 GET-only 的 /api/stats 为样本；
 * - v0.4：无映射路径（NoResourceFoundException）→ 404 + 40401；
 *   /api/** 先过 JwtInterceptor，需带合法 token 才会落到资源 handler。
 */
@WebMvcTest(controllers = StatsController.class, properties = "JWT_SECRET=test-jwt-secret-0123456789abcdef0123456789abcdef")
class ApiMethodMappingTest {

    private static final String SECRET = "test-jwt-secret-0123456789abcdef0123456789abcdef";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    // v0.5：StatsController 增加 /api/dashboard，依赖 DashboardService（slice 内需注入 mock）
    @MockitoBean
    private org.example.bookweb.service.DashboardService dashboardService;

    @Test
    void postToGetOnlyEndpointReturns400And40001() throws Exception {
        mockMvc.perform(post("/api/stats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("请求方法不支持"));
    }

    @Test
    void nonexistentApiPathWithValidTokenReturns404And40401() throws Exception {
        String token = Jwts.builder()
                .subject("2").claim("role", "reader")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key())
                .compact();
        mockMvc.perform(get("/api/nonexistent").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401))
                .andExpect(jsonPath("$.message").value("资源不存在"));
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }
}
