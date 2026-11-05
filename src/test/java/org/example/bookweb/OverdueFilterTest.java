package org.example.bookweb;

import org.example.bookweb.dto.RecordPageResponse;
import org.example.bookweb.entity.BorrowRecord;
import org.example.bookweb.mapper.BorrowRecordMapper;
import org.example.bookweb.service.BorrowService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v0.4 逾期派生态测试（PRD v0.4）：构造 due_at 已过期且 status='active' 的记录，
 * 断言 status=overdue 能筛出、active 也能筛出、returned 筛不出——逾期不落库、只靠派生口径。
 * 清理只改状态（markReturned），不物理删除。
 * 注意：本测试需要真实数据库，./mvnw test 运行时必须提供 SPRING_DATASOURCE_PASSWORD。
 */
@SpringBootTest(properties = {
        "JWT_SECRET=test-jwt-secret-0123456789abcdef0123456789abcdef",
        "ADMIN_INIT_PASSWORD=test-init-pw"
})
class OverdueFilterTest {

    private static final int BOOK_ID = 1;
    /** 测试夹具专用 user_id（库中无对应账号；borrow_record 无外键，仅作数据标记） */
    private static final int FIXTURE_USER_ID = 99999;

    @Autowired
    private BorrowService borrowService;
    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @AfterEach
    void tearDown() {
        // 夹具记录改为 returned：不再出现在 overdue/active 视图，记录本身保留（禁止物理删除）
        borrowRecordMapper.markReturned(BOOK_ID, FIXTURE_USER_ID);
    }

    @Test
    void overdueActiveRecordIsFilteredByDerivedStatus() {
        // 构造过期 active 记录：due_at = 昨天，status 走默认 'active'
        borrowRecordMapper.insert(BOOK_ID, FIXTURE_USER_ID, LocalDateTime.now().minusDays(1));

        RecordPageResponse overdue = borrowService.listRecords(FIXTURE_USER_ID, "overdue", 1, 10);
        assertTrue(overdue.getList().stream().anyMatch(r -> r.getBookId() == BOOK_ID),
                "过期的 active 记录必须能被 overdue 筛出");

        BorrowRecord fixture = borrowService.listRecords(FIXTURE_USER_ID, "active", 1, 10).getList().stream()
                .filter(r -> r.getBookId() == BOOK_ID)
                .findFirst()
                .orElseThrow(() -> new AssertionError("同一条记录对 active 也应可见"));
        assertEquals("active", fixture.getStatus());

        // 按记录 id 精确断言：本条未归还记录不出现在 returned 视图（夹具用户历史 returned 记录不算）
        RecordPageResponse returned = borrowService.listRecords(FIXTURE_USER_ID, "returned", 1, 10);
        assertTrue(returned.getList().stream().noneMatch(r -> r.getId() == fixture.getId()),
                "未归还记录对 returned 不可见");

        assertThrows(BorrowService.InvalidStatusException.class,
                () -> borrowService.listRecords(FIXTURE_USER_ID, "bogus", 1, 5),
                "非法 status 必须 40001");
    }
}
