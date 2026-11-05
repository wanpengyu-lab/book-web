package org.example.bookweb.controller;

import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.example.bookweb.dto.IsbnBookResponse;
import org.example.bookweb.dto.Result;
import org.example.bookweb.service.IsbnService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * ISBN 查询接口（v0.7，admin）：GET /api/isbn/{isbn}。
 * 路径不在 v0.3 放行表内：登录后由 JwtInterceptor 强制 admin；
 * 非数字/超长 ISBN → 400 + 40001（PRD v0.7 校验规则）。
 */
@RestController
@RequiredArgsConstructor
@Validated
public class IsbnController {

    private final IsbnService isbnService;

    @GetMapping("/api/isbn/{isbn}")
    public Result<IsbnBookResponse> lookup(
            @PathVariable
            @Pattern(regexp = "\\d{1,20}", message = "ISBN 必须为数字且不超过 20 位") String isbn) {
        IsbnBookResponse book = isbnService.lookup(isbn);
        if (book == null) {
            // 干净未命中：200 + code 0 + data:null + 明确 message（PRD v0.7 约定）
            return Result.notice("未查到该书目，请手动录入");
        }
        return Result.success(book);
    }
}
