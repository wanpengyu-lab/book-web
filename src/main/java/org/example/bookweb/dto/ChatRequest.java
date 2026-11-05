package org.example.bookweb.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** AI 馆藏助手请求 DTO（v0.7）：POST /api/assistant/chat {message} */
@Data
public class ChatRequest {

    @NotBlank(message = "不能为空")
    private String message;
}
