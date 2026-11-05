package org.example.bookweb.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新图书请求 DTO。
 * id 只从 URL 路径读取（PUT /books/{id}），请求体不接收、不校验 id。
 * v0.5 扩展字段（PRD @Valid 约束：category ≤50、isbn ≤20、cover_url ≤500）；
 * category 缺省由服务层归一化为「未分类」，total_copies 缺省为 1。
 */
@Data
public class UpdateBookRequest {

    @NotBlank(message = "不能为空")
    private String title;

    @NotBlank(message = "不能为空")
    private String author;

    @Size(max = 50, message = "最长 50 字符")
    private String category;

    @Size(max = 100, message = "最长 100 字符")
    private String publisher;

    @Size(max = 20, message = "最长 20 字符")
    private String isbn;

    @Size(max = 500, message = "最长 500 字符")
    private String coverUrl;

    private String description;

    @Min(value = 1, message = "至少为 1")
    private Integer totalCopies;
}
