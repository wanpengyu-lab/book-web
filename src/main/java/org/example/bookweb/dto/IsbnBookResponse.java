package org.example.bookweb.dto;

import lombok.Data;

/**
 * ISBN 查询回填响应（v0.7）：{title, author, publisher, coverUrl, description}。
 * 由外部书源（OpenLibrary → Google Books）映射而来，仅供建档预填，不直接落库。
 */
@Data
public class IsbnBookResponse {

    private String title;
    private String author;
    private String publisher;
    private String coverUrl;
    private String description;
}
