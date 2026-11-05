package org.example.bookweb.dto;

import lombok.Data;

import java.util.List;

/**
 * /api/dashboard 看板响应（v0.5）：data: {trend, categoryRatio, topBorrowed}。
 * 三组数字只能来自 SQL 聚合；空库返回空数组不报错。
 */
@Data
public class DashboardResponse {

    /** 近 30 天每日借出量（只含有借出的日期） */
    private List<TrendPoint> trend;
    /** 分类占比：{name, value}，value 为该分类藏书量 */
    private List<CategoryRatio> categoryRatio;
    /** 借阅次数前 10：{id, title, author, count} */
    private List<TopBorrowed> topBorrowed;

    @Data
    public static class TrendPoint {

        private String date;
        private int count;
    }

    @Data
    public static class CategoryRatio {

        private String name;
        private int value;
    }

    @Data
    public static class TopBorrowed {

        private int id;
        private String title;
        private String author;
        private int count;
    }
}
