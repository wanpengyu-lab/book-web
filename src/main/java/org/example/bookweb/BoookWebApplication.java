package org.example.bookweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * v0.6：@EnableScheduling 激活 OverdueScanJob 的 @Scheduled（PRD 2.1 锁定 @Scheduled 方案）。
 */
@SpringBootApplication
@EnableScheduling
public class BoookWebApplication {

    public static void main(String[] args) {

        SpringApplication.run(BoookWebApplication.class, args);

    }

}
