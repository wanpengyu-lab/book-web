package org.example.bookweb.service;

import lombok.RequiredArgsConstructor;
import org.example.bookweb.dto.AddBookRequest;
import org.example.bookweb.dto.BookDetailResponse;
import org.example.bookweb.dto.BookPageResponse;
import org.example.bookweb.dto.StatsResponse;
import org.example.bookweb.dto.UpdateBookRequest;
import org.example.bookweb.entity.Book;
import org.example.bookweb.mapper.BookMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookService {

    /** 旧数据/缺省分类（PRD v0.5：旧数据 category 显示「未分类」） */
    public static final String DEFAULT_CATEGORY = "未分类";

    private final BookMapper bookMapper;

    public List<Book> getAllBooks() {
        return bookMapper.findAll();
    }

    public Book getBookById(int id) {
        return bookMapper.findById(id);
    }

    /**
     * v0.5：新增携带扩展字段。category 缺省归一化为「未分类」，total_copies 缺省 1，
     * 保证 NOT NULL 列不因请求缺字段而 500。
     */
    public boolean addBook(AddBookRequest req) {
        return bookMapper.add(req.getTitle(), req.getAuthor(),
                normalizeCategory(req.getCategory()), req.getPublisher(), req.getIsbn(),
                req.getCoverUrl(), req.getDescription(), normalizeCopies(req.getTotalCopies()));
    }

    public boolean updateBook(int id, UpdateBookRequest req) {
        // id 只来自 Controller 传出的路径变量，请求体不参与
        return bookMapper.update(id, req.getTitle(), req.getAuthor(),
                normalizeCategory(req.getCategory()), req.getPublisher(), req.getIsbn(),
                req.getCoverUrl(), req.getDescription(), normalizeCopies(req.getTotalCopies()));
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
     * v0.2 服务端搜索（修 G3）+ v0.5 分类精确过滤：keyword 匹配 title/author（LIKE 两端 %），
     * category 精确匹配；两者可组合。category 非空走 findByCategory 口径，否则走 keyword 口径；
     * total 与 list 同口径，翻页携带同一组条件即保持筛选。size 上限 100。
     */
    public BookPageResponse searchBooks(String keyword, String category, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 5;
        if (size > 100) size = 100;
        String kw = keyword == null ? "" : keyword.trim();
        String cat = category == null ? "" : category.trim();
        int offset = (page - 1) * size;
        if (!cat.isEmpty()) {
            List<Book> list = bookMapper.findByCategory(cat, kw, size, offset);
            int total = bookMapper.countByCategory(cat, kw);
            return BookPageResponse.of(total, page, size, list);
        }
        List<Book> list = bookMapper.search(kw, size, offset);
        int total = bookMapper.countByKeyword(kw);
        return BookPageResponse.of(total, page, size, list);
    }

    /**
     * v0.5 详情聚合：全量字段 + recentRecords（最近 5 条，新→旧）+ relatedIds 一次请求取齐（PRD v0.5 强制规范）。
     */
    public BookDetailResponse getBookDetail(int id) {
        Book book = bookMapper.findById(id);
        if (book == null) {
            // 与 v0.3 详情行为一致：书不存在返回 data:null，不引入新错误码
            return null;
        }
        BookDetailResponse detail = new BookDetailResponse();
        BeanUtils.copyProperties(book, detail);
        detail.setRecentRecords(bookMapper.recentRecords(id, 5));
        // v0.6 接入共借：本版固定空数组占位（PRD v0.5）
        detail.setRelatedIds(List.of());
        return detail;
    }

    /**
     * v0.2 全库统计（修 G2）：三数字全部来自 SQL 聚合，与任何分页/当前页内容无关。
     * v0.5：categories 切换真实分类统计（GROUP BY category，按 count 降序）。
     */
    public StatsResponse getStats() {
        int total = bookMapper.countAll();
        StatsResponse stats = new StatsResponse();
        stats.setTotal(total);
        stats.setAvailable(bookMapper.countByBorrowed(false));
        stats.setBorrowed(bookMapper.countByBorrowed(true));
        stats.setCategories(bookMapper.categoryCounts());
        return stats;
    }

    private String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return DEFAULT_CATEGORY;
        }
        return category.trim();
    }

    private int normalizeCopies(Integer totalCopies) {
        return totalCopies == null ? 1 : totalCopies;
    }
}
