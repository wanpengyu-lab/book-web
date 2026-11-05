package org.example.bookweb.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增图书请求 DTO。
 * id 不在请求体内：主键由数据库 AUTO_INCREMENT 生成（V0_2__book_auto_increment.sql）。
 */
@Data
public class AddBookRequest {

    @NotBlank(message = "不能为空")
    private String title;

    @NotBlank(message = "不能为空")
    private String author;
}
