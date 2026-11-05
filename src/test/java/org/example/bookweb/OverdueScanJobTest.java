package org.example.bookweb;

import org.example.bookweb.mapper.BorrowRecordMapper;
import org.example.bookweb.mapper.NotificationMapper;
import org.example.bookweb.schedule.OverdueScanJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v0.6 OverdueScanJob 测试（PRD v0.6 验收「手工触发定时任务」）：连接真实 MySQL。
 * 手工触发方式 = 直接调用 scanDueSoon()（PRD 2.1 锁定 cron 0 0 9 * * * Asia/Shanghai，
 * 不得为验收私改线上 cron）。覆盖：due_at 不足 3 天的 active 记录生成提醒；
 * due_at 距今 3 天以外不提醒；同书同人当日去重（重复扫描不重复生成）。
 * 夹具 user_id 99998/99999（库中无对应账号），清理只改状态，不物理删除。
 * 注意：本测试需要真实数据库，./mvnw test 运行时必须提供 SPRING_DATASOURCE_PASSWORD。
 */
@SpringBootTest(properties = {
        "JWT_SECRET=test-jwt-secret-0123456789abcdef0123456789abcdef",
        "ADMIN_INIT_PASSWORD=test-init-pw"
})
class OverdueScanJobTest {

    private static final int BOOK_ID = 2;
    private static final int U_DUE_SOON = 99999;
    private static final int U_DUE_LATER = 99998;

    @Autowired
    private OverdueScanJob overdueScanJob;
    @Autowired
    private BorrowRecordMapper borrowRecordMapper;
    @Autowired
    private NotificationMapper notificationMapper;

    private String dueSoonContent;

    @BeforeEach
    void setUp() {
        // 夹具：99999 的记录 2 天后到期（窗口内）；99998 的记录 10 天后到期（窗口外）
        borrowRecordMapper.insert(BOOK_ID, U_DUE_SOON, LocalDateTime.now().plusDays(2));
        borrowRecordMapper.insert(BOOK_ID, U_DUE_LATER, LocalDateTime.now().plusDays(10));
        dueSoonContent = "您借阅的《活着》将在 3 天内到期，请及时归还";
    }

    @AfterEach
    void tearDown() {
        // 清理只改状态（markReturned），不物理删除
        borrowRecordMapper.markReturned(BOOK_ID, U_DUE_SOON);
        borrowRecordMapper.markReturned(BOOK_ID, U_DUE_LATER);
    }

    @Test
    void dueSoonRecordGetsReminderWithSameDayDedupe() {
        int before = notificationMapper.countTodayByUserAndContent(U_DUE_SOON, dueSoonContent);

        // 手工触发第一次：窗口内记录生成提醒
        int created = overdueScanJob.scanDueSoon();
        if (before == 0) {
            assertEquals(1, created, "首次扫描应为窗口内夹具记录生成 1 条提醒");
        }
        assertTrue(notificationMapper.countTodayByUserAndContent(U_DUE_SOON, dueSoonContent) > 0,
                "due_at 不足 3 天的 active 记录必须生成到期提醒");

        // 手工触发第二次：同书同人当日去重，不得重复生成
        overdueScanJob.scanDueSoon();
        assertEquals(1, notificationMapper.countTodayByUserAndContent(U_DUE_SOON, dueSoonContent),
                "同书同人当日只允许 1 条提醒");
    }

    @Test
    void recordDueBeyondWindowGetsNoReminder() {
        overdueScanJob.scanDueSoon();

        assertEquals(0, notificationMapper.countTodayByUserAndContent(
                        U_DUE_LATER, dueSoonContent),
                "due_at 距今超过 3 天的记录不得生成提醒");
    }
}
