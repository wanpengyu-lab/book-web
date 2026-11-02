package org.example.bookweb;


public class Book {
    private int id;
    private String title;
    private String author;
    private boolean borrowed;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }


    public boolean isBorrowed() {
        return borrowed;
    }

    public void setBorrowed(boolean borrowed) {
        this.borrowed = borrowed;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // 生成 getter/setter：在类里右键 → 生成 → 访问器（Getter 和 Setter）
    // IDEA 会一口气生成 getId/setId/getTitle/.../isBorrowed/setBorrowed
}
