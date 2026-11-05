package org.example.bookweb;

import org.example.bookweb.config.AdminInitializer;
import org.example.bookweb.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * v0.3 AdminInitializer 单测：无 admin 且未配置 ADMIN_INIT_PASSWORD → 启动失败；
 * 配置了 → 创建 admin 且只存哈希；已有 admin → 不动作。
 */
class AdminInitializerTest {

    private UserMapper userMapper;
    private AdminInitializer initializer;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        initializer = new AdminInitializer(userMapper, new BCryptPasswordEncoder());
    }

    @Test
    void failsWithClearMessageWhenNoAdminAndPasswordNotSet() {
        when(userMapper.countAdmin()).thenReturn(0);
        ReflectionTestUtils.setField(initializer, "adminInitPassword", "");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> initializer.run());
        assertTrue(ex.getMessage().contains("缺少 ADMIN_INIT_PASSWORD"));
        verify(userMapper, never()).insert(anyString(), anyString(), anyString());
    }

    @Test
    void createsAdminWithBcryptHashWhenPasswordSet() {
        when(userMapper.countAdmin()).thenReturn(0);
        ReflectionTestUtils.setField(initializer, "adminInitPassword", "init-pw-123");

        initializer.run();

        ArgumentCaptor<String> hashCap = ArgumentCaptor.forClass(String.class);
        verify(userMapper).insert(org.mockito.ArgumentMatchers.eq("admin"), hashCap.capture(),
                org.mockito.ArgumentMatchers.eq("admin"));
        String hash = hashCap.getValue();
        assertNotEquals("init-pw-123", hash, "明文密码不得入库");
        assertTrue(hash.startsWith("$2"), "必须是 BCrypt 哈希");
    }

    @Test
    void doesNothingWhenAdminAlreadyExists() {
        when(userMapper.countAdmin()).thenReturn(1);
        ReflectionTestUtils.setField(initializer, "adminInitPassword", "");

        initializer.run();
        verify(userMapper, never()).insert(anyString(), anyString(), anyString());
    }
}
