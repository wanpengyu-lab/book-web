package org.example.bookweb.service;

import lombok.RequiredArgsConstructor;
import org.example.bookweb.dto.BookPageResponse;
import org.example.bookweb.dto.StatsResponse;
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

    public boolean addBook(String title, String author) {
        // 业务规则的家：比如以后加"书名不能重复先查重"，写在这
        // id 由数据库 AUTO_INCREMENT 生成，服务层不再接收 id
        return bookMapper.add(title, author);
    }

    public boolean updateBook(int id, String title, String author) {
        // id 只来自 Controller 传出的路径变量，请求体不参与
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

    /**
     * v0.2 服务端搜索（修 G3）：keyword 匹配 title/author（LIKE 两端 %），空/空白 keyword 不过滤；
     * total 与 list 同口径（countByKeyword），翻页携带同一 keyword 即保持搜索条件。size 上限 100。
     */
    public BookPageResponse searchBooks(String keyword, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 5;
        if (size > 100) size = 100;
        String kw = keyword == null ? "" : keyword.trim();
        int offset = (page - 1) * size;
        List<Book> list = bookMapper.search(kw, size, offset);
        int total = bookMapper.countByKeyword(kw);
        return BookPageResponse.of(total, page, size, list);
    }

    /**
     * v0.2 全库统计（修 G2）：三数字全部来自 SQL 聚合，与任何分页/当前页内容无关。
     */
    public StatsResponse getStats() {
        int total = bookMapper.countAll();
        StatsResponse stats = new StatsResponse();
        stats.setTotal(total);
        stats.setAvailable(bookMapper.countByBorrowed(false));
        stats.setBorrowed(bookMapper.countByBorrowed(true));
        // v0.5 切换真实分类：book.category 字段落地后改为 GROUP BY category 的降序统计
        stats.setCategories(List.of(StatsResponse.CategoryCount.of("全部", total)));
        return stats;
    }
}
