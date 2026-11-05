package org.example.bookweb.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借阅记录实体，对应表 borrow_record(id, book_id, user_id, borrowed_at, due_at, returned_at, status)。
 * 逾期为派生态：status='active' AND due_at < NOW()，不落库、不加字段（PRD 2.3）。
 * 记录只改状态，禁止物理删除。
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
}
