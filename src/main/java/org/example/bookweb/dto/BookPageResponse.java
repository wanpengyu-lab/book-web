package org.example.bookweb.dto;

import lombok.Data;
import org.example.bookweb.entity.Book;

import java.util.List;

/**
 * /api/books 统一列表响应（v0.2）：data: {total, page, size, list}。
 * total 为命中 keyword 的全量条数（服务端口径），list 为当前页数据。
 */
@Data
public class BookPageResponse {

    private int total;
    private int page;
    private int size;
    private List<Book> list;

    public static BookPageResponse of(int total, int page, int size, List<Book> list) {
        BookPageResponse r = new BookPageResponse();
        r.total = total;
        r.page = page;
        r.size = size;
        r.list = list;
        return r;
    }
}
