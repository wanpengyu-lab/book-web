package org.example.bookweb;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.bookweb.controller.AuthController;
import org.example.bookweb.controller.BookController;
import org.example.bookweb.entity.User;
import org.example.bookweb.mapper.UserMapper;
import org.example.bookweb.service.AuthService;
import org.example.bookweb.service.BookService;
import org.example.bookweb.service.BorrowService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * v0.3 鉴权测试（PRD v0.3 验收硬门槛）：不需要真实数据库。
 * 未登录删书 401 / reader 建书 403 / 过期 token 401，另覆盖注册重复 40901、登录成功与失败路径。
 * WebConfig（WebMvcConfigurer）在 slice 内加载：JWT_SECRET 走测试属性，JwtInterceptor 拦 /api/**。
 */
@WebMvcTest(controllers = {AuthController.class, BookController.class},
        properties = "JWT_SECRET=test-jwt-secret-0123456789abcdef0123456789abcdef")
@Import(AuthService.class)
class AuthTest {

    /** 测试密钥 48 字节（HS256 要求 ≥32 字节） */
    private static final String SECRET = "test-jwt-secret-0123456789abcdef0123456789abcdef";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private BorrowService borrowService;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String tokenFor(String userId, String role, Date expiration) {
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(expiration)
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
    }

    @Test
    void deleteBookWithoutTokenReturns401And40101() throws Exception {
        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("未登录或缺少 Bearer token"));
    }

    @Test
    void readerCreateBookReturns403And40301() throws Exception {
        String token = tokenFor("2", "reader", new Date(System.currentTimeMillis() + 3_600_000));
        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"三体\",\"author\":\"刘慈欣\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301))
                .andExpect(jsonPath("$.message").value("无权限：该操作需要 admin"));
    }

    @Test
    void expiredTokenReturns401WithClearMessage() throws Exception {
        String expired = tokenFor("2", "reader", new Date(System.currentTimeMillis() - 1_000));
        mockMvc.perform(delete("/api/books/1").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("token 已过期，请重新登录"));
    }

    @Test
    void registerCreatesReaderAndDuplicateReturns40901() throws Exception {
        when(userMapper.countByUsername("reader1")).thenReturn(0);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"reader1\",\"password\":\"pw123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        // 哈希入库：insert 收到的是 BCrypt 哈希而非明文
        verify(userMapper).insert(Mockito.eq("reader1"), startsWith("$2"), Mockito.eq("reader"));

        when(userMapper.countByUsername("reader1")).thenReturn(1);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"reader1\",\"password\":\"pw123456\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    void loginReturnsTokenAndUserView() throws Exception {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User admin = new User();
        admin.setId(1);
        admin.setUsername("admin");
        admin.setPasswordHash(encoder.encode("admin-pw-123"));
        admin.setRole("admin");
        when(userMapper.findByUsername("admin")).thenReturn(admin);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin-pw-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.id").value(1))
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andExpect(jsonPath("$.data.user.role").value("admin"));
    }

    @Test
    void loginWrongPasswordReturns401And40101() throws Exception {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User admin = new User();
        admin.setId(1);
        admin.setUsername("admin");
        admin.setPasswordHash(encoder.encode("admin-pw-123"));
        admin.setRole("admin");
        when(userMapper.findByUsername("admin")).thenReturn(admin);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong-pw\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }
}
