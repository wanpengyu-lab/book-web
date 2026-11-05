package org.example.bookweb;

import org.example.bookweb.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 上下文启动冒烟测试。密码已外置为 ${SPRING_DATASOURCE_PASSWORD}（无默认值），
 * 测试上下文以 properties 提供占位值，不依赖真实数据库连接。
 * v0.3 起：全量上下文会执行 AdminInitializer（CommandLineRunner），
 * 因此 mock UserMapper 并提供 ADMIN_INIT_PASSWORD，避免测试期触发真实 DB 调用。
 */
@SpringBootTest(properties = {
        "spring.datasource.password=v0-1-test-placeholder",
        "JWT_SECRET=test-jwt-secret-0123456789abcdef0123456789abcdef",
        "ADMIN_INIT_PASSWORD=test-init-pw"
})
class BoookWebApplicationTests {

    @MockitoBean
    private UserMapper userMapper;

    @Test
    void contextLoads() {
    }

}
