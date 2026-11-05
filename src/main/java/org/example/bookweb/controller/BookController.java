package org.example.bookweb.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.bookweb.dto.AddBookRequest;
import org.example.bookweb.dto.BookDetailResponse;
import org.example.bookweb.dto.BookPageResponse;
import org.example.bookweb.dto.Result;
import org.example.bookweb.dto.UpdateBookRequest;
import org.example.bookweb.entity.Book;
import org.example.bookweb.service.BookService;
import org.example.bookweb.service.BorrowService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 图书接口（v0.10）：仅保留 /api 前缀端点；旧无前缀端点（/books、/books/page、
 * /books/{id}、/books/{id}/borrowed、/books/{id}/return）已按 PRD v0.10 删除。
 */
@RestController
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final BorrowService borrowService;

    /**
     * v0.2 统一列表（修 G3）：keyword 匹配 title/author（LIKE 两端 %），服务端完成搜索+分页；
     * v0.5：+ category 精确过滤，可与 keyword 组合；翻页时前端携带同一组条件即可保持筛选。
     * 开放接口（v0.3 起在拦截器放行表内）。
     */
    @GetMapping("/api/books")
    public Result<BookPageResponse> apiListBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return Result.success(bookService.searchBooks(keyword, category, page, size));
    }

    /**
     * v0.2 起不再接收 id（修 G5）：主键由数据库 AUTO_INCREMENT 生成（V0_2__book_auto_increment.sql）。
     * v0.3 起 POST/PUT/DELETE /api/books/** 由 JwtInterceptor 强制 admin（PRD v0.3 拦截规则）。
     * v0.5：接收扩展字段（category/publisher/isbn/coverUrl/description/totalCopies，@Valid 校验）。
     */
    @PostMapping("/api/books")
    public Result<Boolean> apiAddBook(@Valid @RequestBody AddBookRequest req) {
        return Result.success(bookService.addBook(req));
    }

    /**
     * v0.5：聚合详情。GET 在 PRD 放行表内（「GET /api/books（含详情）」）；
     * 全量字段 + recentRecords(≤5) + relatedIds 一次请求取齐（PRD v0.5 强制规范）；
     * v0.6：relatedIds 为真实共借推荐。
     */
    @GetMapping("/api/books/{id}")
    public Result<BookDetailResponse> apiGetBook(@PathVariable int id) {
        return Result.success(bookService.getBookDetail(id));
    }

    /**
     * v0.6：共借推荐（前 6，按共借次数降序）；无共借数据退化为同分类最近入库前 6。
     * 不在 v0.3 放行表内 → 拦截器按「其余 /api/** 需登录」处理。
     */
    @GetMapping("/api/books/{id}/related")
    public Result<List<Integer>> apiRelatedBooks(@PathVariable int id) {
        return Result.success(bookService.getRelatedBookIds(id));
    }

    /** v0.5：编辑接口（admin）携带扩展字段（category/publisher/isbn/coverUrl/description/totalCopies） */
    @PutMapping("/api/books/{id}")
    public Result<Boolean> apiUpdateBook(@PathVariable int id, @Valid @RequestBody UpdateBookRequest req) {
        return Result.success(bookService.updateBook(id, req));
    }

    /** v0.3：删除接口（admin），行为与旧端点一致 */
    @DeleteMapping("/api/books/{id}")
    public Result<Boolean> apiDeleteBook(@PathVariable int id) {
        return Result.success(bookService.deleteBook(id));
    }

    /** v0.4：借出（登录用户；拦截器放行，BorrowService 条件更新并发安全） */
    @PutMapping("/api/books/{id}/borrow")
    public Result<Boolean> apiBorrow(@PathVariable int id, HttpServletRequest request) {
        return Result.success(borrowService.borrow(id, (Integer) request.getAttribute("userId")));
    }

    /** v0.4：归还（登录用户；同事务回写借阅记录与在借状态） */
    @PutMapping("/api/books/{id}/return")
    public Result<Boolean> apiReturn(@PathVariable int id, HttpServletRequest request) {
        return Result.success(borrowService.returnBook(id, (Integer) request.getAttribute("userId")));
    }
}
