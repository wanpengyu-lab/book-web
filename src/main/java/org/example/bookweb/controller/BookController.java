package org.example.bookweb.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.bookweb.dto.BookRequest;
import org.example.bookweb.dto.Result;
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

    @GetMapping("/books/page")
    public Result<Map<String, Object>> pageBooks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return Result.success(bookService.getBooksPage(page, size));
    }

    @PostMapping("/books")
    public Result<Boolean> addBook(@Valid @RequestBody BookRequest req) {
        return Result.success(bookService.addBook(req.getId(), req.getTitle(), req.getAuthor()));
    }

    @PutMapping("/books/{id}")
    public Result<Boolean> updateBook(@PathVariable int id, @Valid @RequestBody BookRequest req) {
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
