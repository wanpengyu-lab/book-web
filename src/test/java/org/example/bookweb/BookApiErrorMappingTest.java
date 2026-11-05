package org.example.bookweb;

import org.example.bookweb.controller.BookController;
import org.example.bookweb.service.BookService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * v0.1 错误码映射测试（PRD 3.1）：不需要真实数据库。
 * 缺字段 -> 400 + 40001；类型错误 -> 400 + 40001；重复编号 -> 409 + 40901。
 */
@WebMvcTest(BookController.class)
class BookApiErrorMappingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @Test
    void missingFieldReturns400AndCode40001() throws Exception {
        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"author\":\"刘慈欣\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("title 不能为空"));
    }

    @Test
    void wrongFieldTypeReturns400AndCode40001() throws Exception {
        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"abc\",\"title\":\"三体\",\"author\":\"刘慈欣\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void duplicateKeyReturns409AndCode40901() throws Exception {
        Mockito.when(bookService.addBook(Mockito.anyInt(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new DuplicateKeyException("Duplicate entry '1' for key 'PRIMARY'"));
        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"title\":\"三体\",\"author\":\"刘慈欣\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("该编号已存在，添加失败"));
    }
}
