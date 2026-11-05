package org.example.bookweb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 上下文启动冒烟测试。密码已外置为 ${SPRING_DATASOURCE_PASSWORD}（无默认值），
 * 测试上下文以 properties 提供占位值，不依赖真实数据库连接。
 */
@SpringBootTest(properties = "spring.datasource.password=v0-1-test-placeholder")
class BoookWebApplicationTests {

    @Test
    void contextLoads() {
    }

}
