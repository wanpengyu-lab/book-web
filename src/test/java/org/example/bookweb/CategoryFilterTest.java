package org.example.bookweb;

import org.example.bookweb.dto.AddBookRequest;
import org.example.bookweb.dto.UpdateBookRequest;
import org.example.bookweb.entity.Book;
import org.example.bookweb.mapper.BookMapper;
import org.example.bookweb.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * v0.5 分类过滤测试（PRD v0.5 验收：/api/books?category= 过滤正确）：
 * - category 精确过滤下推 SQL（findByCategory / countByCategory 同口径）；
 * - keyword + category 可组合，翻页参数正确；
 * - 无/空白 category 走原 keyword 口径，不误触分类过滤；
 * - 新增/更新时 category 缺省归一化「未分类」（旧数据口径一致）、total_copies 缺省 1。
 */
class CategoryFilterTest {

    private BookMapper bookMapper;
    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookMapper = mock(BookMapper.class);
        bookService = new BookService(bookMapper);
    }

    @Test
    void categoryExactFilterPushedDownToSql() {
        when(bookMapper.findByCategory(eq("科幻"), anyString(), anyInt(), anyInt()))
                .thenReturn(List.<Book>of());
        when(bookMapper.countByCategory(eq("科幻"), anyString())).thenReturn(3);

        bookService.searchBooks(null, "科幻", 1, 5);

        // 精确匹配（非 LIKE）：category 原样下推，keyword 为空串不过滤，首页 offset=0
        verify(bookMapper).findByCategory("科幻", "", 5, 0);
        verify(bookMapper).countByCategory("科幻", "");
    }

    @Test
    void categoryAndKeywordCombined() {
        when(bookMapper.findByCategory(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.<Book>of());
        when(bookMapper.countByCategory(anyString(), anyString())).thenReturn(1);

        bookService.searchBooks("刘", "科幻", 2, 5);

        // B 阶段列表页会同传两者：组合条件整体下推，翻页 offset 保持
        verify(bookMapper).findByCategory("科幻", "刘", 5, 5);
        verify(bookMapper).countByCategory("科幻", "刘");
    }

    @Test
    void noCategoryKeepsKeywordOnlyPath() {
        when(bookMapper.search(anyString(), anyInt(), anyInt())).thenReturn(List.<Book>of());
        when(bookMapper.countByKeyword(anyString())).thenReturn(12);

        bookService.searchBooks("刘", null, 1, 5);
        bookService.searchBooks("刘", "  ", 1, 5);

        // 无/空白 category 不触发分类过滤（旧行为不变）
        verify(bookMapper, org.mockito.Mockito.times(2)).search("刘", 5, 0);
        verify(bookMapper, never()).findByCategory(anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void addBookDefaultsCategoryAndCopies() {
        AddBookRequest req = new AddBookRequest();
        req.setTitle("三体");
        req.setAuthor("刘慈欣");
        // category / totalCopies 均缺省

        bookService.addBook(req);

        ArgumentCaptor<String> catCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> copiesCap = ArgumentCaptor.forClass(Integer.class);
        verify(bookMapper).add(eq("三体"), eq("刘慈欣"), catCap.capture(),
                any(), any(), any(), any(), copiesCap.capture());
        assertEquals("未分类", catCap.getValue(), "缺省分类必须归一化为「未分类」（旧数据口径）");
        assertEquals(Integer.valueOf(1), copiesCap.getValue(), "total_copies 缺省 1");
    }

    @Test
    void addBookKeepsProvidedCategoryTrimmed() {
        AddBookRequest req = new AddBookRequest();
        req.setTitle("三体");
        req.setAuthor("刘慈欣");
        req.setCategory(" 科幻 ");
        req.setTotalCopies(3);

        bookService.addBook(req);

        verify(bookMapper).add(eq("三体"), eq("刘慈欣"), eq("科幻"),
                any(), any(), any(), any(), eq(3));
    }

    @Test
    void updateBookDefaultsCategoryAndCopies() {
        UpdateBookRequest req = new UpdateBookRequest();
        req.setTitle("三体");
        req.setAuthor("刘慈欣");

        bookService.updateBook(7, req);

        ArgumentCaptor<String> catCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> copiesCap = ArgumentCaptor.forClass(Integer.class);
        verify(bookMapper).update(eq(7), eq("三体"), eq("刘慈欣"), catCap.capture(),
                any(), any(), any(), any(), copiesCap.capture());
        assertEquals("未分类", catCap.getValue());
        assertEquals(Integer.valueOf(1), copiesCap.getValue());
    }
}
