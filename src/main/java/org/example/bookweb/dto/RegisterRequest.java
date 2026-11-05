package org.example.bookweb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 注册请求 DTO（v0.3）：注册即 reader，用户名重复 40901 */
@Data
public class RegisterRequest {

    @NotBlank(message = "不能为空")
    @Size(max = 50, message = "长度不能超过 50")
    private String username;

    @NotBlank(message = "不能为空")
    @Size(max = 100, message = "长度不能超过 100")
    private String password;
}
