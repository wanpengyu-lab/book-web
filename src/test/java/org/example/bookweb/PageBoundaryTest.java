package org.example.bookweb;

import org.example.bookweb.entity.Book;
import org.example.bookweb.mapper.BookMapper;
import org.example.bookweb.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * v0.1 分页边界单测（不依赖数据库，mock BookMapper 捕获实际下推参数）。
 * 覆盖：page=0 / 负数 / size=0 / size>100。
 */
class PageBoundaryTest {

    private BookMapper bookMapper;
    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookMapper = mock(BookMapper.class);
        bookService = new BookService(bookMapper);
        when(bookMapper.countAll()).thenReturn(12);
        when(bookMapper.findPage(org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(Collections.<Book>emptyList());
    }

    private Map<String, Object> pageAndCapture(int page, int size, ArgumentCaptor<Integer> sizeCap,
                                               ArgumentCaptor<Integer> offsetCap) {
        Map<String, Object> result = bookService.getBooksPage(page, size);
        verify(bookMapper).findPage(sizeCap.capture(), offsetCap.capture());
        return result;
    }

    @Test
    void pageZeroClampedToOne() {
        ArgumentCaptor<Integer> sizeCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> offsetCap = ArgumentCaptor.forClass(Integer.class);
        Map<String, Object> result = pageAndCapture(0, 5, sizeCap, offsetCap);

        assertEquals(1, result.get("page"));
        assertEquals(0, offsetCap.getValue());
        assertEquals(5, sizeCap.getValue());
    }

    @Test
    void negativePageClampedToOne() {
        ArgumentCaptor<Integer> sizeCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> offsetCap = ArgumentCaptor.forClass(Integer.class);
        Map<String, Object> result = pageAndCapture(-3, 5, sizeCap, offsetCap);

        assertEquals(1, result.get("page"));
        assertEquals(0, offsetCap.getValue());
        assertEquals(5, sizeCap.getValue());
    }

    @Test
    void sizeZeroClampedToDefaultFive() {
        ArgumentCaptor<Integer> sizeCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> offsetCap = ArgumentCaptor.forClass(Integer.class);
        Map<String, Object> result = pageAndCapture(1, 0, sizeCap, offsetCap);

        assertEquals(5, result.get("size"));
        assertEquals(5, sizeCap.getValue());
        assertEquals(0, offsetCap.getValue());
    }

    @Test
    void sizeAbove100ClampedTo100() {
        ArgumentCaptor<Integer> sizeCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> offsetCap = ArgumentCaptor.forClass(Integer.class);
        Map<String, Object> result = pageAndCapture(1, 500, sizeCap, offsetCap);

        assertEquals(100, result.get("size"));
        assertEquals(100, sizeCap.getValue());
        assertEquals(0, offsetCap.getValue());
    }
}
