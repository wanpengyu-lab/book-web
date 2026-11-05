package org.example.bookweb.controller;

import lombok.RequiredArgsConstructor;
import org.example.bookweb.dto.Result;
import org.example.bookweb.dto.StatsResponse;
import org.example.bookweb.service.BookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计接口（v0.2）：/api/stats 为开放接口（v0.3 起在 JwtInterceptor 放行表内）。
 */
@RestController
@RequiredArgsConstructor
public class StatsController {

    private final BookService bookService;

    /** 全库统计：三数字来自 SQL 聚合，与分页/当前页内容解耦（修 G2） */
    @GetMapping("/api/stats")
    public Result<StatsResponse> stats() {
        return Result.success(bookService.getStats());
    }
}
