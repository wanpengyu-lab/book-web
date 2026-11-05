package org.example.bookweb.config;

import io.jsonwebtoken.security.Keys;
import org.example.bookweb.dto.BorrowProperties;
import org.example.bookweb.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Web 配置（v0.3）：
 * - HS256 密钥来自 JWT_SECRET 环境变量（PRD 6.1 必填；不足 32 字节时 jjwt 在启动期抛 WeakKeyException 快速失败）；
 * - BCryptPasswordEncoder（PRD 2.1 锁定，仅 crypto 模块）；
 * - JwtInterceptor 只拦 /api/**（v0.10：旧无前缀端点已删除，静态资源本就不在拦截范围）；
 * - v0.10 SPA fallback：未匹配的非 /api GET 回 /index.html（新 SPA 入口），
 *   deep link（如 /books/1、/records）刷新可直达；favicon 等真实存在的静态资源正常返回。
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

    /**
     * v0.10 SPA fallback（PRD：未匹配的非 /api GET forward 到 /index.html）：
     * 静态资源命中原样返回；未命中时——/api/** 保持 404+40401（GlobalExceptionHandler），
     * 其余路径（SPA 前端路由）回 index.html。含扩展名的未知文件（如 /no-such.js）不兜底，
     * 维持 404，避免把资源缺失误报成页面。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        boolean api = resourcePath.equals("api") || resourcePath.startsWith("api/");
                        boolean hasExtension = resourcePath.lastIndexOf('.') > resourcePath.lastIndexOf('/');
                        if (!api && !hasExtension) {
                            Resource index = location.createRelative("index.html");
                            if (index.exists()) {
                                return index;
                            }
                        }
                        return null;
                    }
                });
    }
}
