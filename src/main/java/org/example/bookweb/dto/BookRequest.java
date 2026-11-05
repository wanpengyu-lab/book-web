package org.example.bookweb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 新增 / 更新图书的请求 DTO（v0.1 起替代裸 Map 入参）。
 * 注：id 在 v0.1 仍由请求显式提供（主键自增在 v0.2 落地后移除该字段）。
 */
@Data
public class BookRequest {

    @NotNull(message = "不能为空")
    @Positive(message = "必须为正整数")
    private Integer id;

    @NotBlank(message = "不能为空")
    private String title;

    @NotBlank(message = "不能为空")
    private String author;
}
