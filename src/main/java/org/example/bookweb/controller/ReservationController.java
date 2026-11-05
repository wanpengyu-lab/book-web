package org.example.bookweb.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.bookweb.dto.Result;
import org.example.bookweb.service.ReservationService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 预约接口（v0.6，登录用户）：POST /api/books/{id}/reserve。
 * 拦截器中与 borrow/return 同类豁免 admin 检查（仅登录即可）。
 */
@RestController
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /** 预约：在借状态才可约；重复预约 40901；在馆书 40001（PRD v0.6） */
    @PostMapping("/api/books/{id}/reserve")
    public Result<Boolean> reserve(@PathVariable int id, HttpServletRequest request) {
        return Result.success(reservationService.reserve(id, (Integer) request.getAttribute("userId")));
    }
}
