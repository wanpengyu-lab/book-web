package org.example.bookweb;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.bookweb.controller.BookController;
import org.example.bookweb.dto.AddBookRequest;
import org.example.bookweb.dto.UpdateBookRequest;
import org.example.bookweb.service.BookService;
import org.example.bookweb.service.BorrowService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * v0.1 错误码映射测试（PRD 3.1）：不需要真实数据库。
 * 缺字段 -> 400 + 40001；请求体格式/类型错误 -> 400 + 40001；重复键冲突 -> 409 + 40901。
 * 注：AddBookRequest / UpdateBookRequest 均无 id 字段，请求体不出现 id。
 */
// v0.3 起 slice 会加载 WebConfig（注册 JwtInterceptor），测试属性提供 JWT_SECRET
@WebMvcTest(controllers = BookController.class, properties = "JWT_SECRET=test-jwt-secret-0123456789abcdef0123456789abcdef")
class BookApiErrorMappingTest {

    private static final String SECRET = "test-jwt-secret-0123456789abcdef0123456789abcdef";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private BorrowService borrowService;

    /**
     * v0.10：被测路径由旧无前缀端点迁移到 /api 对应端点（PRD v0.10 删除清单）。
     * POST/PUT /api/books/** 过 JwtInterceptor 需 admin，此处携带合法 admin token。
     */
    private String adminToken() {
        return Jwts.builder()
                .subject("1").claim("role", "admin")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key())
                .compact();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void missingFieldReturns400AndCode40001() throws Exception {
        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\":\"刘慈欣\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("title 不能为空"));
    }

    @Test
    void wrongFieldTypeReturns400AndCode40001() throws Exception {
        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":[1,2],\"author\":\"刘慈欣\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void duplicateKeyReturns409AndCode40901() throws Exception {
        Mockito.when(bookService.addBook(Mockito.any(AddBookRequest.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry for key 'PRIMARY'"));
        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"三体\",\"author\":\"刘慈欣\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("该编号已存在，添加失败"));
    }

    @Test
    void updateReadsIdFromPathOnly() throws Exception {
        Mockito.when(bookService.updateBook(Mockito.anyInt(), Mockito.any(UpdateBookRequest.class)))
                .thenReturn(true);
        // body 无 id，也不接受 body 携带 id（未知字段被忽略，不影响校验）
        mockMvc.perform(put("/api/books/7")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"围城\",\"author\":\"钱钟书\",\"id\":99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        // 路径 id 7 必须原样下传，body 里的 99 不得覆盖
        org.mockito.ArgumentCaptor<UpdateBookRequest> cap =
                org.mockito.ArgumentCaptor.forClass(UpdateBookRequest.class);
        Mockito.verify(bookService).updateBook(Mockito.eq(7), cap.capture());
        org.junit.jupiter.api.Assertions.assertEquals("围城", cap.getValue().getTitle());
        org.junit.jupiter.api.Assertions.assertEquals("钱钟书", cap.getValue().getAuthor());
    }
}
