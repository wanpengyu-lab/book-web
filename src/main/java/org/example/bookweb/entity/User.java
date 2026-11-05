package org.example.bookweb.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体，对应表 app_user(id, username, password_hash, role, created_at)。
 * passwordHash 为 BCrypt 哈希（PRD 2.1），明文密码不落库、不出现在日志与异常信息。
 */
@Data
public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String role;
    private LocalDateTime createdAt;
}
