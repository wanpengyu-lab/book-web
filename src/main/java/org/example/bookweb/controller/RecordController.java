package org.example.bookweb.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.bookweb.dto.RecordPageResponse;
import org.example.bookweb.dto.Result;
import org.example.bookweb.service.BorrowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 借阅记录接口（v0.4，登录用户）：
 * reader 只看本人（查他人 403）；admin 可加 userId= 查指定用户，不传查全部；
 * status = active / returned / overdue（overdue 查询派生）。
 */
@RestController
@RequiredArgsConstructor
public class RecordController {

    private final BorrowService borrowService;

    @GetMapping("/api/records")
    public Result<RecordPageResponse> records(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size,
            HttpServletRequest request) {
        int selfId = (Integer) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");

        Integer filterUserId;
        if ("admin".equals(role)) {
            filterUserId = userId;
        } else {
            if (userId != null && userId != selfId) {
                throw new BorrowService.RecordAccessDeniedException("无权限查看他人借阅记录");
            }
            filterUserId = selfId;
        }
        return Result.success(borrowService.listRecords(filterUserId, status, page, size));
    }
}
