package org.example.bookweb.dto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 借阅配置（v0.4 锁定方案）：借期 BORROW_DAYS 环境变量，默认 30 天；
 * due_at 由服务端计算（borrow.days），禁止前端传入。
 */
@Data
@ConfigurationProperties(prefix = "borrow")
public class BorrowProperties {

    /** 借期天数，默认 30 */
    private int days = 30;
}
