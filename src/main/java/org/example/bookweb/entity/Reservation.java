package org.example.bookweb.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约实体，对应表 reservation(id, book_id, user_id, status, created_at)。
 * 状态机（PRD 2.3）：waiting → notified → fulfilled，取消为 cancelled；
 * 还书事务内队首 waiting → notified；借出人在借出时其预约置 fulfilled。
 */
@Data
public class Reservation {
    private int id;
    private int bookId;
    private int userId;
    private String status;
    private LocalDateTime createdAt;
}
