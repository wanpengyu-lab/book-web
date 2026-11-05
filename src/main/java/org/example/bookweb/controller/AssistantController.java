package org.example.bookweb.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.bookweb.dto.ChatRequest;
import org.example.bookweb.dto.Result;
import org.example.bookweb.service.AssistantService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 馆藏助手接口（v0.7，登录用户）：POST /api/assistant/chat {message} → data:{reply}。
 * 未配置 GLM_API_KEY → 200 + code 0 + data:null + message「AI 服务未配置」（全局异常处理映射）。
 */
@RestController
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/api/assistant/chat")
    public Result<Map<String, String>> chat(@Valid @RequestBody ChatRequest req) {
        String reply = assistantService.chat(req.getMessage());
        return Result.success(Map.of("reply", reply));
    }
}
