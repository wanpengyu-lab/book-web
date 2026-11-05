package org.example.bookweb.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.bookweb.dto.NotificationPageResponse;
import org.example.bookweb.dto.Result;
import org.example.bookweb.service.ReservationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内通知接口（v0.6，登录用户）：GET /api/notifications，未读优先，分页。
 * 归属于预约域（3.2 service 清单无 NotificationService，由 ReservationService 承载）。
 */
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final ReservationService reservationService;

    @GetMapping("/api/notifications")
    public Result<NotificationPageResponse> notifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size,
            HttpServletRequest request) {
        int userId = (Integer) request.getAttribute("userId");
        return Result.success(reservationService.listNotifications(userId, page, size));
    }
}
