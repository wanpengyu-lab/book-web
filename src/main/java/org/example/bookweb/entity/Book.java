package org.example.bookweb.entity;

import lombok.Data;

/**
 * 图书实体，对应表 book(id, title, author, is_borrowed)。
 * v0.5 扩展：category / publisher / isbn / cover_url / description / total_copies（V0_5__book_fields.sql）。
 * total_copies 仅存储展示，禁止副本级借用逻辑（PRD v0.5 禁止项）。
 */
@Data
public class Book {
    private int id;
    private String title;
    private String author;
    private boolean borrowed;
    private String category;
    private String publisher;
    private String isbn;
    private String coverUrl;
    private String description;
    private int totalCopies;
}
