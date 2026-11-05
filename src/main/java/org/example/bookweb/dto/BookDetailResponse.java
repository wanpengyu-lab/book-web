package org.example.bookweb.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.bookweb.entity.Book;
import org.example.bookweb.entity.BorrowRecord;

import java.util.List;

/**
 * /api/books/{id} 聚合详情响应（v0.5）：{...全量字段, borrowed, recentRecords, relatedIds}。
 * 继承 Book 平铺全量字段；一次请求取齐（PRD v0.5 强制规范），禁止前端拼多个请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BookDetailResponse extends Book {

    /** 最近 5 条借阅记录（新→旧） */
    private List<BorrowRecord> recentRecords;

    /** v0.6 接入共借：本版固定空数组占位 */
    private List<Integer> relatedIds;
}
