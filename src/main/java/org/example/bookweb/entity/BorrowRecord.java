package org.example.bookweb.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借阅记录实体，对应表 borrow_record(id, book_id, user_id, borrowed_at, due_at, returned_at, status)。
 * 逾期为派生态：status='active' AND due_at < NOW()，不落库、不加字段（PRD 2.3）。
 * 记录只改状态，禁止物理删除。
 * v0.7（挂账项①，v0.4 拍板）：查询响应附带 bookTitle/bookAuthor（JOIN book 一次取齐，仅展示，不落库）。
 */
@Data
public class BorrowRecord {
    private int id;
    private int bookId;
    private int userId;
    private LocalDateTime borrowedAt;
    private LocalDateTime dueAt;
    private LocalDateTime returnedAt;
    private String status;
    /** 关联展示字段（JOIN book），不对应本表列 */
    private String bookTitle;
    private String bookAuthor;
}
