package org.example.bookweb.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.bookweb.dto.AddBookRequest;
import org.example.bookweb.dto.BookPageResponse;
import org.example.bookweb.dto.Result;
import org.example.bookweb.dto.UpdateBookRequest;
import org.example.bookweb.entity.Book;
import org.example.bookweb.service.BookService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

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
     * 过渡态：本版暂无鉴权，v0.3 起加锁（POST/PUT/DELETE /api/books/** 需 admin）。
     */
    @PostMapping("/api/books")
    public Result<Boolean> apiAddBook(@Valid @RequestBody AddBookRequest req) {
        return Result.success(bookService.addBook(req.getTitle(), req.getAuthor()));
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

    @PutMapping("/books/{id}/borrowed")
    public Result<Boolean> borrowedBook(@PathVariable int id) {
        return Result.success(bookService.borrowBook(id));
    }

    @PutMapping("/books/{id}/return")
    public Result<Boolean> returnBook(@PathVariable int id) {
        return Result.success(bookService.returnBook(id));
    }
}
