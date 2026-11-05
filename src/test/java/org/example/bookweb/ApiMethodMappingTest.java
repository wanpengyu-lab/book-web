package org.example.bookweb;

import org.example.bookweb.controller.StatsController;
import org.example.bookweb.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * v0.3 补充：请求方法与接口不匹配（405 场景）映射为 400 + 40001，
 * 不再落入 50001 兜底（用户拍板：不新增错误码）。
 * 以 GET-only 的 /api/stats 为样本；slice 需加载 StatsController 才会在映射期抛 405。
 */
@WebMvcTest(controllers = StatsController.class, properties = "JWT_SECRET=test-jwt-secret-0123456789abcdef0123456789abcdef")
class ApiMethodMappingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @Test
    void postToGetOnlyEndpointReturns400And40001() throws Exception {
        mockMvc.perform(post("/api/stats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("请求方法不支持"));
    }
}
