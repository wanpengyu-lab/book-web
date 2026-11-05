package org.example.bookweb.dto;

import lombok.Data;

/** 登录响应 DTO（v0.3）：data: {token, user:{id, username, role}} */
@Data
public class LoginResponse {

    private String token;

    private UserView user;

    @Data
    public static class UserView {
        private int id;
        private String username;
        private String role;
    }
}
