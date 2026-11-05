package org.example.bookweb.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.bookweb.dto.AddBookRequest;
import org.example.bookweb.dto.BookPageResponse;
import org.example.bookweb.dto.Result;
import org.example.bookweb.dto.UpdateBookRequest;
import org.example.bookweb.entity.Book;
import org.example.bookweb.service.BookService;
import org.example.bookweb.service.BorrowService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final BorrowService borrowService;

    @GetMapping("/books")
    public Result<List<Book>> listBooks() {
        return Result.success(bookService.getAllBooks());
    }

    @GetMapping("/books/{id}")
    public Result<Book> getBook(@PathVariable int id) {
        return Result.success(bookService.getBookById(id));
    }

    /**
     * v0.2 统一列表（修 G3）：keyword 匹配 title/author（LIKE 两端 %），服务端完成搜索+分页；
     * 翻页时前端携带同一 keyword 即可保持搜索条件。开放接口（v0.3 起在拦截器放行表内）。
     */
    @GetMapping("/api/books")
    public Result<BookPageResponse> apiListBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return Result.success(bookService.searchBooks(keyword, page, size));
    }

    /**
     * v0.2 起不再接收 id（修 G5）：主键由数据库 AUTO_INCREMENT 生成（V0_2__book_auto_increment.sql）。
     * v0.3 起 POST/PUT/DELETE /api/books/** 由 JwtInterceptor 强制 admin（PRD v0.3 拦截规则）。
     */
    @PostMapping("/api/books")
    public Result<Boolean> apiAddBook(@Valid @RequestBody AddBookRequest req) {
        return Result.success(bookService.addBook(req.getTitle(), req.getAuthor()));
    }

    /**
     * v0.3：详情接口。GET 在 PRD 放行表内（「GET /api/books（含详情）」），
     * 行为暂与旧端点一致，v0.5 起改为聚合响应。
     */
    @GetMapping("/api/books/{id}")
    public Result<Book> apiGetBook(@PathVariable int id) {
        return Result.success(bookService.getBookById(id));
    }

    /** v0.3：编辑接口（admin），行为与旧端点一致，v0.5 扩展新字段 */
    @PutMapping("/api/books/{id}")
    public Result<Boolean> apiUpdateBook(@PathVariable int id, @Valid @RequestBody UpdateBookRequest req) {
        return Result.success(bookService.updateBook(id, req.getTitle(), req.getAuthor()));
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

    /** 旧端点：已被 /api/books 取代，保留至 v0.10 统一删除（PRD 2.1） */
    @Deprecated
    @GetMapping("/books/page")
    public Result<Map<String, Object>> pageBooks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return Result.success(bookService.getBooksPage(page, size));
    }

    /** 旧端点：保留至 v0.10 统一删除（PRD 2.1），行为随 v0.2 自增改造不再使用 id */
    @PostMapping("/books")
    public Result<Boolean> addBook(@Valid @RequestBody AddBookRequest req) {
        return Result.success(bookService.addBook(req.getTitle(), req.getAuthor()));
    }

    @PutMapping("/books/{id}")
    public Result<Boolean> updateBook(@PathVariable int id, @Valid @RequestBody UpdateBookRequest req) {
        return Result.success(bookService.updateBook(id, req.getTitle(), req.getAuthor()));
    }

    @DeleteMapping("/books/{id}")
    public Result<Boolean> deleteBook(@PathVariable int id) {
        return Result.success(bookService.deleteBook(id));
    }

    /** 旧端点：已被 PUT /api/books/{id}/borrow（登录用户 + 借阅记录）取代，保留至 v0.10 统一删除（PRD v0.4） */
    @Deprecated
    @PutMapping("/books/{id}/borrowed")
    public Result<Boolean> borrowedBook(@PathVariable int id) {
        return Result.success(bookService.borrowBook(id));
    }

    /** 旧端点：已被 PUT /api/books/{id}/return（登录用户 + 借阅记录）取代，保留至 v0.10 统一删除（PRD v0.4） */
    @Deprecated
    @PutMapping("/books/{id}/return")
    public Result<Boolean> returnBook(@PathVariable int id) {
        return Result.success(bookService.returnBook(id));
    }
}
