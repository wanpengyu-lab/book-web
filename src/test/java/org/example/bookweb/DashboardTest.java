package org.example.bookweb;

import org.example.bookweb.dto.DashboardResponse;
import org.example.bookweb.mapper.BookMapper;
import org.example.bookweb.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * v0.5 看板测试（PRD v0.5 验收）：
 * - 空库（三组 SQL 均无行）→ 三个空数组，不报错；
 * - 样例数据三组与手工 SQL 核对一致：mock 返回值即「手工执行看板 SQL 的期望结果」，
 *   服务层必须原样组装（日期/名称/计数/排序不得被改动或丢失）。
 * 三组 SQL 本体（trend / categoryRatio / topBorrowed）在验收时以真实库手工核对。
 */
class DashboardTest {

    private BookMapper bookMapper;
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        bookMapper = mock(BookMapper.class);
        dashboardService = new DashboardService(bookMapper);
    }

    @Test
    void emptyDatabaseReturnsEmptyArrays() {
        when(bookMapper.trend()).thenReturn(List.of());
        when(bookMapper.categoryRatio()).thenReturn(List.of());
        when(bookMapper.topBorrowed()).thenReturn(List.of());

        DashboardResponse d = dashboardService.getDashboard();

        assertNotNull(d.getTrend(), "trend 必须是空数组而非 null");
        assertNotNull(d.getCategoryRatio(), "categoryRatio 必须是空数组而非 null");
        assertNotNull(d.getTopBorrowed(), "topBorrowed 必须是空数组而非 null");
        assertTrue(d.getTrend().isEmpty());
        assertTrue(d.getCategoryRatio().isEmpty());
        assertTrue(d.getTopBorrowed().isEmpty());
    }

    @Test
    void sampleDataAssembledAsManualSqlResult() {
        // 手工 SQL 期望结果（样例）：trend 两日、categoryRatio 两类（value 降序）、topBorrowed 两条（count 降序）
        DashboardResponse.TrendPoint t1 = new DashboardResponse.TrendPoint();
        t1.setDate("2026-11-05");
        t1.setCount(3);
        DashboardResponse.TrendPoint t2 = new DashboardResponse.TrendPoint();
        t2.setDate("2026-11-06");
        t2.setCount(2);

        DashboardResponse.CategoryRatio c1 = new DashboardResponse.CategoryRatio();
        c1.setName("科幻");
        c1.setValue(5);
        DashboardResponse.CategoryRatio c2 = new DashboardResponse.CategoryRatio();
        c2.setName("未分类");
        c2.setValue(7);

        DashboardResponse.TopBorrowed b1 = new DashboardResponse.TopBorrowed();
        b1.setId(1);
        b1.setTitle("三体");
        b1.setAuthor("刘慈欣");
        b1.setCount(4);
        DashboardResponse.TopBorrowed b2 = new DashboardResponse.TopBorrowed();
        b2.setId(2);
        b2.setTitle("围城");
        b2.setAuthor("钱钟书");
        b2.setCount(1);

        when(bookMapper.trend()).thenReturn(List.of(t1, t2));
        when(bookMapper.categoryRatio()).thenReturn(List.of(c1, c2));
        when(bookMapper.topBorrowed()).thenReturn(List.of(b1, b2));

        DashboardResponse d = dashboardService.getDashboard();

        assertEquals(2, d.getTrend().size());
        assertEquals("2026-11-05", d.getTrend().get(0).getDate());
        assertEquals(3, d.getTrend().get(0).getCount());
        assertEquals("2026-11-06", d.getTrend().get(1).getDate());
        assertEquals(2, d.getTrend().get(1).getCount());

        assertEquals(2, d.getCategoryRatio().size());
        assertEquals("科幻", d.getCategoryRatio().get(0).getName());
        assertEquals(5, d.getCategoryRatio().get(0).getValue());
        assertEquals("未分类", d.getCategoryRatio().get(1).getName());
        assertEquals(7, d.getCategoryRatio().get(1).getValue());

        assertEquals(2, d.getTopBorrowed().size());
        assertEquals(1, d.getTopBorrowed().get(0).getId());
        assertEquals("三体", d.getTopBorrowed().get(0).getTitle());
        assertEquals("刘慈欣", d.getTopBorrowed().get(0).getAuthor());
        assertEquals(4, d.getTopBorrowed().get(0).getCount());
        assertEquals(2, d.getTopBorrowed().get(1).getId());
        assertEquals(1, d.getTopBorrowed().get(1).getCount());
    }
}
