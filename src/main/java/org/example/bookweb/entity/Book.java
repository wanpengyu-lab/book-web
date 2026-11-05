package org.example.bookweb.entity;

import lombok.Data;

/**
 * 图书实体，对应表 book(id, title, author, is_borrowed)。
 */
@Data
public class Book {
    private int id;
    private String title;
    private String author;
    private boolean borrowed;
}
