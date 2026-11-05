package org.example.bookweb;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.bookweb.controller.IsbnController;
import org.example.bookweb.dto.IsbnBookResponse;
import org.example.bookweb.service.IsbnService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * v0.7 ISBN 契约测试（PRD v0.7）：非数字/超长 ISBN → 400 + 40001；
 * 命中 → 200 data 携带书目字段；干净未命中 → 200 + code 0 + data:null + 明确 message。
 * 不出网：IsbnService 为 mock，外部调用失败/超时路径由服务实现保证（UpstreamUnavailableException → 200 notice）。
 */
@WebMvcTest(controllers = IsbnController.class,
        properties = "JWT_SECRET=test-jwt-secret-0123456789abcdef0123456789abcdef")
class IsbnContractTest {

    private static final String SECRET = "test-jwt-secret-0123456789abcdef0123456789abcdef";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IsbnService isbnService;

    @Test
    void nonNumericIsbnRejected40001() throws Exception {
        mockMvc.perform(get("/api/isbn/abc123x").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void oversizedIsbnRejected40001() throws Exception {
        String isbn21 = "9".repeat(21);
        mockMvc.perform(get("/api/isbn/" + isbn21).header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void readerForbidden40301() throws Exception {
        mockMvc.perform(get("/api/isbn/9787536692930").header("Authorization", "Bearer " + readerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void hitReturnsBookFields() throws Exception {
        IsbnBookResponse book = new IsbnBookResponse();
        book.setTitle("三体");
        book.setAuthor("刘慈欣");
        book.setPublisher("重庆出版社");
        book.setCoverUrl("https://covers.openlibrary.org/b/id/1-L.jpg");
        book.setDescription("简介");
        when(isbnService.lookup("9787536692930")).thenReturn(book);

        mockMvc.perform(get("/api/isbn/9787536692930").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("三体"))
                .andExpect(jsonPath("$.data.author").value("刘慈欣"))
                .andExpect(jsonPath("$.data.coverUrl").isNotEmpty());
    }

    @Test
    void missReturns200WithNoticeAndNullData() throws Exception {
        when(isbnService.lookup("9999999999999")).thenReturn(null);

        mockMvc.perform(get("/api/isbn/9999999999999").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value((Object) null))
                .andExpect(jsonPath("$.message").value("未查到该书目，请手动录入"));
    }

    private String adminToken() {
        return token("admin");
    }

    private String readerToken() {
        return token("reader");
    }

    private String token(String role) {
        return Jwts.builder()
                .subject("3").claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key())
                .compact();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }
}
