package org.example.bookweb.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知实体，对应表 notification(id, user_id, content, is_read, created_at)。
 * 只做站内通知（PRD v0.6 禁止项：邮件/短信/MQ）；来源：还书事务内预约到货提醒 + OverdueScanJob 到期提醒。
 */
@Data
public class Notification {
    private int id;
    private int userId;
    private String content;
    private boolean read;
    private LocalDateTime createdAt;
}
