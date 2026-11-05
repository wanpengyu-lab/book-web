package org.example.bookweb.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.bookweb.dto.LoginRequest;
import org.example.bookweb.dto.LoginResponse;
import org.example.bookweb.dto.RegisterRequest;
import org.example.bookweb.dto.Result;
import org.example.bookweb.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 认证接口（v0.3）：/api/auth/** 在 JwtInterceptor 放行表内 */
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 注册即 reader；用户名重复 → 40901 */
    @PostMapping("/api/auth/register")
    public Result<Boolean> register(@Valid @RequestBody RegisterRequest req) {
        return Result.success(authService.register(req.getUsername(), req.getPassword()));
    }

    /** 登录成功 → data: {token, user:{id, username, role}}；凭据错误 → 40101 */
    @PostMapping("/api/auth/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.success(authService.login(req.getUsername(), req.getPassword()));
    }
}
