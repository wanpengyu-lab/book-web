package org.example.bookweb.service;

import lombok.RequiredArgsConstructor;
import org.example.bookweb.entity.Book;
import org.example.bookweb.mapper.BookMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookMapper bookMapper;

    public List<Book> getAllBooks() {
        return bookMapper.findAll();
    }

    public Book getBookById(int id) {
        return bookMapper.findById(id);
    }

    public boolean addBook(int id, String title, String author) {
        // 业务规则的家：比如以后加"编号不能重复先查重""书名不能为空"，写在这
        return bookMapper.add(id, title, author);
    }

    public boolean updateBook(int id, String title, String author) {
        return bookMapper.update(id, title, author);
    }

    public boolean deleteBook(int id) {
        return bookMapper.delete(id);
    }

    public boolean borrowBook(int id) {
        return bookMapper.setBorrowed(true, id);
    }

    public boolean returnBook(int id) {
        return bookMapper.setBorrowed(false, id);
    }

    public Map<String, Object> getBooksPage(int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 5;
        if (size > 100) size = 100;
        int offset = (page - 1) * size;
        List<Book> list = bookMapper.findPage(size, offset);
        int total = bookMapper.countAll();

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("list", list);
        return result;
    }
}
