package org.example.bookweb;

import org.example.bookweb.entity.Book;
import org.example.bookweb.mapper.BookMapper;
import org.example.bookweb.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 分页边界单测（不依赖数据库，mock BookMapper 捕获实际下推参数）。
 * v0.1 覆盖 page=0 / 负数 / size=0 / size>100；
 * v0.10：旧无前缀端点 /books/page 与 getBooksPage 已删除（PRD v0.10 删除清单），
 * 同一套钳制逻辑在 /api/books 的 searchBooks 中，断言迁移到该口径（keyword/category 传空）。
 */
class PageBoundaryTest {

    private BookMapper bookMapper;
    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookMapper = mock(BookMapper.class);
        bookService = new BookService(bookMapper);
        when(bookMapper.countByKeyword(anyString())).thenReturn(12);
        when(bookMapper.search(anyString(), anyInt(), anyInt()))
                .thenReturn(Collections.<Book>emptyList());
    }

    private void pageAndCapture(int page, int size, ArgumentCaptor<Integer> sizeCap,
                                ArgumentCaptor<Integer> offsetCap) {
        bookService.searchBooks(null, null, page, size);
        verify(bookMapper).search(anyString(), sizeCap.capture(), offsetCap.capture());
    }

    @Test
    void pageZeroClampedToOne() {
        ArgumentCaptor<Integer> sizeCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> offsetCap = ArgumentCaptor.forClass(Integer.class);
        pageAndCapture(0, 5, sizeCap, offsetCap);

        assertEquals(0, offsetCap.getValue());
        assertEquals(5, sizeCap.getValue());
    }

    @Test
    void negativePageClampedToOne() {
        ArgumentCaptor<Integer> sizeCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> offsetCap = ArgumentCaptor.forClass(Integer.class);
        pageAndCapture(-3, 5, sizeCap, offsetCap);

        assertEquals(0, offsetCap.getValue());
        assertEquals(5, sizeCap.getValue());
    }

    @Test
    void sizeZeroClampedToDefaultFive() {
        ArgumentCaptor<Integer> sizeCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> offsetCap = ArgumentCaptor.forClass(Integer.class);
        pageAndCapture(1, 0, sizeCap, offsetCap);

        assertEquals(5, sizeCap.getValue());
        assertEquals(0, offsetCap.getValue());
    }

    @Test
    void sizeAbove100ClampedTo100() {
        ArgumentCaptor<Integer> sizeCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> offsetCap = ArgumentCaptor.forClass(Integer.class);
        pageAndCapture(1, 500, sizeCap, offsetCap);

        assertEquals(100, sizeCap.getValue());
        assertEquals(0, offsetCap.getValue());
    }
}
