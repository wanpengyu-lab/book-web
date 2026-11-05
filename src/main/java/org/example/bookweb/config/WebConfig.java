package org.example.bookweb.config;

import io.jsonwebtoken.security.Keys;
import org.example.bookweb.dto.BorrowProperties;
import org.example.bookweb.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Web 配置（v0.3）：
 * - HS256 密钥来自 JWT_SECRET 环境变量（PRD 6.1 必填；不足 32 字节时 jjwt 在启动期抛 WeakKeyException 快速失败）；
 * - BCryptPasswordEncoder（PRD 2.1 锁定，仅 crypto 模块）；
 * - JwtInterceptor 只拦 /api/**，旧无前缀端点保持现状。
 */
@Configuration
@EnableConfigurationProperties(BorrowProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final SecretKey jwtSecretKey;

    public WebConfig(@Value("${JWT_SECRET}") String jwtSecret) {
        this.jwtSecretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /** 供 AuthService 签发 token 与拦截器校验共用同一密钥 */
    @Bean
    public SecretKey jwtSecretKey() {
        return jwtSecretKey;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截器只序列化 Result{code,message,data}，独立 ObjectMapper 即可，不依赖容器 bean
        registry.addInterceptor(new JwtInterceptor(jwtSecretKey, new com.fasterxml.jackson.databind.ObjectMapper()))
                .addPathPatterns("/api/**");
    }
}
