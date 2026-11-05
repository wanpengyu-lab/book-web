package org.example.bookweb.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新图书请求 DTO。
 * id 只从 URL 路径读取（PUT /books/{id}），请求体不接收、不校验 id。
 */
@Data
public class UpdateBookRequest {

    @NotBlank(message = "不能为空")
    private String title;

    @NotBlank(message = "不能为空")
    private String author;
}
