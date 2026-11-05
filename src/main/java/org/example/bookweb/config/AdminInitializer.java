package org.example.bookweb.config;

import org.example.bookweb.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * admin 初始化（v0.3 锁定方案，CommandLineRunner）：
 * - 库中无 admin 且配置了 ADMIN_INIT_PASSWORD → 创建 admin 账号（哈希入库，明文不入日志）；
 * - 库中无 admin 且未配置 → 启动失败并提示「缺少 ADMIN_INIT_PASSWORD」（PRD 2.1，禁止默认密码兜底）；
 * - 已有 admin → 不做任何事。
 */
@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_INIT_PASSWORD:}")
    private String adminInitPassword;

    public AdminInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userMapper.countAdmin() > 0) {
            return;
        }
        if (adminInitPassword == null || adminInitPassword.isBlank()) {
            throw new IllegalStateException("缺少 ADMIN_INIT_PASSWORD：库中无 admin 账号且未配置初始密码，拒绝启动");
        }
        userMapper.insert("admin", passwordEncoder.encode(adminInitPassword), "admin");
        log.info("admin 账号已初始化（角色 admin）");
    }
}
