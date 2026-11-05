package org.example.bookweb.service;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.example.bookweb.dto.LoginResponse;
import org.example.bookweb.entity.User;
import org.example.bookweb.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * 认证服务（v0.3 锁定方案）：
 * - 密码只存 BCryptPasswordEncoder 哈希（spring-security-crypto，非完整 spring-security）；
 * - token：jjwt 0.12.6 / HS256，payload {sub:userId, role, exp}，有效期 24h（PRD 2.1，禁止自动续期）。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long TTL_MS = 24L * 60 * 60 * 1000;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecretKey jwtSecretKey;

    /** 用户名已存在（注册冲突）→ 40901，由 GlobalExceptionHandler 映射 */
    public static class UsernameExistsException extends RuntimeException {
        public UsernameExistsException(String message) { super(message); }
    }

    /** 用户名或密码错误（登录失败）→ 40101，由 GlobalExceptionHandler 映射 */
    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) { super(message); }
    }

    /** 注册即 reader（PRD v0.3）；密码只在 encode 内部短暂存在，不落日志 */
    public boolean register(String username, String password) {
        if (userMapper.countByUsername(username) > 0) {
            throw new UsernameExistsException("用户名已存在");
        }
        userMapper.insert(username, passwordEncoder.encode(password), "reader");
        return true;
    }

    public LoginResponse login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException("用户名或密码错误");
        }
        LoginResponse resp = new LoginResponse();
        resp.setToken(createToken(user.getId(), user.getRole()));
        LoginResponse.UserView view = new LoginResponse.UserView();
        view.setId(user.getId());
        view.setUsername(user.getUsername());
        view.setRole(user.getRole());
        resp.setUser(view);
        return resp;
    }

    /** payload {sub:userId, role, exp}，HS256 签名，24h 过期 */
    private String createToken(int userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + TTL_MS))
                // PRD 2.1 锁定 HS256：显式指定算法，避免 jjwt 按密钥长度自动升级为 HS384/512
                .signWith(jwtSecretKey, Jwts.SIG.HS256)
                .compact();
    }
}
