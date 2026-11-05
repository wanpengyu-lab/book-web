package org.example.bookweb;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.bookweb.controller.AssistantController;
import org.example.bookweb.mapper.BookMapper;
import org.example.bookweb.service.AssistantService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * v0.7 AI 配置契约测试（PRD v0.7 验收：未配置 GLM_API_KEY → 明确提示，不 500 堆栈）。
 * 不出网：controller 层验证「未配置 → 200 + code 0 + data:null + message」映射；
 * service 层验证空 key 的快速失败分支。真实问答两条验收项按 6.2 挂账（key 未配置）。
 */
@WebMvcTest(controllers = AssistantController.class,
        properties = "JWT_SECRET=test-jwt-secret-0123456789abcdef0123456789abcdef")
class AssistantConfigTest {

    private static final String SECRET = "test-jwt-secret-0123456789abcdef0123456789abcdef";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssistantService assistantService;

    @Test
    void notConfiguredReturns200WithHint() throws Exception {
        when(assistantService.chat(anyString()))
                .thenThrow(new AssistantService.NotConfiguredException("AI 服务未配置"));

        mockMvc.perform(post("/api/assistant/chat")
                        .header("Authorization", "Bearer " + readerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"馆里有哪些科幻小说\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("AI 服务未配置"))
                .andExpect(jsonPath("$.data").value((Object) null));
    }

    @Test
    void blankMessageRejected40001() throws Exception {
        mockMvc.perform(post("/api/assistant/chat")
                        .header("Authorization", "Bearer " + readerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void serviceFailsFastOnBlankKey() {
        AssistantService service = new AssistantService(Mockito.mock(BookMapper.class), "", "glm-4.6");
        assertThrows(AssistantService.NotConfiguredException.class,
                () -> service.chat("馆里有哪些科幻小说"),
                "未配置 GLM_API_KEY 必须快速失败并提示，不得发起外部调用");
    }

    private String readerToken() {
        return Jwts.builder()
                .subject("4").claim("role", "reader")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key())
                .compact();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }
}
