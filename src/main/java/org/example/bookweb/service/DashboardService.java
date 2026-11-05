package org.example.bookweb.service;

import lombok.RequiredArgsConstructor;
import org.example.bookweb.dto.DashboardResponse;
import org.example.bookweb.mapper.BookMapper;
import org.springframework.stereotype.Service;

/**
 * 看板服务（v0.5）：三组数据全部来自 SQL 聚合（BookMapper 注解 SQL），服务层只做组装；
 * 空库时各数组为空，不报错。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final BookMapper bookMapper;

    public DashboardResponse getDashboard() {
        DashboardResponse dashboard = new DashboardResponse();
        dashboard.setTrend(bookMapper.trend());
        dashboard.setCategoryRatio(bookMapper.categoryRatio());
        dashboard.setTopBorrowed(bookMapper.topBorrowed());
        return dashboard;
    }
}
