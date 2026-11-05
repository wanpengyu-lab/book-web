package org.example.bookweb.controller;

import lombok.RequiredArgsConstructor;
import org.example.bookweb.dto.DashboardResponse;
import org.example.bookweb.dto.Result;
import org.example.bookweb.dto.StatsResponse;
import org.example.bookweb.service.BookService;
import org.example.bookweb.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计接口（v0.2）：/api/stats 为开放接口（v0.3 起在 JwtInterceptor 放行表内）。
 * v0.5：/api/dashboard 看板聚合数据也挂在本控制器（3.2 控制器清单未新增 DashboardController；
 * DashboardService 为 PRD 3.2 service 清单内组件）。dashboard 需登录（不在放行表内）。
 */
@RestController
@RequiredArgsConstructor
public class StatsController {

    private final BookService bookService;
    private final DashboardService dashboardService;

    /** 全库统计：三数字来自 SQL 聚合，与分页/当前页内容解耦（修 G2） */
    @GetMapping("/api/stats")
    public Result<StatsResponse> stats() {
        return Result.success(bookService.getStats());
    }

    /** 看板（v0.5）：trend / categoryRatio / topBorrowed 三组均来自 SQL 聚合；空库返回空数组 */
    @GetMapping("/api/dashboard")
    public Result<DashboardResponse> dashboard() {
        return Result.success(dashboardService.getDashboard());
    }
}
