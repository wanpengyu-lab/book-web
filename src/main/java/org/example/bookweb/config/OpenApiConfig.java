package org.example.bookweb.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc-openapi 文档元信息（v0.1 锁定方案）。
 * Swagger UI：/swagger-ui.html；OpenAPI JSON：/v3/api-docs。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI boookWebOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("boook-web API")
                .description("图书管理系统 API。自 v0.1 起所有接口统一返回 Result{code, message, data}。")
                .version("v0.1"));
    }
}
