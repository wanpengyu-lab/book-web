package org.example.bookweb;

import org.example.bookweb.dto.BookPageResponse;
import org.example.bookweb.dto.StatsResponse;
import org.example.bookweb.entity.Book;
import org.example.bookweb.mapper.BookMapper;
import org.example.bookweb.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * v0.2 统计一致性测试（修 G2）：12 条数据、size=5 翻到第 3 页，
 * 统计三数字仍等于全库真值（SQL 聚合口径），不受当前页内容影响。
 */
class StatsConsistencyTest {

    private static final int TOTAL = 12;
    private static final int AVAILABLE = 7;
    private static final int BORROWED = 5;

    private BookMapper bookMapper;
    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookMapper = mock(BookMapper.class);
        bookService = new BookService(bookMapper);
        when(bookMapper.countAll()).thenReturn(TOTAL);
        when(bookMapper.countByBorrowed(false)).thenReturn(AVAILABLE);
        when(bookMapper.countByBorrowed(true)).thenReturn(BORROWED);
        when(bookMapper.search(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(Collections.<Book>emptyList());
        when(bookMapper.countByKeyword(org.mockito.ArgumentMatchers.anyString())).thenReturn(TOTAL);
    }

    @Test
    void statsStayWholeLibraryAcrossAllPages() {
        // size=5 翻页 1→2→3，每页统计均为全库真值而非当前页计数
        for (int page = 1; page <= 3; page++) {
            BookPageResponse pageData = bookService.searchBooks("", page, 5);
            assertEquals(page, pageData.getPage());

            StatsResponse stats = bookService.getStats();
            assertEquals(TOTAL, stats.getTotal(), "第 " + page + " 页 total 应为全库真值");
            assertEquals(AVAILABLE, stats.getAvailable());
            assertEquals(BORROWED, stats.getBorrowed());
            assertEquals(TOTAL, stats.getAvailable() + stats.getBorrowed(), "在馆+在借 应等于总量");
        }
    }

    @Test
    void statsIgnoreCurrentPageContent() {
        // G2 回归：当前页 5 条全部在借，若统计按当前页累加会得到 available=0；SQL 聚合应仍是 7
        List<Book> pageAllBorrowed = List.of(
                book(1, true), book(2, true), book(3, true), book(4, true), book(5, true));
        when(bookMapper.search(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(pageAllBorrowed);

        bookService.searchBooks("", 1, 5);

        StatsResponse stats = bookService.getStats();
        assertEquals(AVAILABLE, stats.getAvailable(), "统计不得来自当前页累加");
        assertEquals(BORROWED, stats.getBorrowed());
    }

    @Test
    void categoriesPlaceholderUntilV05() {
        // v0.5 切换真实分类：当前为「全部」占位（PRD v0.2 交付物约定）
        StatsResponse stats = bookService.getStats();
        assertEquals(1, stats.getCategories().size());
        assertEquals("全部", stats.getCategories().get(0).getName());
        assertEquals(TOTAL, stats.getCategories().get(0).getCount());
    }

    private Book book(int id, boolean borrowed) {
        Book b = new Book();
        b.setId(id);
        b.setBorrowed(borrowed);
        return b;
    }
}
