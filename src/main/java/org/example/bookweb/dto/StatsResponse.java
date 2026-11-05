package org.example.bookweb.dto;

import lombok.Data;

import java.util.List;

/**
 * /api/stats 全库统计响应（v0.2）：data: {total, available, borrowed, categories}。
 * 三个数字只能来自 SQL 聚合（PRD v0.2 强制规范），禁止前端按当前页累加。
 */
@Data
public class StatsResponse {

    private int total;
    private int available;
    private int borrowed;
    /** 按 count 降序；v0.5 起为 GROUP BY category 的真实分类统计 */
    private List<CategoryCount> categories;

    @Data
    public static class CategoryCount {

        private String name;
        private int count;

        public static CategoryCount of(String name, int count) {
            CategoryCount c = new CategoryCount();
            c.name = name;
            c.count = count;
            return c;
        }
    }
}
