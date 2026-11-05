package org.example.bookweb;

import org.example.bookweb.dto.BookPageResponse;
import org.example.bookweb.entity.Book;
import org.example.bookweb.mapper.BookMapper;
import org.example.bookweb.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * v0.2 服务端搜索测试（修 G3）：keyword 命中分布在不同页的数据时，
 * 每页请求都携带 keyword（翻页保持搜索条件），total 为命中全量而非当前页条数。
 */
class SearchAcrossPagesTest {

    private BookMapper bookMapper;
    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookMapper = mock(BookMapper.class);
        bookService = new BookService(bookMapper);
    }

    @Test
    void keywordCarriedOnEveryPageRequest() {
        when(bookMapper.search(anyString(), anyInt(), anyInt())).thenReturn(List.<Book>of());
        when(bookMapper.countByKeyword(anyString())).thenReturn(12);

        // 第 1 页：offset=0；第 3 页：offset=10 —— keyword 均下推 SQL
        bookService.searchBooks("刘", null, 1, 5);
        bookService.searchBooks("刘", null, 3, 5);

        ArgumentCaptor<String> kwCap = ArgumentCaptor.forClass(String.class);
        var sizeCap = ArgumentCaptor.forClass(Integer.class);
        var offsetCap = ArgumentCaptor.forClass(Integer.class);
        verify(bookMapper, org.mockito.Mockito.times(2))
                .search(kwCap.capture(), sizeCap.capture(), offsetCap.capture());

        assertEquals(List.of("刘", "刘"), kwCap.getAllValues(), "翻页必须保持搜索条件");
        assertEquals(List.of(5, 5), sizeCap.getAllValues());
        assertEquals(List.of(0, 10), offsetCap.getAllValues());
    }

    @Test
    void totalIsHitCountNotPageSize() {
        // keyword 命中 12 条分布在不同页：任一页 total 都是命中全量 12，不是本页 5
        when(bookMapper.search(anyString(), anyInt(), anyInt())).thenReturn(List.<Book>of(new Book()));
        when(bookMapper.countByKeyword(anyString())).thenReturn(12);

        for (int page = 1; page <= 3; page++) {
            BookPageResponse r = bookService.searchBooks("科幻", null, page, 5);
            assertEquals(12, r.getTotal(), "第 " + page + " 页 total 应为命中全量");
        }
        verify(bookMapper, org.mockito.Mockito.times(3)).countByKeyword("科幻");
    }

    @Test
    void noHitReturnsEmptyListAndZeroTotalWithoutError() {
        when(bookMapper.search(anyString(), anyInt(), anyInt())).thenReturn(List.<Book>of());
        when(bookMapper.countByKeyword(anyString())).thenReturn(0);

        BookPageResponse r = bookService.searchBooks("不存在的关键词", null, 1, 5);

        assertEquals(0, r.getTotal());
        assertTrue(r.getList().isEmpty(), "无结果应返回空 list，不报错");
    }

    @Test
    void blankKeywordMeansNoFilter() {
        when(bookMapper.search(anyString(), anyInt(), anyInt())).thenReturn(List.<Book>of());
        when(bookMapper.countByKeyword(anyString())).thenReturn(12);

        bookService.searchBooks(null, null, 1, 5);
        bookService.searchBooks("   ", null, 1, 5);

        // 空关键词 trim 后传空串，由 SQL <if> 跳过过滤（全库分页）
        verify(bookMapper, org.mockito.Mockito.times(2)).search("", 5, 0);
        verify(bookMapper, org.mockito.Mockito.times(2)).countByKeyword("");
    }

    @Test
    void sizeCappedAt100AndPageClampedTo1() {
        when(bookMapper.search(anyString(), anyInt(), anyInt())).thenReturn(List.<Book>of());
        when(bookMapper.countByKeyword(anyString())).thenReturn(0);

        BookPageResponse oversize = bookService.searchBooks("", null, 1, 500);
        assertEquals(100, oversize.getSize(), "size 上限 100");

        BookPageResponse zeroPage = bookService.searchBooks("", null, 0, 5);
        assertEquals(1, zeroPage.getPage(), "page 下限 1");

        verify(bookMapper).search("", 100, 0);
        verify(bookMapper).search("", 5, 0);
    }
}
